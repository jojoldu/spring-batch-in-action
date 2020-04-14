# Spring Batch에서 MultiThread로 Step 실행하기

일반적으로 Spring Batch는 단일 쓰레드에서 실행됩니다.  
즉, 모든 것이 순차적으로 실행되는 것을 의미하는데요.  
Spring Batch에서는 이를 병렬로 실행할 수 있는 방법을 여러가지 지원합니다.  
이번 시간에는 그 중 하나인 멀티스레드로 Step을 실행하는 방법에 대해서 알아보겠습니다.  

## 1. 소개

Spring Batch의 멀티쓰레드 Step은 Spring의 ```TaskExecutor```를 이용하여 **각 쓰레드가 Chunk 단위로 실행되게** 하는 방식입니다.  
  
> Spring Batch Chunk에 대한 내용은 [이전 포스팅](https://jojoldu.tistory.com/331)에 소개되어있습니다.

![intro](./images/intro.png)

여기서 어떤 ```TaskExecutor``` 를 선택하냐에 따라 모든 Chunk 단위별로 쓰레드가 계속 새로 생성될 수도 있으며 (```SimpleAsyncTaskExecutor```) 혹은 쓰레드풀 내에서 지정된 갯수의 쓰레드만을 재사용하면서 실행 될 수도 있습니다. (```ThreadPoolTaskExecutor```)  
  
Spring Batch에서 멀티쓰레드 환경을 구성하기 위해서 가장 먼저 해야할 일은 사용하고자 하는 **Reader와 Writer가 멀티쓰레드를 지원하는지** 확인하는 것 입니다.  

![javadoc](./images/javadoc.png)

(```JpaPagingItemReader```의 Javadoc)  
  
각 Reader와 Writer의 Javadoc에 항상 저 **thread-safe** 문구가 있는지 확인해보셔야 합니다.  
만약 없는 경우엔 thread-safe가 지원되는 Reader 와 Writer를 선택해주셔야하며, 꼭 그 Reader를 써야한다면 [SynchronizedItemStreamReader](https://docs.spring.io/spring-batch/docs/current/api/org/springframework/batch/item/support/SynchronizedItemStreamReader.html) 등을 이용해 **thread-safe**로 변환해서 사용해볼 수 있습니다.  
  
그리고 또 하나 주의할 것은 멀티 쓰레드로 각 Chunk들이 개별로 진행되다보니 Spring Batch의 큰 장점중 하나인 **실패 지점에서 재시작하는 것은 불가능** 합니다.  
  
이유는 간단합니다.  
단일 쓰레드로 순차적으로 실행할때는 10번째 Chunk가 실패한다면 **9번째까지의 Chunk가 성공했음이 보장**되지만, 멀티쓰레드의 경우 1~10개의 Chunk가 동시에 실행되다보니 10번째 Chunk가 실패했다고 해서 **1~9개까지의 Chunk가 다 성공된 상태임이 보장되지 않습니다**.  
  
그래서 일반적으로는 ItemReader의 ```saveState``` 옵션을 ```false``` 로 설정하고 사용합니다.  

> 이건 예제 코드에서 설정을 보여드리겠습니다.


자 그럼 실제로 하나씩 코드를 작성하면서 실습해보겠습니다.

## 2. PagingItemReader 예제

가장 먼저 알아볼 것은 PagingItemReader를 사용할때 입니다.  
이때는 걱정할 게 없습니다.  
PagingItemReader는 **Thread Safe** 하기 때문입니다.  


> 멀티 쓰레드로 실행할 배치가 필요하시다면 웬만하면 PagingItemReader로 사용하길 추천드립니다.


예제 코드는 JpaPagingItemReader로 작성하였습니다.

```java

@Slf4j
@RequiredArgsConstructor
@Configuration
public class MultiThreadPagingConfiguration {
    public static final String JOB_NAME = "multiThreadPagingBatch";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private int chunkSize;

    @Value("${chunkSize:1000}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    private int poolSize;

    @Value("${poolSize:10}") // (1)
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    @Bean(name = JOB_NAME+"taskPool")
    public TaskExecutor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(); // (2)
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
                .taskExecutor(executor()) // (2)
                .throttleLimit(poolSize) // (3)
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
                .queryString("SELECT p FROM Product p WHERE p.createDate =:createDate")
                .parameterValues(params)
                .saveState(false) // (4)
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


(1) ```@Value("${poolSize:10}")```

* 생성할 쓰레드 풀의 쓰레드 수를 환경변수로 받아서 사용합니다.
* ```${poolSize:10}``` 에서 10은 앞에 선언된 변수 ```poolSize```가 없을 경우 10을 사용한다는 기본값으로 보시면 됩니다.

(2) ```ThreadPoolTaskExecutor```

* 쓰레드 풀을 이용한 쓰레드 관리 방식입니다.
* 옵션
  * ```corePoolSize```: Pool의 기본 사이즈
  * ```maxPoolSize```: Pool의 최대 사이즈
* 이외에도 ```SimpleAsyncTaskExecutor``` 가 있는데, 이를 사용할 경우 **매 요청시마다 쓰레드를 생성**하게 됩니다.
  * 이때 계속 생성하다가 concurrency limit 을 초과할 경우 이후 요청을 막게되는 현상까지 있어, 운영 환경에선 잘 사용하진 않습니다.
* 좀 더 자세한 설명은 [링크](https://github.com/HomoEfficio/dev-tips/blob/master/Java-Spring%20Thread%20Programming%20%EA%B0%84%EB%8B%A8%20%EC%A0%95%EB%A6%AC.md#threadpoolexecutor) 참고하시면 더 좋습니다
  
(3) ```throttleLimit(poolSize)```

* 기본값은 **4** 입니다.
* 생성된 쓰레드 중 몇개를 실제 작업에 사용할지를 결정합니다.
* 만약 10개의 쓰레드를 생성하고 ```throttleLimit```을 4로 두었다면, 10개 쓰레드 중 4개만 배치에서 사용하게 됨을 의미합니다.
* 일반적으로 ```corePoolSize```, ```maximumPoolSize```, ```throttleLimit``` 를 모두 같은 값으로 맞춥니다.
 
(4) ```.saveState(false)```

* 앞에서도 설명드린것처럼, 멀티쓰레드 환경에서 사용할 경우 필수적으로 사용해야할 옵션이 ```saveState = false``` 입니다.
* 해당 옵션을 끄게 되면 (```false```) Reader 가 실패한 지점을 저장하지 못하게해, 다음 실행시에도 무조건 처음부터 다시 읽도록 합니다.
* 이 옵션을 켜놓으면 오히려 더 큰 문제가 발생할 수 있습니다.
  * 8번째 Chunk 에서 실패했는데, 사실은 4번째 Chunk도 실패했다면 8번째가 기록되어 다음 재실행시 8번째부터 실행될수 있기 때문입니다.
  * 실패하면 무조건 처음부터 다시 실행될 수 있도록 해당 옵션은 ```false```로 두는 것을 추천합니다.
* 비슷한 기능으로 Job 옵션에 있는 ```.preventRestart()```가 있는데, 해당 옵션은 Job이 같은 파라미터로 재실행되는것을 금지합니다.
  * ```.saveState(false)```는 Reader가 실패난 지점을 기록하지 못하게 하는 옵션이라 엄밀히 말하면 둘은 서로 다른 옵션이긴 합니다.
  * **Step 재실행을 막는다**정도로 봐주시면 됩니다.


자 그럼 이제 이 코드가 실제로 멀티쓰레드로 잘 작동하는지 테스트 코드로 검증해보겠습니다.

### 테스트 코드

> 모든 테스트 코드는 JUnit5를 사용합니다.
> Spring Batch에서 테스트 코드 작성이 처음이신분들은 [앞에 작성된 포스팅](https://jojoldu.tistory.com/455)을 먼저 참고해주세요. 

```java
@ExtendWith(SpringExtension.class)
@SpringBatchTest
@SpringBootTest(classes={MultiThreadPagingConfiguration.class, TestBatchConfig.class})
@TestPropertySource(properties = {"chunkSize=1", "poolSize=2"}) // (1)
public class MultiThreadPagingConfigurationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductBackupRepository productBackupRepository;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @AfterEach
    void after() {
        productRepository.deleteAll();
        productBackupRepository.deleteAll();
    }

    @Test
    public void 페이징_분산처리_된다() throws Exception {
        //given
        LocalDate createDate = LocalDate.of(2020,4,13);
        ProductStatus status = ProductStatus.APPROVE;
        long price = 1000L;
        for (int i = 0; i < 10; i++) {
            productRepository.save(Product.builder()
                    .price(i * price)
                    .createDate(createDate)
                    .status(status)
                    .build());
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("createDate", createDate.toString())
                .addString("status", status.name())
                .toJobParameters();
        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        List<ProductBackup> backups = productBackupRepository.findAll();
        backups.sort(Comparator.comparingLong(ProductBackup::getPrice));

        assertThat(backups).hasSize(10);
        assertThat(backups.get(0).getPrice()).isEqualTo(0L);
        assertThat(backups.get(9).getPrice()).isEqualTo(9000L);
    }

}
```

(1) ```properties = {"chunkSize=1", "poolSize=2"}```

* 각 옵션은 다음과 같은 의미를 가집니다.
  * ```chunkSize=1```: 하나의 Chunk가 처리할 데이터가 1건을 의미합니다.
  * ```poolSize=2```: 생성될 쓰레드 풀의 쓰레드 개수를 2개로 합니다.
* 이렇게 할 경우 10개의 데이터를 처리할때 **2개의 쓰레드가 각 5회씩** 처리됩니다.
  * 물론 1개의 쓰레드에서 오랜 시간 동안 처리하게 된다면 다른 1개가 더 많은 건수를 처리할 수도 있습니다.  

위 테스트 코드를 한번 실행해보면?  
아래 그림처럼 **2개의 쓰레드가 각자 페이지를 Read하고 Write** 하는것을 확인할 수 있습니다.

![paging-test-1](./images/paging-test-1.png)

이전과 같이 단일 쓰레드 모델이였다면 어떻게 될까요?  
그럼 아래와 같이 1개페이지에 대해 처리가 끝난 후에야 다음 페이지를 읽게 됩니다.
(계속 앞에 페이지를 읽는 것을 기다려야 하는 것이죠)

![paging-test-2](./images/paging-test-2.png)

JpaPagingItemReader를 예시로 보여드렸지만, 그외 나머지 PagingItemReader들 역시 동일하게 사용하시면 됩니다

![jdbcpaging](./images/jdbcpaging.png)

(JdbcPagingItemReader)
## 3. CursorItemReader

SynchronizedItemStreamReader 로 Wrapping 하여 처리한다.

> JpaCursorItemReader는 [Spring Batch 4.3](https://github.com/spring-projects/spring-batch/issues/901)에 추가될 예정입니다.
> 
## 4. Tip

### PoolSize는 Environment Variable (환경변수)로 받기

개발 환경에서는 1개의 쓰레드로, 운영에선 10개의 쓰레드로 실행해야 될 수 있기 때문입니다.  
몇개의 쓰레드풀을 쓸지를 요청자가 결정할 수 있도록 chunkSize와 마찬가지로 환경변수로 받아서 사용합니다.  
  
이렇게 하지 않으면, 로컬/개발/테스트 등에선 1개의 쓰레드로 **순차적으로 로그를 확인하고 싶을때도 동적으로 변경이 어렵습니다**.  

## 마무리

이제 느린 Batch 작업들은 멀티쓰레드로 해결하면 되는 것일까요!?  
그렇지는 않습니다.  
이미 네트워크/DISK IO/CPU/Memory 등 서버 자원이 이미 **단일 쓰레드에서도 한계치에 달했다면** 멀티쓰레드로 진행한다고 해서 성능 향상을 기대할 순 없습니다.  


* [Spring 공식문서](https://docs.spring.io/spring-batch/docs/current/reference/html/scalability.html#multithreadedStep)