# Spring Batch Partitioning 활용하기

지난 시간에 소개 드린 [Multithread Step](https://jojoldu.tistory.com/493)과 더불어 Partitioning은 Spring Batch의 대표적인 Scalling 기능입니다.  
  
서비스에 적재된 데이터가 적을 경우에는 Spring Batch의 기본 기능들만 사용해도 큰 문제가 없으나, 일정 규모 이상이 되면 (ex: 매일 수백만 row가 추가되는 상황에서의 일일 집계) 서버를 Scalling (Up or Out) 하듯이 배치 애플리케이션 역시 확장이 필요합니다.  
  
이런 문제를 고려해서 Spring Batch 에서는 여러 Scalling 기능들을 지원하는데요.  
대표적으로 다음과 같습니다.

* [Multi-threaded Step](https://jojoldu.tistory.com/493) (Single process / Local)
  * **단일 Step**을 수행할 때, 해당 Step 내의 **각 Chunk를 별도의 여러 쓰레드에서 실행** 하는 방법
* Parallel Steps (Single Process / Local)
  * **여러개의 Step**을 **병렬**로 실행하는 방법
  * **단일 Step의 성능 향상은 없음**
* Remote Chunking (Multi process / Remote)
  * 일종의 분산환경처럼 Step 처리가 여러 프로세스로 분할되어 외부의 다른 서버로 전송되어 처리하는 방식
    * ex) A서버에서 ItemReader 구현체를 사용하여 데이터를 읽고, B 서버에서 ItemWriter 구현체를 갖고 있어 A 서버에서 보낸 데이터를 저장하는 등
  * 다만, **어느 서버에서 어느 데이터를 처리하고 있는지 메타 데이터 관리를 하지 않기 때문에** 메세지 유실이 안되는 것이 100% 보장되어야 한다 (ex: AWS SQS, 카프카 등의 메세지큐 사용을 권장)
* Partitioning (Single or Multi process / Local or Remote)
  * 마스터를 이용해 데이터를 더 작은 Chunk (파티션이라고 함)로 나눈 다음 파티션에서 슬레이브가 독립적으로 작동하는 방식 (이번 시간에 해볼 것)
  * 슬레이브가 로컬일 필요가 없어 확장된 JVM 환경에서의 실행을 해볼 수 있음. 
    * 원격 슬레이브와 통신하기 위해 다양한 통신 메커니즘을 지원
* ```AsyncItemProcessor```/```AsyncItemWriter```
  * 별개의 쓰레드를 통해 ItemProcessor와 ItemWriter를 처리하는 방식
  * ```spring-batch-integration``` 의존성에서 지원
  * 주의) AsyncItemProcessor 및 AsyncItemWriter 는 함께 사용해야 함 
    * 그렇지 않으면 AsyncItemProcessor에서 전달한 ```Future``` 객체를 본인이 직접 다뤄야 함

이번 시간에는 이 중 하나인 Partitioning에 대해서 다뤄볼 예정입니다.  
  
아마 여기까지 이 글을 보신 분들은 다음과 같은 생각을 할 수도 있습니다.  

"```completablefuture``` 나 ```@Async```를 이용하여 비동기로 동시에 쓰레드를 사용하면 되지 않냐"  
  
