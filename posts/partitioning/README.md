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
  
## 소개

파티셔닝은 마스터 단계 팜이 처리를 위해 여러 작업자 단계로 작업하는 개념입니다.  
파티션 된 단계에서 큰 데이터 세트 (예 : 백만 개의 행이있는 데이터베이스 테이블)는 더 작은 파티션으로 나뉩니다.  
각 파티션은 작업자가 병렬로 처리합니다.  
각 작업자는 자체 읽기, 처리, 쓰기 등을 담당하는 완전한 Spring Batch 단계입니다.  
이 모델에는 큰 장점이 있습니다.  
예를 들어, 이 모델에서는 다시 시작 가능성과 같은 모든 기능을 즉시 사용할 수 있습니다.  
작업자의 구현은 또 다른 단계이기 때문에 자연스럽게 느껴집니다.

Remote Chunking 과의 차이는?

Remote Chunking과 달리 파티셔닝은 메세지 유실에 대해 개발자가 직접 고려해야할 필요가 없습니다.  
파티셔닝을 통해 Spring Batch는 자체 단계 실행에서 각 파티션을 처리합니다. 실패 후 다시 시작하면 Spring Batch는 파티션을 다시 생성하고 다시 처리합니다.  
Spring Batch는 데이터를 처리되지 않은 상태로 두지 않습니다.

## 설계

### Partitioner

Partitioner 인터페이스는 partition (int gridSize) 라는 단일 메서드로 구성됩니다 . Map <String, ExecutionContext>를 반환합니다 . gridSize는 더에 대한 힌트보다 전체 클러스터에 대한 효율적인 방법으로 데이터를 분할 할 수있을 것으로 얼마나 많은 노동자에 관해서 아무것도 아니다. 즉, 해당 값을 동적으로 결정하는 Spring Batch에는 아무것도 없습니다. 계산하거나 설정하는 것은 귀하에게 달려 있습니다. 메서드가 반환 하는 Map 은 키가 파티션의 이름이고 고유해야하는 키 값 쌍으로 구성되어야합니다. 의 ExecutionContext는 , 전술 한 바와 같이, 처리하는 어떤 식별 파티션 메타 데이터의 표현이다.

### PartitionHandler

이 인터페이스는 작업자와 통신하는 방법을 이해하는 인터페이스입니다. 각 작업자에게 작업 할 작업을 알리는 방법과 모든 작업이 완료되는시기를 식별하는 방법. Spring Batch를 사용할 때 자신 만의 Partitioner 구현을 작성할 수 있지만 , 자신 만의 PartitionHandler를 작성하지는 않을 것입니다 .

* TaskExecutorPartitionHandler
  * 단일 JVM 내에서 분할 개념을 사용할 수 있도록 같은 JVM 내에서 스레드로 분할 실행
* MessageChannelPartitionHandler
  * 원격의 JVM에 메타 데이터를 전송
  
## 예제

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