# Spring Batch에서 MultiThread로 Step 실행하기

일반적으로 Spring Batch는 단일 쓰레드에서 실행됩니다.  
즉, 모든 것이 순차적으로 실행되는 것을 의미하는데요.  
Spring Batch에서는 이를 병렬로 실행할 수 있는 방법을 여러가지 지원합니다.  
이번 시간에는 그 중 하나인 멀티스레드로 Step을 실행하는 방법에 대해서 알아보겠습니다.  

## 1. 소개

Spring Batch의 멀티쓰레드 Step은 Spring의 ```TaskExecutor```를 이용하여 **Chunk 단위로 쓰레드를 생성하여 실행** 하는 방식입니다.  
  
> Spring Batch Chunk에 대한 내용은 [이전 포스팅](https://jojoldu.tistory.com/331)에 소개되어있습니다.

![intro](./images/intro.png)

여기서 어떤 ```TaskExecutor``` 를 선택하냐에 따라 모든 Chunk 단위별로 쓰레드가 새로 생성될 수도 있으며 (```SimpleAsyncTaskExecutor```) 혹은 쓰레드풀 내에서 지정된 갯수의 쓰레드만을 재사용하면서 실행 될 수도 있습니다. (```ThreadPoolTaskExecutor```)  
  
Spring Batch에서 멀티쓰레드 환경을 구성하기 위해서 가장 먼저 해야할 일은 사용하고자 하는 **Reader와 Writer가 멀티쓰레드를 지원하는지** 확인하는 것 입니다.  

![javadoc](./images/javadoc.png)

(```JpaPagingItemReader```의 Javadoc)  
  
각 Reader와 Writer의 Javadoc에 항상 저 **thread-safe** 문구가 있는지 확인해보셔야 합니다.  

그러나 다중 스레드 클라이언트에서 사용되는 경우 saveState = false를 사용해야합니다 (다시 시작할 수 없음)

```java

@Slf4j
@RequiredArgsConstructor
@Configuration
public class MultiThreadConfiguration {
    public static final String JOB_NAME = "multiThreadStepBatch";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private int chunkSize;

    @Value("${chunkSize:1000}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    private int poolSize;

    @Value("${poolSize:10}")
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    @Bean(name = JOB_NAME+"taskPool")
    public TaskExecutor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setThreadNamePrefix("multi-thread-");
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
        executor.initialize();
        return executor;
    }

    @Bean(name = JOB_NAME)
    public Job job() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(step())
                .preventRestart()
                .build();
    }

    @Bean(name = JOB_NAME +"_step")
    @JobScope
    public Step step() {
        return stepBuilderFactory.get(JOB_NAME +"_step")
                .<Product, ProductBackup>chunk(chunkSize)
                .reader(reader(null))
                .processor(processor())
                .writer(writer())
                .taskExecutor(executor())
                .throttleLimit(poolSize)
                .build();
    }


    @Bean(name = JOB_NAME +"_reader")
    @StepScope
    public JpaPagingItemReader<Product> reader(@Value("#{jobParameters[createDate]}") String createDate) {

        Map<String, Object> params = new HashMap<>();
        params.put("createDate", LocalDate.parse(createDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        return new JpaPagingItemReaderBuilder<Product>()
                .name(JOB_NAME +"_reader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(chunkSize)
                .queryString("SELECT p FROM Product p WHERE p.createDate =:createDate AND p.status =:status")
                .parameterValues(params)
                .saveState(false) // (1)
                .build();
    }

    private ItemProcessor<Product, ProductBackup> processor() {
        return ProductBackup::new;
    }

    @Bean(name = JOB_NAME +"_writer")
    @StepScope
    public JpaItemWriter<ProductBackup> writer() {
        return new JpaItemWriterBuilder<ProductBackup>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
```

```java
    @Bean(name = BATCH_NAME+"taskPool")
    public TaskExecutor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setThreadNamePrefix("collect-order-");
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
        executor.initialize();
        return executor;
    }
```

(1) ```.saveState(false)```

* Spring Batch에서 제공하는 대부분의 ItemReader 는 stateful 입니다. 
* Spring Batch는 Job을 다시 시작할 때 이 state를 사용하므로 처리가 중단 된 위치를 알 수 있습니다.  
* 그러나 멀티 스레드 환경에서 여러 스레드가 액세스 할 수 있는 방식 (동기화되지 않은 등)으로 상태를 유지하는 객체는 스레드의 상태를 서로 덮어 쓰는 문제가 발생할 수 있습니다. 
* 이 때문에 리더의 상태 저장 기능을 해제하여 이 작업을 다시 시작할 수 없게합니다.

(2) ```ThreadPoolTaskExecutor```

* 쓰레드 풀을 이용한 쓰레드 관리 방식입니다.
* 옵션
  * ```corePoolSize```: Pool의 기본 사이즈
  * ```maxPoolSize```: Pool의 최대 사이즈
* 이외에도 ```SimpleAsyncTaskExecutor``` 가 있는데, 이를 사용할 경우 **매 요청시마다 쓰레드를 생성**하게 됩니다.
  * 이때 계속 생성하다가 concurrency limit 을 초과할 경우 이후 요청을 막게되는 현상까지 있어, 일반적으로 잘 사용하진 않습니다.
* 좀 더 자세한 설명은 [링크](https://github.com/HomoEfficio/dev-tips/blob/master/Java-Spring%20Thread%20Programming%20%EA%B0%84%EB%8B%A8%20%EC%A0%95%EB%A6%AC.md#threadpoolexecutor) 참고

(3) ```throttleLimit(poolSize)```

* 기본값은 4 입니다.
* 생성된 쓰레드 중 몇개를 실제 작업에 사용할지를 결정합니다.
* 만약 10개의 쓰레드를 생성하고 ```throttleLimit```을 4로 두었다면, 10개 쓰레드 중 4개만 사용하게 됨을 의미합니다.
* 일반적으로 ```corePoolSize```, ```maximumPoolSize```, ```throttleLimit``` 를 모두 같은 값으로 맞춥니다.
  
## PagingItemReader

쓰레드에 안전합니다

* [JdbcPagingItemReader](https://docs.spring.io/spring-batch/docs/current/api/org/springframework/batch/item/database/JdbcPagingItemReader.html)

## CursorItemReader

SynchronizedItemStreamReader 로 Wrapping 하여 처리한다.

### PoolSize를 setter로 받는 이유

개발 환경에서는 1개의 쓰레드로, 운영에선 10개의 쓰레드로 실행해야 될 수 있기 때문입니다.  
몇개의 쓰레드풀을 쓸지를 요청자가 결정할 수 있도록 chunkSize와 마찬가지로 환경변수로 받아서 사용합니다.

## 마무리

이제 느린 Batch 작업들은 멀티쓰레드로 해결하면 되는 것일까요!?  
그렇지는 않습니다.  
이미 네트워크/DISK IO/CPU/Memory 등이 단일 쓰레드에서도 한계치에 달했다면 멀티쓰레드로 진행한다고 해서 성능 향상을 기대할 순 없습니다.  


* [Spring 공식문서](https://docs.spring.io/spring-batch/docs/current/reference/html/scalability.html#multithreadedStep)