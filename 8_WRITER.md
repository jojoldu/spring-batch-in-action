# 8. ItemWriter

앞 시간에 Reader에 대해서 배웠습니다.  
Writer는 Reader, Prcessor와 함께 ChunkOrientedTasklet을 구성하는 3 요소입니다.  
여기서 Processor가 아닌 Writer를 우선 선택한 이유는 **Processor는 선택**이기 때문입니다.  Processor는 없어도 ChunkOrientedTasklet는 구성할 수 있습니다.
반면 Reader와 Writer는 ChunkOrientedTasklet에서 필수 요소입니다.  
  
그래서 Writer를 먼저 다뤄보겠습니다.  
  
## 8-1. ItemWriter 소개

ItemWriter는 Spring Batch에서 사용하는 **출력** 기능입니다.  
Spring Batch가 처음 나왔을 때, ItemWriter는 ItemReader와 마찬가지로 item을 하나씩 다루었습니다.  
그러나 Spring Batch2와 청크 (Chunk) 기반 처리의 도입으로 인해 ItemWriter에도 큰 변화가 있었습니다.  
  
이 업데이트 이후 부터 ItemWriter는 item 하나를 작성하지 않고 **Chunk 단위로 묶인 item List**를 다룹니다.  
이 때문에 ItemWriter 인터페이스는 ItemReader 인터페이스와 약간 다릅니다.  

![itemwriter1](./images/8/itemwriter1.png)

