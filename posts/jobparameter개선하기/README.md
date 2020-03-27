# Spring Batch에서 JobParameter 좀 더 잘 쓰는 법

Spring Batch에서는 Spring Environment Variables (환경 변수) 외에 Batch에서만 사용할 수 있는 JobParameter를 지원합니다.  

> [Spring Batch Scope와 Job Parameter](https://jojoldu.tistory.com/330)

기존에 사용하던 방식으로는 불편한 점이나 단점이 많아 이를 어떻게 해결하면 좋을지에 대해 정리하게 되었습니다.  
  
기존에 사용하던 방식은 무엇인지,  
해당 방식에 어떤 단점이 있는지,  
개선할 수 있는 방법은 무엇인지 등등을 정리해보았으니 Job Parameter를 사용하면서 불편하셨던 분들은 참고하시면 좋을것 같습니다.
  

> 기본적인 내용은 아래 호돌맨님의 블로그 글을 참고하시면 더욱 좋습니다.  
> [호돌맨 - SpringBoot Batch에서 JobParameter로 받을 수 있는 Type](https://hodolman.tistory.com/17)


> 모든 코드는 [Github](https://github.com/jojoldu/spring-batch-in-action/tree/master/src/main/java/com/jojoldu/spring/springbatchinaction/jobparameter)에 있습니다.

## 1. 기존 방식

Spring Batch에서 Job Parameter를 사용하기 위해서는 일반적으로 다음과 같이 코드를 작성합니다.

![1](./images/1.png)

```java
@Bean(name = JOB_NAME +"_step")
@JobScope
public Step step() {
    return stepBuilderFactory.get(JOB_NAME +"_step")
            .<Product, Product>chunk(chunkSize)
            .reader(reader(null, null)) // (1)
            .writer(writer())
            .build();
}

@Bean(name = JOB_NAME +"_reader")
@StepScope
public JpaPagingItemReader<Product> reader(
        @Value("#{jobParameters[status]}") ProductStatus status,
        @Value("#{jobParameters[createDate]}") String createDateStr) { // (2)

    validate();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // (3)
    LocalDate createDate = LocalDate.parse(createDateStr, formatter);

    Map<String, Object> params = new HashMap<>();
    params.put("createDate", createDate);
    params.put("status", status);
    log.info(">>>>>>>>>>> createDate={}, status={}", createDate, status);

    return new JpaPagingItemReaderBuilder<Product>()
            .name(JOB_NAME +"_reader")
            .entityManagerFactory(entityManagerFactory)
            .pageSize(chunkSize)
            .queryString("SELECT p FROM Product p WHERE p.createDate =:createDate AND p.status =:status")
            .parameterValues(params)
            .build();
}
```

* 


## 2. JobParameter 클래스

```java
@Slf4j
@Getter
@NoArgsConstructor
public class CreateDateJobParameter {

    @Value("#{jobParameters[status]}")
    private ProductStatus status;
    
    private LocalDate createDate;

    @Value("#{jobParameters[createDate]}")
    public void setCreateDate(String createDate) {
       DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
       this.createDate = LocalDate.parse(createDate, formatter);
   }
}
```

```java
@Bean(JOB_NAME + "jobParameter")
@JobScope
public CreateDateJobParameter jobParameter() {
    return new CreateDateJobParameter();
}
```

```java
@Bean(name = JOB_NAME +"_step")
@JobScope
public Step step() {
    return stepBuilderFactory.get(JOB_NAME +"_step")
            .<Product, Product>chunk(chunkSize)
            .reader(reader())
            .writer(writer())
            .build();
}


@Bean(name = JOB_NAME +"_reader")
@StepScope
public JpaPagingItemReader<Product> reader() {
    validate();

    Map<String, Object> params = new HashMap<>();
    params.put("createDate", jobParameter.getCreateDate());
    params.put("status", jobParameter.getStatus());
    log.info(">>>>>>>>>>> createDate={}, status={}", jobParameter.getCreateDate(), jobParameter.getStatus());

    return new JpaPagingItemReaderBuilder<Product>()
            .name(JOB_NAME +"_reader")
            .entityManagerFactory(entityManagerFactory)
            .pageSize(chunkSize)
            .queryString("SELECT p FROM Product p WHERE p.createDate =:createDate AND p.status =:status")
            .parameterValues(params)
            .build();
}
```

### 테스트 코드

```java
@RunWith(SpringRunner.class)
@SpringBatchTest
@SpringBootTest(classes={JobParameterExtendsConfiguration.class, TestBatchConfig.class})
public class JobParameterExtendsConfigurationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @After
    public void tearDown() throws Exception {
        productRepository.deleteAll();
    }

    @Test
    public void jobParameter정상출력_확인() throws Exception{
        //given
        LocalDate createDate = LocalDate.of(2019,9,26);
        long price = 1000L;
        ProductStatus status = ProductStatus.APPROVE;
        productRepository.save(Product.builder()
                .price(price)
                .createDate(createDate)
                .status(status)
                .build());

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("createDate", createDate.toString())
                .addString("status", status.name())
                .toJobParameters();
        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
```


## 3. LocalDate 를 필드로 사용하기

### 3-1. SpEL만으로 처리하기

### 3-2. SpEL + Converter Class로 처리하기