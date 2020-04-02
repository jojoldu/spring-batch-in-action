# Spring Batch에서 MultiThread로 Step 실행하기


![intro](./images/intro.png)



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

* ```SimpleAsyncTaskExecutor``` 를 사용할 경우 **매 요청시마다 쓰레드를 생성**하게 됩니다.

(3) ```throttleLimit(poolSize)```

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