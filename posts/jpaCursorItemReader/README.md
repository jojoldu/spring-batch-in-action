# Spring Batch JpaCursorItemReader 도입

Spring Batch 4.3이 릴리즈 되면서 JpaCursorItemReader 가 도입되었습니다.  

![intro](./images/intro.png)

([Spring Batch 4.3 release notes](https://github.com/spring-projects/spring-batch/releases/tag/4.3.0))

그 전 버전까지 (~4.2.x)는 JpaCursorItemReader가 없었음을 의미하는데요.  
**HibernateCursorItemReader는 존재하는데**, 왜 JpaCursorItemReader는 여태 없었던 것이지? 라고 의문이 들 수 있습니다.  
  
이는 JPA 스펙 때문인데, JPA 2.1 전까지는 데이터 스트리밍이 가능한 스펙이 별도로 없었습니다.  

그래서 Hibernate의 상태 비저장 세션 (`StatelessSession`)과 유사한 개념이 JPA에는 없어서 Cursor 기능을 구현할 수 없었습니다. 

> 상태 비저장 세션 (`StatelessSession`) 은 **Hibernate에서만 지원하는 모드**로서 1차/2차 캐시가 없고 상태가 없는 세션 (Session) 모드를 이야기합니다.  
> 가장 Jdbc와 유사한 형태의 데이터 조회가 가능하여 일반적으로 데이터베이스에서 데이터를 스트리밍할때 주로 사용됩니다.

그러다 JPA 2.2부터 드디어 ```Query#getResultStream()``` 가 도입되어 이런 데이터 스트리밍이 가능하게 되었는데요.  
([Add ability to stream the result of a query execution](https://github.com/eclipse-ee4j/jpa-api/issues/99))  
  
JPA 2.2 스펙 도입이 예전에 되었지만, 스프링 배치에서는 최근에서야 이 부분을 적용하게 되어 드디어 스프링 배치 4.3부터 Jpa에도 CursorItemReader가 도입되게 되었습니다.  
  
기본적인 작동원리는 기존의 다른 CursorItemReader (Jdbc/Hibernate)와 다르지 않습니다.  
  
자 그럼 실제 간단한 예제를 통해 JpaCursorItemReader를 배워보겠습니다.

## 1. 예제

빠르게 `JpaCursorItemReader`를 활용한 예제 코드를 만들어보겠습니다.

```java
@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class JpaCursorItemReaderJobConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private int chunkSize;
    @Value("${chunkSize:100}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Bean
    public Job jpaCursorItemReaderJob() {
        return jobBuilderFactory.get("jpaCursorItemReaderJob")
                .start(jpaCursorItemReaderStep())
                .build();
    }

    @Bean
    public Step jpaCursorItemReaderStep() {
        return stepBuilderFactory.get("jpaCursorItemReaderStep")
                .<Pay, Pay>chunk(chunkSize)
                .reader(jpaCursorItemReader())
                .writer(jpaCursorItemWriter())
                .build();
    }

    @Bean
    public JpaCursorItemReader<Pay> jpaCursorItemReader() {
        return new JpaCursorItemReaderBuilder<Pay>()
                .name("jpaCursorItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT p FROM Pay p")
                .build();
    }

    private ItemWriter<Pay> jpaCursorItemWriter() {
        return list -> {
            for (Pay pay: list) {
                log.info("Current Pay={}", pay);
            }
        };
    }
}
```

기존의 [JpaPagingItemReader](https://jojoldu.tistory.com/336)와 크게 다르지 않는 포맷인데요.  
각 설정들이 하는 역할은 다음과 같습니다.

|속성                   |소개           |기본값             |
|----------------------|-------------|-------------------|
| name                 | 실행 컨텍스트 (ExecutionContext) 내에서 구분하기 위한 Key. <br/>`saveState` 가 `true` 로 설정된 경우 필수|                   
| entityManagerFactory | JPA를 사용하기 위한 EntityManagerFactory |        |           
| queryString          | 사용할 JPQL 쿼리문            |                   |
| maxItemCount         | 조회할 최대 item 수             | Integer.MAX_VALUE |
| currentItemCount     | 조회 Item의 시작지점            | 0                 |
| saveState            | 동일 Job 재실행시 실행 컨텍스트 내에서 ItemStream Support의 상태를 유지할지 여부  | true |

JpaPagingItemReader와 달리 JpaCursorItemReader에는 `pageSize` 설정이 없고, `maxItemCount`, `currentItemCount` 이 추가되었습니다.  
Cursor 방식이 스트리밍이기 때문에 한번에 몇개의 데이터를 읽어올지를 결정하는 `pageSize` 는 Cursor에서는 필요가 없습니다.  
그리고 `maxItemCount`, `currentItemCount` 의 경우에는 다음과 같은 역할을 하는데요.  
  
예를 들어 아래와 같이 10개의 데이터가 조회되는 JpaCursorItemReader가 있다고 하겠습니다.

```bash
Current Pay=Pay(id=1, amount=0, txName=0, txDateTime=2021-01-24T19:36:33.690)
Current Pay=Pay(id=2, amount=1000, txName=1, txDateTime=2021-01-24T19:36:33.735)
Current Pay=Pay(id=3, amount=2000, txName=2, txDateTime=2021-01-24T19:36:33.736)
Current Pay=Pay(id=4, amount=3000, txName=3, txDateTime=2021-01-24T19:36:33.737)
Current Pay=Pay(id=5, amount=4000, txName=4, txDateTime=2021-01-24T19:36:33.738)
Current Pay=Pay(id=6, amount=5000, txName=5, txDateTime=2021-01-24T19:36:33.739)
Current Pay=Pay(id=7, amount=6000, txName=6, txDateTime=2021-01-24T19:36:33.740)
Current Pay=Pay(id=8, amount=7000, txName=7, txDateTime=2021-01-24T19:36:33.740)
Current Pay=Pay(id=9, amount=8000, txName=8, txDateTime=2021-01-24T19:36:33.741)
Current Pay=Pay(id=10, amount=9000, txName=9, txDateTime=2021-01-24T19:36:33.742)
```

여기서 `.maxItemCount(5)` 를 추가해서 수행하게 되면 다음과 같이 **5개만** 최대 조회 됩니다. 
즉, `.maxItemCount` 이란 **최대로 조회할 데이터 갯수**를 설정하는 것입니다.

```bash
Current Pay=Pay(id=1, amount=0, txName=0, txDateTime=2021-01-24T19:38:39.569)
Current Pay=Pay(id=2, amount=1000, txName=1, txDateTime=2021-01-24T19:38:39.616)
Current Pay=Pay(id=3, amount=2000, txName=2, txDateTime=2021-01-24T19:38:39.617)
Current Pay=Pay(id=4, amount=3000, txName=3, txDateTime=2021-01-24T19:38:39.618)
Current Pay=Pay(id=5, amount=4000, txName=4, txDateTime=2021-01-24T19:38:39.619)
```

이 외에 `.currentItemCount(2)` 를 추가하게 되면 다음과 같이 `.currentItemCount` **지정값 다음부터 데이터를 조회**하게 됩니다.

* `.maxItemCount(5)`
* `.currentItemCount(2)`

```bash
Current Pay=Pay(id=3, amount=2000, txName=2, txDateTime=2021-01-24T19:35:28.344)
Current Pay=Pay(id=4, amount=3000, txName=3, txDateTime=2021-01-24T19:35:28.345)
Current Pay=Pay(id=5, amount=4000, txName=4, txDateTime=2021-01-24T19:35:28.346)
```

* `.maxItemCount(5)`를 통해 최대 5개를 조회하도록 제한 뒤,
* `.currentItemCount(2)` 를 통해 총 읽어야할 데이터 중 시작지점을 어디로 할지

각각의 설정들을 알아보았으니, 이제 테스트 코드로 검증을 해보겠습니다.

## 2. 테스트 코드

> 전체 코드는 [Github](https://github.com/jojoldu/spring-batch-in-action/tree/master/src/test/java/com/jojoldu/batch)에 있습니다.

Junit5를 통해 테스트 코드를 작성합니다.

```java
@ExtendWith(SpringExtension.class)
@SpringBatchTest
@SpringBootTest(classes = {JpaCursorItemReaderJobConfig.class, TestBatchConfig.class})
public class JpaCursorItemReaderJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PayRepository payRepository;

    @AfterEach
    public void tearDown() throws Exception {
        payRepository.deleteAllInBatch();
    }

    @SuppressWarnings("Duplicates")
    @Test
    void JPA_Cursor_조회() throws Exception {
        //given
        for (long i = 0; i < 10; i++) {
            payRepository.save(new Pay(i * 1000, String.valueOf(i), LocalDateTime.now()));
        }

        JobParameters jobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
                .addString("version", "1")
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
``` 

총 10개의 pay 엔티티를 등록하고, 이들이 잘 노출되는지 검증하는 것인데요.  
수행해보시면, 아래와 같이 Cursor용 select쿼리와 writer 로그를 볼 수 있습니다.

```sql
Hibernate: select pay0_.id as id1_0_, pay0_.amount as amount2_0_, pay0_.tx_date_time as tx_date_3_0_, pay0_.tx_name as tx_name4_0_ from pay pay0_
```

```bash
Current Pay=Pay(id=1, amount=0, txName=0, txDateTime=2021-01-24T19:36:33.690)
Current Pay=Pay(id=2, amount=1000, txName=1, txDateTime=2021-01-24T19:36:33.735)
Current Pay=Pay(id=3, amount=2000, txName=2, txDateTime=2021-01-24T19:36:33.736)
Current Pay=Pay(id=4, amount=3000, txName=3, txDateTime=2021-01-24T19:36:33.737)
Current Pay=Pay(id=5, amount=4000, txName=4, txDateTime=2021-01-24T19:36:33.738)
Current Pay=Pay(id=6, amount=5000, txName=5, txDateTime=2021-01-24T19:36:33.739)
Current Pay=Pay(id=7, amount=6000, txName=6, txDateTime=2021-01-24T19:36:33.740)
Current Pay=Pay(id=8, amount=7000, txName=7, txDateTime=2021-01-24T19:36:33.740)
Current Pay=Pay(id=9, amount=8000, txName=8, txDateTime=2021-01-24T19:36:33.741)
Current Pay=Pay(id=10, amount=9000, txName=9, txDateTime=2021-01-24T19:36:33.742)
```

## 마무리

JpaCursorItemReader를 통해 HQL이 아닌 JPQL로도 데이터 스트리밍 배치를 구현할 수 있게 되었습니다.  
Cursor를 이용하여 1) 데이터 변경에 무관한 무결성 조회 2) 페이징 보다 높은 성능 의 배치 조회가 가능합니다.  
단, 페이징과 달리 타임아웃이 굉장히 길어야하니 이 점은 주의해야겠죠?  
  
스프링 배치는 여전히 발전중이라서, 이후에도 추가되는 기능 중 많은 분들이 도움 될만한 요소가 있다면 공유하겠습니다.