[쳅터 7](https://jojoldu.tistory.com/336)을 보신 분들은 아시겠지만, Reader의 ```read()```는 Item 하나를 반환하는 반면, Writer의 ```write()```는 인자로 Item List를 받습니다.  

이를 그림으로 표현하면 아래와 같습니다.

![write-process](./images/8/write-process.png)

* ItemReader를 통해 각 항목을 개별적으로 읽고 이를 처리하기 위해 ItemProcessor에 전달합니다.  
* 이 프로세스는 청크의 Item 개수 만큼 처리 될 때까지 계속됩니다.  
* 청크 단위만큼 처리가 완료되면 Writer에 전달되어 Writer에 명시되어있는대로 일괄처리합니다.

즉, Reader와 Processor를 거쳐 처리된 Item을 Chunk 단위 만큼 쌓은 뒤 이를 Writer에 전달하는 것입니다.  

> 위 내용은 이미 쳅터6 Chunk 지향 처리에서 상세하게 언급되었습니다.

Spring Batch는 다양한 Output 타입을 처리 할 수 있도록 많은 Writer를 제공합니다.  
Reader와 마찬가지로, 모든 내용을 다루기는 어렵기 때문에 Database와 관련된 내용들만 다루겠습니다.

## 8-2. Database Writer

Java 세계에서는 JDBC 또는 ORM을 사용하여 RDBMS에 접근합니다.  
Spring Batch는 JDBC와 ORM 모두 Writer를 제공합니다.  
Writer는 Chunk단위의 마지막 단계입니다.  
그래서 Database의 영속성과 관련해서는 **항상 마지막에 Flush를 해줘야만** 합니다.  
  
예를 들어 아래와 같이 영속성을 사용하는 JPA, Hibernate의 경우 ItemWriter 구현체에서는 ```flush()```와 ```session.clear()```가 따라옵니다. 

![flush1](./images/8/flush1.png)

(JpaItemWriter)

![flush2](./images/8/flush2.png)

(HibernateItemWriter)  
  
Writer가 받은 모든 Item이 처리 된 후, Spring Batch는 현재 트랜잭션을 커밋합니다.  

데이터베이스와 관련된 Writer는 아래와 같이 3가지가 있습니다.

* JdbcBatchItemWriter
* HibernateItemWriter
* JpaItemWriter

이 중에서 많은 분들이 사용하고 계신 JdbcBatchItemWriter와 JpaItemWriter를 소개드리겠습니다.

## 8-3. JdbcBatchItemWriter

ORM을 사용하지 않는 경우 Writer는 대부분 JdbcBatchItemWriter를 사용합니다.  
이 JdbcBatchItemWriter는 아래 그림과 같이 **JDBC의 Batch 기능을 사용하여 한번에 Database로 전달하여 Database 내부에서 쿼리들이 실행**되도록 합니다.

![jdbcwrite-flow](./images/8/jdbcwrite-flow.png)

이렇게 처리하는 이유는 어플리케이션과 데이터베이스 간에 데이터를 주고 받는 회수를 최소화 하여 성능 향상을 꾀하기 위함입니다.  

> [JdbcTemplate.batchUpdate](https://docs.spring.io/spring/docs/3.0.0.M4/reference/html/ch12s04.html)의 공식 문서 내용을 참고하시면 같은 내용을 알 수 있습니다.

* 업데이트를 일괄 처리로 그룹화하면 데이터베이스와 어플리케이션간 왕복 횟수가 줄어들어 성능이 향상 됩니다.

실제로 JdbcBatchItemWriter의 ```write()```를 확인해보시면 일괄처리 하는 것을 확인할 수 있습니다.

![jdbcwrite](./images/8/jdbcwrite.png)

그럼 ```JdbcBatchItemWriter```로 간단한 배치를 하나 작성해보겠습니다.

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class JdbcBatchItemWriterJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource; // DataSource DI

    private static final int chunkSize = 10;

    @Bean
    public Job jdbcBatchItemWriterJob() {
        return jobBuilderFactory.get("jdbcBatchItemWriterJob")
                .start(jdbcBatchItemWriterStep())
                .build();
    }

    @Bean
    public Step jdbcBatchItemWriterStep() {
        return stepBuilderFactory.get("jdbcBatchItemWriterStep")
                .<Pay, Pay>chunk(chunkSize)
                .reader(jdbcBatchItemWriterReader())
                .writer(jdbcBatchItemWriter())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Pay> jdbcBatchItemWriterReader() {
        return new JdbcCursorItemReaderBuilder<Pay>()
                .fetchSize(chunkSize)
                .dataSource(dataSource)
                .rowMapper(new BeanPropertyRowMapper<>(Pay.class))
                .sql("SELECT id, amount, tx_name, tx_date_time FROM pay")
                .name("jdbcBatchItemWriter")
                .build();
    }

    /**
     * reader에서 넘어온 데이터를 하나씩 출력하는 writer
     */
    @Bean // beanMapped()을 사용할때는 필수
    public JdbcBatchItemWriter<Pay> jdbcBatchItemWriter() {
        return new JdbcBatchItemWriterBuilder<Pay>()
                .dataSource(dataSource)
                .sql("insert into pay2(amount, tx_name, tx_date_time) values (:amount, :txName, :txDateTime)")
                .beanMapped()
                .build();
    }
}

```

대부분의 코드는 [Reader 쳅터](https://jojoldu.tistory.com/336)때 사용한 코드와 비슷합니다.  
JdbcBatchItemWriterBuilder를 사용하는 코드만 조금 다르기 때문에 조금 자세히 설명드리겠습니다.  
  
JdbcBatchItemWriterBuilder는 다음과 같은 설정값을 갖고 있습니다

|  Property     |  Parameter Type     |  설명   |
|  ---                          |  ---                              |  ---  |
| assertUpdates                 | boolean |  적어도 하나의 항목이 행을 업데이트하거나 삭제하지 않을 경우 예외를 throw할지 여부를 설정합니다. 기본값은 ```true```입니다. Exception:```EmptyResultDataAccessException```     | 
| columnMapped        | 없음 | Key,Value 기반으로 Insert SQL의 Values를 매핑합니다 (ex: ```Map<String, Object>```)      |
| beanMapped        | 없음  | Pojo 기반으로 Insert SQL의 Values를 매핑합니다      |

여기서 ```columnMapped```과 ```beanMapped```의 차이가 궁금하실것 같습니다.
예를 들면 위 예제는 ```beanMapped```로 작성되었습니다.  
만약 위 예제를 ```columnMapped``` 로 변경하면 다음과 같은 코드가 됩니다.

```java

new JdbcBatchItemWriterBuilder<Map<String, Object>>() // Map 사용
				.columnMapped()
				.dataSource(this.dataSource)
				.sql("insert into pay2(amount, tx_name, tx_date_time) values (:amount, :txName, :txDateTime)")
				.build();

```

차이는 간단합니다. Reader에서 Writer로 넘겨주는 타입이 ```Map<String, Object>``` 냐, ```Pay.class```와 같은 Pojo 타입이냐 입니다.  
  
그 외 궁금해 하실만한 것은 ```values(:field)```일 것 같습니다.  
이 값의 경우 **Dto의 Getter 혹은 Map의 Key**에 매핑되어 값이 할당 됩니다.
  
추가로 JdbcBatchItemWriterBuilder가 아닌 ```JdbcBatchItemWriter```의 설정에서 주의하실게 하나 있습니다.

* JdbcBatchItemWriter의 제네릭 타입은 **Reader에서 넘겨주는 값의 타입**입니다.

Spring Batch를 처음 쓰시는 분들이 자주 오해하시는게 이 부분입니다.  
위 코드에서도 나와있지만, **Pay2 테이블에 데이터를 넣은 Writer이지만 선언된 제네릭 타입은 Reader/Processor에서 넘겨준 Pay클래스**입니다.  
  
이외에도 추가로 아셔야할 메소드는 ```afterPropertiesSet``` 정도가 있습니다.  
이 메소드는 ```InitializingBean``` 인터페이스 에서 갖고 있는 메소드입니다.  
JdbcBatchItemWriter, JpaItemWriter등 ItemWriter의 구현체들은 모두 ```InitializingBean``` 인터페이스를 구현하고 있는데요.  
여기서 ```afterPropertiesSet```가 하는 일은 각각의 Writer들이 실행되기 위해 필요한 필수값들이 제대로 세팅되어있는지를 체크합니다.

![afterpropertiesset1](./images/8/afterpropertiesset1.png)

Writer를 생성하시고 위 메소드를 그 아래에서 바로 실행해보시면 어느 값이 누락되었는지 명확하게 인지할 수 있어서 많이들 사용하는 옵션입니다.

## 8-4. JpaItemWriter

두번째로 알아볼 Writer는 ORM을 사용할 수 있는 ```JpaItemWriter```입니다.  
Writer에 전달하는 데이터가 Entity 클래스라면 JpaItemWriter를 사용하시면 됩니다.  
  
바로 샘플 코드로 가보겠습니다.

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class JpaItemWriterJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;
    
    private static final int chunkSize = 10;

    @Bean
    public Job jpaItemWriterJob() {
        return jobBuilderFactory.get("jpaItemWriterJob")
                .start(jpaItemWriterStep())
                .build();
    }

    @Bean
    public Step jpaItemWriterStep() {
        return stepBuilderFactory.get("jpaItemWriterStep")
                .<Pay, Pay2>chunk(chunkSize)
                .reader(jpaItemWriterReader())
                .processor(jpaItemProcessor())
                .writer(jpaItemWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Pay> jpaItemWriterReader() {
        return new JpaPagingItemReaderBuilder<Pay>()
                .name("jpaItemWriterReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(chunkSize)
                .queryString("SELECT p FROM Pay p")
                .build();
    }

    @Bean
    public ItemProcessor<Pay, Pay2> jpaItemProcessor() {
        return pay -> new Pay2(pay.getAmount(), pay.getTxName(), pay.getTxDateTime());
    }

    @Bean
    public JpaItemWriter<Pay2> jpaItemWriter() {
        JpaItemWriter<Pay2> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
        return jpaItemWriter;
    }
}
```

JpaItemWriter는 JPA를 사용하기 때문에 영속성 관리를 위해 EntityManager를 할당해줘야 합니다.  

> 일반적으로 ```spring-boot-starter-data-jpa```를 의존성에 등록하면 Entity Manager가 Bean으로 자동생성되어 DI 코드만 추가해주시면 됩니다.  

대신 **필수로 설정해야할 값이 EntityManager뿐**입니다.  
JdbcBatchItemWriter에 비해 필수값이 Entity Manager 뿐이라 체크할 요소가 적다는 것이 장점아닌 장점입니다.

![afterpropertiesset2](./images/8/afterpropertiesset2.png)

(필수값 체크 메소드인 ```afterPropertiesSet```)  
  
EntityManager만 ```set``` 하면 모든 설정은 끝납니다.  
  
여기서 한가지 JdbcBatchItemWriter와 다른것이 있다면 processor가 추가 되었습니다.  
이유는 Pay Entity를 읽어서 Writer에는 Pay2 Entity를 전달해주기 위함입니다.  

> Reader에서 읽은 데이터를 가공해야할 때 Processor가 필요합니다.  

JpaItemWriter는 JdbcBatchItemWriter와 달리 **넘어온 Entity를 데이터베이스에 반영**합니다.  
  
즉, JpaItemWriter는 **Entity 클래스를 제네릭 타입으로 받아야만 합니다**.  
JdbcBatchItemWriter의 경우 DTO 클래스를 받더라도 ```sql```로 지정된 쿼리가 실행되니 문제가 없지만, JpaItemWriter 는 넘어온 Item을 그대로 ```entityManger.merge()```로 테이블에 반영을 하기 때문입니다.

![dowrite](./images/8/dowrite.png)

(```JpaItemWriter.doWrite()```)  
  

이렇게만 설정하시면 JpaItemWriter의 사용법은 끝입니다.  
실제로 실행해보시면 정상적으로 결과가 나오는것을 확인할 수 있습니다.

![jpaitemwriter-result](./images/8/jpaitemwriter-result.png)

## 8-5. Custom ItemWriter

Reader와 달리 Writer의 경우 Custom하게 구현해야할 일이 많습니다.

> 물론 Reader 역시 조회용 프레임워크를 어떤걸 쓰는지에 따라 Reader를 Custom 하게 구현해야할 수도 있습니다.  
예를 들면 Querydsl기반의 ItemReader를 만든다거나, Jooq 기반의 ItemReader를 만드는 등이 있을 수 있습니다.

예를 들어 다음과 같은 경우가 있습니다.

* Reader에서 읽어온 데이터를 RestTemplate으로 외부 API로 전달해야할때
* 임시저장을 하고 비교하기 위해 싱글톤 객체에 값을 넣어야할때
* 여러 Entity를 동시에 save 해야할때

등등 여러 상황이 있습니다.  
이렇게 Spring Batch에서 공식적으로 지원하지 않는 Writer를 사용하고 싶을때 **ItemWriter인터페이스를 구현**하시면 됩니다.  
  
아래는 processor에서 넘어온 데이터를 ```System.out.println``` 으로 출력하는 Writer를 만든 경우입니다.

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class CustomItemWriterJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;
    
    private static final int chunkSize = 10;

    @Bean
    public Job customItemWriterJob() {
        return jobBuilderFactory.get("customItemWriterJob")
                .start(customItemWriterStep())
                .build();
    }

    @Bean
    public Step customItemWriterStep() {
        return stepBuilderFactory.get("customItemWriterStep")
                .<Pay, Pay2>chunk(chunkSize)
                .reader(customItemWriterReader())
                .processor(customItemWriterProcessor())
                .writer(customItemWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Pay> customItemWriterReader() {
        return new JpaPagingItemReaderBuilder<Pay>()
                .name("customItemWriterReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(chunkSize)
                .queryString("SELECT p FROM Pay p")
                .build();
    }

    @Bean
    public ItemProcessor<Pay, Pay2> customItemWriterProcessor() {
        return pay -> new Pay2(pay.getAmount(), pay.getTxName(), pay.getTxDateTime());
    }

    @Bean
    public ItemWriter<Pay2> customItemWriter() {
        return new ItemWriter<Pay2>() {
            @Override
            public void write(List<? extends Pay2> items) throws Exception {
                for (Pay2 item : items) {
                    System.out.println(item);
                }
            }
        };
    }
}
```

보시는것처럼 ```write()```만 ```@Override``` 하시면 구현체 생성은 끝납니다.  
위 코드는 Java7 이하일 경우 사용하시면 되지만, Java8 이상을 사용하시는 경우 아래처럼 람다식을 사용하시면 더욱 깔끔하게 구현할 수 있습니다.

```java
   @Bean
   public ItemWriter<Pay2> customItemWriter() {
       return items -> {
           for (Pay2 item : items) {
               System.out.println(item);
           }
       };
   }
```


## 8-6. 주의 사항

ItemWriter를 사용할 때 **Processor에서 Writer에 List를 전달**하고 싶을때가 있습니다.  
이때 ItemWriter의 제네릭을 List로 선언해서는 문제를 해결할 수 없는데요.  
해결할 수 있는 방법을 아래 링크에 상세하게 작성했으니 참고하시면 좋을것 같습니다.

* [Writer에 List형 Item을 전달하고 싶을때](https://jojoldu.tistory.com/140)