우리가 [Multithread Step](https://jojoldu.tistory.com/493) 나 Partitioning와 같은 Spring Batch의 Scalling 기능을 사용하는 이유는, **기존의 코드 변경 없이** 성능을 향상 시키기 위함입니다.  
  
위에서 언급한대로 ```completablefuture``` 나 ```@Async``` 를 기존 Spring Batch에 사용하기 위해서는 일정 부분 혹은 아주 많은 부분의 코드 변경이 필수인 반면, Spring Batch의 Scalling 기능들은 기존 코드 변경이 거의 없습니다.  
  
다양한 Scalling 기능을 **기존의 스프링 배치 코드 변경 없이**, 그리고 많은 레퍼런스로 인해 안정적으로 구현이 가능한 기능들이기 때문에 대량의 데이터 처리가 필요한 상황에서는 꼭 사용해봐야한다고 봅니다.  
  
## 1. 소개

파티셔닝은 마스터 (혹은 매니저) Step이 대량의 데이터 처리를 위해 지정된 수의 작업자 (Worker) Step으로 일감을 분할처리하는 방식.

![intro1](./images/intro1.png)

(이미지출처: [Spring Batch 공식문서](https://docs.spring.io/spring-batch/docs/current/reference/html/scalability.html#partitioning))

컨셉만 들었을 때는 이게 Multithread Step과 무엇이 다른건지 궁금하실텐데요.  
  
* Multithread Step은 **단일 Step을 Chunk 단위로 쓰레드를 생성해 분할 처리** 하게 됩니다.
  * 어떤 쓰레드에서 어떤 데이터들을 처리하게 할지 세밀한 조정이 불가능합니다.
  * 또한, 해당 Step의 ItemReader/ItemWriter 등이 **Multithread 환경을 지원하는지** 유무가 굉장히 중요합니다.
* 반면 Partitioning의 경우는 각각 별도의 StepExecution 파라미터를 가진 여러개의 Worker Step으로 실행합니다. 
  * (Local로 실행할 경우) Multithread으로 작동하나, Multithread Step과는 별개로 ItemReader/ItemWriter의 **Multithread 환경 지원 여부가 중요하지 않습니다**
 
예를 들어 Partitioning Step에서 백만 개의 데이터를 더 작은 파티션으로 나누어 각 파티션을 Worker Step들이 병렬로 처리합니다.  
  
각각의 Worker는 ItemReader / ItemProcessor / ItemWriter 등을 담당하는 완전한 Spring Batch Step이기 때문에 기존의 Spring Batch 코드 변경이 거의 없는채로 병렬 실행 환경을 구성할 수 있습니다.  
  
## 2. 주요 인터페이스 소개

### Partitioner

Partitioner 인터페이스는 파티셔닝된 Step (Worker Step)을 위한 Step Executions을 생성하는 인터페이스 입니다.  
  
기본 구현은 SimplePartitioner로, 빈 Step Executions를 생성합니다.  
  
인터페이스가 갖고 있는 메소드는 1개로 ```partition (int gridSize)``` 입니다.  
  
해당 파라미터로 넘기는 ```gridSize```는 Spring Batch에서 기본적으로 **1**로 두며, 이를 변경하기 위해서는 PartitionHandler 등을 통해서 변경 가능합니다.  
  
다만, 이렇게 ```gridSize```만 지정했다고 하여, Worker Step이 자동으로 구성되진 않습니다.  
  
해당 ```gridSize```를 이용하여 각 Worker Step마다 어떤 Step Executions 환경을 갖게 할지는 오로지 개발자들의 몫 입니다.  


### PartitionHandler

PartitionHandler 인터페이스는 매니저 Step이 어떻게 Worker Step를 다룰지를 정의하는 인터페이스입니다.  
이를테면, 어느 Step을 Worker step의 코드로 두고 병렬로 실행하게할지, 병렬로 실행한다면 쓰레드풀 관리는 어떻게 할지, ```gridSize```는 몇으로 둘지 등등을 비롯하여 모든 작업이 완료되었는지를 식별하는지를 다룹니다.  
  
일반적으로는 Partitioner의 구현체는 개발자가 요구사항에 따라 별도 생성해서 사용하곤 하지만, 자신만의 PartitionHandler를 작성하지는 않을 것입니다.  
  
구현체로는 크게 2가지가 있습니다.

* TaskExecutorPartitionHandler
  * 단일 JVM 내에서 분할 개념을 사용할 수 있도록 같은 JVM 내에서 스레드로 분할 실행
* MessageChannelPartitionHandler
  * 원격의 JVM에 메타 데이터를 전송
  
## 3. 예제

보통의 예제는 **여러 파일을 파티션 단위로 나눠서 읽어서 처리하는 방식**를 소개하는데요.  

* [baeldung - Spring Batch using Partitioner](https://www.baeldung.com/spring-batch-partitioner)

이미 기존에 많이 나온 예제라서 이번 시간에는 **특정 기간의 DB 데이터를 집계**하는 파티셔닝 배치를 진행해보겠습니다.  

 
 파티 셔 너는 테이블을 파티셔닝하지 않으며 번호가 매겨진 파티션 이름을 키로 사용하는 "빈"실행 컨텍스트 만 생성합니다. 따라서 작업자는 실제로보고있는 행동을 설명하는 동일한 데이터를 읽습니다.

### 로컬

```java
import com.jojoldu.batch.entity.product.Product;
import com.jojoldu.batch.entity.product.ProductRepository;
import com.jojoldu.batch.entity.product.backup.ProductBackup;
import com.jojoldu.batch.entity.product.backup.ProductBackupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class PartitionLocalConfiguration {
    public static final String JOB_NAME = "partitionLocalBatch";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;
    private final ProductRepository productRepository;
    private final ProductBackupRepository productBackupRepository;

    private int chunkSize;

    @Value("${chunkSize:1000}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    private int poolSize;

    @Value("${poolSize:5}")
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    @Bean(name = JOB_NAME+"taskPool")
    public TaskExecutor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setThreadNamePrefix("partition-thread");
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
        executor.initialize();
        return executor;
    }

    @Bean
    public TaskExecutorPartitionHandler partitionHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(step1());
        partitionHandler.setTaskExecutor(executor());
        partitionHandler.setGridSize(poolSize);
        return partitionHandler;
    }

    @Bean(name = JOB_NAME)
    public Job job() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(step1Manager())
                .preventRestart()
                .build();
    }

    @Bean(name = JOB_NAME +"_step1Manager")
    public Step step1Manager() {
        return stepBuilderFactory.get("step1.manager")
                .partitioner("step1", partitioner(null, null))
                .step(step1())
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean(name = JOB_NAME +"_partitioner")
    @StepScope
    public ProductIdRangePartitioner partitioner(
            @Value("#{jobParameters['startDate']}") String startDate,
            @Value("#{jobParameters['endDate']}") String endDate) {
        
        LocalDate startLocalDate = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate endLocalDate = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return new ProductIdRangePartitioner(productRepository, startLocalDate, endLocalDate);
    }

    @Bean(name = JOB_NAME +"_step")
    public Step step1() {
        return stepBuilderFactory.get(JOB_NAME +"_step")
                .<Product, ProductBackup>chunk(chunkSize)
                .reader(reader(null, null))
                .processor(processor())
                .writer(writer(null, null))
                .build();
    }

    @Bean(name = JOB_NAME +"_reader")
    @StepScope
    public JpaPagingItemReader<Product> reader(
            @Value("#{stepExecutionContext[minId]}") Long minId,
            @Value("#{stepExecutionContext[maxId]}") Long maxId) {

        Map<String, Object> params = new HashMap<>();
        params.put("minId", minId);
        params.put("maxId", maxId);

        log.info("reader minId={}, maxId={}", minId, maxId);

        return new JpaPagingItemReaderBuilder<Product>()
                .name(JOB_NAME +"_reader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(chunkSize)
                .queryString(
                        "SELECT p " +
                        "FROM Product p " +
                        "WHERE p.id BETWEEN :minId AND :maxId")
                .parameterValues(params)
                .build();
    }

    private ItemProcessor<Product, ProductBackup> processor() {
        return ProductBackup::new;
    }

    @Bean(name = JOB_NAME +"_writer")
    @StepScope
    public ItemWriter<ProductBackup> writer(
            @Value("#{stepExecutionContext[minId]}") Long minId,
            @Value("#{stepExecutionContext[maxId]}") Long maxId) {

        return items -> {
            log.info("expectedMinId={}, real minId={}", minId, items.get(0).getOriginId());
            log.info("expectedMaxId={}, real maxId={}", maxId, items.get(items.size()-1).getOriginId());
            productBackupRepository.saveAll(items);
        };
    }
}
```

* [ColumnRangePartitioner.java](https://github.com/spring-projects/spring-batch/blob/d8fc58338d3b059b67b5f777adc132d2564d7402/spring-batch-samples/src/main/java/org/springframework/batch/sample/common/ColumnRangePartitioner.java)
 
### 원격

A: Spring Batch 파티셔닝
B: Post API

시나리오: A에서 파티셔닝으로 B 