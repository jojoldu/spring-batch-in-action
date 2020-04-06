# Spring Batch에서 MultiThread로 Step 실행하기


![intro](./images/intro.png)

Spring Batch에서 멀티쓰레드 환경을 구성하기 위해서 가장 먼저 해야할 일은 사용하고자 하는 **Reader와 Writer가 멀티쓰레드를 지원하는지** 확인하는 것 입니다.  

![javadoc](./images/javadoc.png)

각 Reader와 Writer의 Javadoc에 항상 저 **thread-safe** 문구가 있는지 확인해보셔야 합니다.  

그러나 다중 스레드 클라이언트에서 사용되는 경우 saveState = false를 사용해야합니다 (다시 시작할 수 없음)

```java

public class MultithreadedJobApplication {
        @Autowired
        private JobBuilderFactory jobBuilderFactory;
        @Autowired
        private StepBuilderFactory stepBuilderFactory;
        @Bean
        @StepScope
        public FlatFileItemReader<Transaction> fileTransactionReader(
                        @Value("#{jobParameters['inputFlatFile']}") Resource resource) {
                return new FlatFileItemReaderBuilder<Transaction>()
                                .name("transactionItemReader")
                                .resource(resource)
                                .saveState(false) // (1)
                                .delimited()
                                .names(new String[] {"account", "amount", "timestamp"})
                                .fieldSetMapper(fieldSet -> {
                                        Transaction transaction = new Transaction();
                                        transaction.setAccount(fieldSet.readString("account"));
                                        transaction.setAmount(fieldSet.readBigDecimal("amount"));
                                        transaction.setTimestamp(fieldSet.readDate("timestamp", "yyyy-MM-dd HH:mm:ss"));
                                        return transaction;
                                })
                                .build();
        }
        @Bean
        @StepScope
        public JdbcBatchItemWriter<Transaction> writer(DataSource dataSource) {
                return new JdbcBatchItemWriterBuilder<Transaction>()
                                .dataSource(dataSource)
                                .sql("INSERT INTO TRANSACTION (ACCOUNT, AMOUNT, TIMESTAMP) VALUES (:account, :amount, :timestamp)")
                                .beanMapped()
                                .build();
        }
        @Bean
        public Job multithreadedJob() {
                return this.jobBuilderFactory.get("multithreadedJob")
                                .start(step1())
                                .build();
        }
        @Bean
        public Step step1() {
                return this.stepBuilderFactory.get("step1")
                                .<Transaction, Transaction>chunk(100)
                                .reader(fileTransactionReader(null))
                                .writer(writer(null))
                                .taskExecutor(new SimpleAsyncTaskExecutor())
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