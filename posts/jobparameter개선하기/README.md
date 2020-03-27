# Spring Batch에서 JobParameter 활용 방법

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

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); 
    LocalDate createDate = LocalDate.parse(createDateStr, formatter); // (3)

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

(1) ```reader(null, null)```

* Reader에서 Job Parameter를 사용하기 위해 Reader를 호출하는 Step에서는 ```null``` 이라는 임시값을 강제로 등록

(2) ```@Value("#{jobParameters[createDate]}") String createDateStr```

* 실제 Reader에서는 ```null``` 이 넘어와도 ```@Value("#{jobParameters[createDate]}")``` 로 인해 JobParameter 값으로 교체되어 사용
* Spring Batch JobParameter는 LocalDate를 지원하지 않으니, 일단 문자열로 받아서 ```LocalDate```로 변환한다

(3) ```LocalDate.parse(createDateStr, formatter);```

* 문자열로 받은 값을 ```LocalDate```로 변환한다

이 과정의 문제점은 뭐가 있을까요?  

* 형변환 (```LocalDate```) 된 Parameter 값을 재사용할 수가 없습니다.
    * 결국은 Reader/Processor/Writer 생성 메소드 혹은 별도 클래스 에서 각자 형변환을 해야만 합니다.
* Job Parameter 확장성이 떨어집니다.
    * 위 문제와 이어지는 것인데, 예를 들어 ```2020.03```으로 파라미터가 넘어오면 Reader에서 ```2020.03.01``` 과 ```2020.03.31```로 2개의 값으로 분리되서 필요하다면 어떻게 해야할까요?
    * 결국 Reader에서 파라미터를 받아 본인이 원하는 형태로 분리하는 로직도 함께 갖고 있어야만 합니다.
    * Reader 가 해야할 일은 **기간별 조회** 기능 + **월로 넘어온 값을 시작일/종료일로 분리하기**라는 2가지 기능을 같이 해야만 하는 경우가 생기는 것이죠.
     
위 문제들의 해결책은 무엇일까요?  
**JobParameter에 관한 모든 기능을 담당할** Job Parameter 클래스가 있으면 됩니다.  
그리고 해당 Class는 DI를 받을 수 있게 Spring Bean (```@JobScope```를 가진) 이면 별도의 Reader/Processor/Writer에서도 쉽게 DI 받을 수 있겠죠?  


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

## 2. JobParameter 클래스

### 2-1. Setter 로 주입 받기

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
@Slf4j
@RequiredArgsConstructor
@Configuration
public class JobParameterExtendsConfiguration {
    
    ...
    private final CreateDateJobParameter jobParameter;
    ...
    
    @Bean(JOB_NAME + "jobParameter")
    @JobScope
    public CreateDateJobParameter jobParameter() {
        return new CreateDateJobParameter();
    }
    
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
}




```

### 2-2. Constructor 로 주입 받기

다만 이럴 경우 JobParameter 클래스에서 ```@Component```가 선언되어있어야만 합니다.  
Setter처럼 ```@Bean``` 을 사용하려면 결국 생성자에 빈값이라도 넣어줘야해서 

```java
@Slf4j
@Getter
public class CreateDateJobParameter {

    private LocalDate createDate;
    private ProductStatus status;
    
    public CreateDateJobParameter(String createDateStr, ProductStatus status) {
        this.createDate = LocalDate.parse(createDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.status = status;
    }
}
```

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class JobParameterExtendsConfiguration {
    ...
    private final CreateDateJobParameter jobParameter;
    
    @Bean(JOB_NAME + "jobParameter")
    @JobScope
    public CreateDateJobParameter jobParameter(@Value("#{jobParameters[createDate]}") String createDateStr,
                                               @Value("#{jobParameters[status]}") ProductStatus status) {
        return new CreateDateJobParameter(createDateStr, status);
    }
    ...

    @Bean(name = JOB_NAME +"_reader")
    @StepScope
    public JpaPagingItemReader<Product> reader() {

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
}
```
### 2-3. Field로 주입 받기 (SpEL만으로)

### 2-4. Field로 주입 받기 (SpEL + Converter Class로 처리하기)