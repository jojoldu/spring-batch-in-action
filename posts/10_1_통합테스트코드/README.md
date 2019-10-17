# 10. Spring Batch 테스트 코드

배치 애플리케이션이 웹 애플리케이션 보다 어려운 점을 꼽자면 QA를 많이들 얘기합니다.  
  
일반적으로 웹 애플리케이션의 경우 전문 테스터 분들 혹은 QA 분들이 전체 기능을 검증을 해주시는 반면, 
배치 애플리케이션의 경우 DB의 최종상태라던가 메세징큐의 발행내역 등 **개발자들이 직접 확인해주는 것** 외에는 검증 하기가 쉽진 않습니다.  
(별도의 어드민을 제공하는것도 포함입니다.)  
  
더군다나 개발자가 로컬 환경에서 배치 애플리케이션을 수행하는 것도 많은 수작업이 필요합니다.  
수정/삭제 등의 배치 애플리케이션이라면 **한번 수행할때마다 로컬 DB의 데이터를 원복**하고 다시 수행하는 작업을 반복해야 합니다.  
  
이러다보니 당연하게 테스트 코드의 필요성이 많이 강조됩니다.  
  
다행이라면 배치 애플리케이션은 웹 애플리케이션 보다 테스트 코드 작성이 좀 더 수월하고, 한번 작성하게 되면 그 효과가 좋습니다.  
  
아무래도 UI 검증이 필요한 웹 애플리케이션에 비해 **Java 코드에 대한 검증만** 필요한 배치 애플리케이션의 테스트 코드가 좀 더 수월합니다.  
  
이번 챕터에서는 스프링 배치 환경에서의 테스트 코드에 관해 배워봅니다.  

JUnit & Mockito 프레임워크와 H2를 이용한 테스트 환경 등에 대해서는 별도로 설명하지 않습니다.  
  
해당 프레임워크에 대한 기본적인 사용법은 이미 충분히 많은 자료들이 있으니 참고해서 봐주시면 됩니다.  
  
## 10-1. 통합 테스트

개인적인 생각으로 스프링 배치 테스트 코드는 ItemReader의 단위 테스트를 작성하는 것 보다 **통합 테스트 코드 작성이 좀 더 쉽다**고 생각합니다.  
  
스프링 배치 모듈들 사이에서 ItemReader만 뽑아내 **쿼리를 테스트 해볼 수 있는 환경**을 Setup 하려면 여러가지 장치가 필요합니다.  
  
물론 그렇다고 해서 항상 통합 테스트만 작성하라는 의미는 아닙니다.  
  
저 같은 경우 최근에는 배치의 테스트 코드를 작성할때 **Reader / Processor의 단위 테스트 코드를 먼저 작성** 후 통합 테스트 코드를 작성합니다.  
  
단위 테스트의 장점을 버리라는 의미는 아닙니다.  
  
단지 그동안 해오셨던 웹 애플리케이션의 테스트 코드와 달리 스프링 배치의 테스트 코드는 **특이성**이 있으니, 그 부분을 고려해 **쉽게 접근 가능한 통합 테스트 코드를 먼저** 배워보자는 의미입니다.  
  
그래서 먼저 해볼것은 스프링 배치의 통합 테스트 입니다.  
  
> 스프링 부트 배치 테스트를 사용하실때는 의존성에 ```spring-boot-starter-test``` 가 꼭 있어야만 합니다.

### 10-1-1. 4.0.x (부트 2.0) 이하 버전

스프링 배치 4.1 보다 아래 버전의 스프링 배치를 사용하신다면 다음과 같이 통합 테스트를 사용할 수 있습니다.  

> 스프링 부트 배치 기준으로는 **2.1.0 보다 하위 버전**이라고 보시면 됩니다.

```java
@RunWith(SpringRunner.class)
@SpringBootTest(classes={BatchJpaTestConfiguration.class, TestBatchLegacyConfig.class}) // (1)
public class BatchIntegrationTestJobConfigurationLegacyTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils; // (2)

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private SalesSumRepository salesSumRepository;

    @After
    public void tearDown() throws Exception {
        salesRepository.deleteAllInBatch();
        salesSumRepository.deleteAllInBatch();
    }

    @Test
    public void 기간내_Sales가_집계되어_SalesSum이된다() throws Exception {
        //given
        LocalDate orderDate = LocalDate.of(2019,10,6);
        int amount1 = 1000;
        int amount2 = 500;
        int amount3 = 100;

        salesRepository.save(new Sales(orderDate, amount1, "1"));
        salesRepository.save(new Sales(orderDate, amount2, "2"));
        salesRepository.save(new Sales(orderDate, amount3, "3"));

        JobParameters jobParameters = new JobParametersBuilder() 
                .addString("orderDate", orderDate.format(FORMATTER))
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters); // (3)

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        List<SalesSum> salesSumList = salesSumRepository.findAll();
        assertThat(salesSumList.size()).isEqualTo(1);
        assertThat(salesSumList.get(0).getOrderDate()).isEqualTo(orderDate);
        assertThat(salesSumList.get(0).getAmountSum()).isEqualTo(amount1+amount2+amount3);
    }
}
```

> 모든 코드는 [Github](https://github.com/jojoldu/spring-batch-in-action/tree/master/src/test/java/com/jojoldu/spring/springbatchinaction/exam10) 에 있습니다.


(1) ```@SpringBootTest(classes={...})```

* 통합 테스트 실행시 사용할 Java 설정들을 선택합니다.
* ```BatchJpaTestConfiguration``` : 테스트할 Batch Job
* ```TestBatchLegacyConfig``` : 배치 테스트 환경
  * 아래에서 좀 더 자세히 설명드리겠습니다.

(2) ```JobLauncherTestUtils```

* Batch Job을 테스트 환경에서 실행할 Utils 클래스입니다.
* CLI 등으로 실행하는 Job을 **테스트 코드에서 Job을 실행**할 수 있도록 지원합니다.

(3) ```jobLauncherTestUtils.launchJob(jobParameters)```

* **JobParameter와 함께 Job을 실행**합니다.
  * 운영 환경에서는 CLI로 배치를 수행하겠지만, 지금 같은 테스트 코드에서는 ```JobLauncherTestUtils``` 를 통해 Job을 수행하고 결과를 검증합니다.
* 해당 Job의 결과는 ```JobExecution```에 담겨 반환 됩니다.
* 성공적으로 Batch가 수행되었는지는 ```jobExecution.getStatus()```로 검증합니다.

(1)의 코드를 보시면 어떤 Batch를 수행할지 **Config 클래스로 지정**되었습니다.  
(여기서는 ```BatchJpaTestConfiguration``` Job이 수행되겠죠?)  
  
이외에 나머지 클래스들은 불러오지 않기 때문에 **실행 대상에서 자동으로 제외**됩니다.  
자동으로 제외될 수 있는 이유는 ```JobLauncherTestUtils```가 ```@Autowired setJob()```로 현재 Bean에 올라간 Job을 주입받기 때문인데요.  

![1](./images/1.png)

(```JobLauncherTestUtils```의 ```setJob``` 메소드)  
  
현재 실행하는 테스트 환경에서 ```Job``` 클래스의 Bean은 ```class={}```에 등록된 ```BatchJpaTestConfiguration```의 Job 하나 뿐이라 자동 선택되는 것입니다.  
  
이렇게 하지 않을 경우 JobLauncherTestUtils에서는 **여러개의 Job Bean 중 어떤것을 선택해야할지 알 수 없어 에러**가 발생합니다.  
  
그래서 ```@SpringBootTest(classes={...})``` 를 통해 **단일 Job Config**만 선택하도록 합니다.  
  
아마 이 코드를 보고 의아해하시는분들이 계실텐데요.  
이전에는 ```@ConditionalOnProperty```와 ```@TestPropertySource``` 를 사용하여 **특정 Batch Job**만 설정을 불러와 배치를 테스트 했습니다.  
  
다만 저 개인적으로 생각하는 이 방식의 단점들은 아래와 같습니다.  

1. 흔히 말하는 행사 코드가 많이 필요합니다.

* Batch Job에는 ```@ConditionalOnProperty(name = "job.name", havingValue = job명)```, 테스트 코드에서는 ```@TestPropertySource(properties = {"job.name=" + job명})``` 등의 코드가 항상 필요합니다.

2. 전체 테스트 수행시 매번 Spring Context가 재실행됩니다.

* 앞에서 얘기한 행사 코드인 ```@TestPropertySource```로 인해 **전체 테스트 수행시에는 매번 Spring의 Context가 다시 생성**됩니다.
* 단일 테스트 속도는 빠르나 **전체 테스트에선 너무나 느립니다**.

대신 장점도 있습니다.

1. Bean 충돌을 걱정안해도 된다.

* 운영 환경에서도 ```@ConditionalOnProperty``` 덕분에 Job / Step / Reader 등의 Bean 생성시 **다른 Job에서 사용된 Bean 이름**에 대해서 크게 신경쓰지 않아도 됩니다.

2. 운영 환경에서의 Spring 실행 속도가 빠르다.

* 1번과 마찬가지로 운영 환경에서 배치가 수행될때 단일 Job 설정들만 로딩되기 때문에 경량화된 상태로 실행 가능합니다.

둘 중 어느걸 쓰더라도 무방하다고 생각합니다.  
그래서 써보시고 마음에 드시는 방법으로 선택하시면 될 것 같습니다.  
  
저 같은 경우 현재 스프링 배치 공식 문서에서도 권장하는 방법인 ```@ContextConfiguration``` 를 사용 중입니다.  
  
> ```@SpringBootTest(classes={...})``` 는 내부적으로 ```@ContextConfiguration```를 사용하기 때문에 둘은 같습니다.
  
첫번째 단점인 **많은 행사코드** 문제가 ```@ContextConfiguration``` 를 통해 어느 정도는 해소됩니다.  
  
이 어노테이션은 ```ApplicationContext``` 에서 관리할 Bean과 Configuration 들을 지정할 수 있기 때문에 **특정 Batch Job**의 설정들만 가져와서 수행할 수 있습니다.  
  
Batch Job 코드에서는 별도로 ```@ConditionalOnProperty``` 등을 사용할 필요가 없습니다.  
**테스트 코드에서 해당 클래스만 별도로 호출**해서 사용하기 때문이죠.  
  
다만 이 방식을 선택해도 기본적으로 **전체 테스트 수행시 Spring Context가 재실행되는 것은 여전**합니다.  
다행인것은 ```@ContextConfiguration```를 선택한다면 테스트 코드를 어떻게 작성하냐에 따라서 **하나의 Spring Context를 사용하는 방법/각자의 Spring Context를 사용하는 방법을 선택**할 수 있습니다.  
  
> 하나의 Spring Context를 사용하는 방법에 대해서는 **별도의 테스트 포스팅**으로 소개드리겠습니다.  
> 일단 이 글에서는 처음 스프링 배치 테스트를 작성하는 분들의 기본을 잡는게 목적입니다.  

이런 이유로 ```@ConditionalOnProperty``` 대신에 ```@ContextConfiguration``` (```@SpringBootTest(classes={})```) 를 사용하여 Batch Job 클래스를 호출하였습니다.  
  
그럼 나머지 호출 대상인 ```TestBatchLegacyConfig``` 는 어떤 역할일까요?  
이는 해당 클래스의 코드를 바로 보면서 설명드리겠습니다.  
  
TestBatchLegacyConfig 의 코드는 아래와 같이 구성합니다.

```java
@Configuration
@EnableAutoConfiguration
@EnableBatchProcessing // (1)
public class TestBatchLegacyConfig {

    @Bean
    public JobLauncherTestUtils jobLauncherTestUtils() { // (2)
        return new JobLauncherTestUtils();
    }
}
```

(1) ```@EnableBatchProcessing```

* 배치 환경을 자동 설정합니다.
* 테스트 환경에서도 필요하기 때문에 별도의 설정에서 선언되어 사용합니다.
  * 모든 테스트 클래스에서 선언하는 불편함을 없애기 위함입니다.

(2) ```@Bean JobLauncherTestUtils```

* 스프링 배치 테스트 유틸인 ```JobLauncherTestUtils```을 Bean으로 등록합니다.
    * ```JobLauncherTestUtils``` 를 이용해서 JobParameter를 사용한 Job 실행 등이 이루어집니다.
* ```JobLauncherTestUtils``` Bean을 각 테스트 코드에서 ```@Autowired```로 호출해서 사용합니다.

4.1 아래 즉, 4.0.x 버전까지 쓰시는 분들은 위와 같이 테스트 코드를 작성하시면 됩니다.  
  
위 코드를 실제로 수행해보시면!

![2](./images/2.png)

테스트가 잘 수행된 것을 확인할 수 있습니다.

### 10-1-2. 4.1.x 이상 (부트 2.1) 버전

스프링 배치 4.1에서 새로운 어노테이션이 추가되었습니다.  
바로 ```@SpringBatchTest```입니다.  
해당 어노테이션을 추가하게되면 자동으로 ApplicationContext 에 테스트에 필요한 여러 유틸 Bean을 등록해줍니다.

> Tip) 다들 아시겠지만, ApplicationContext 은 Spring의 Bean 컨테이너입니다.  
> 여기에 Spring의 Bean들이 모두 담겨져있고, 가져와서 (```@Autowired```) 사용할 수 있다고 보시면 됩니다.

자동으로 등록되는 빈은 총 4개입니다.

* JobLauncherTestUtils
  * 스프링 배치 테스트에 필요한 전반적인 유틸 기능들을 지원
* JobRepositoryTestUtils
  * DB에 생성된 JobExecution을 쉽게 생성/삭제 가능하게 지원
* StepScopeTestExecutionListener
  * 배치 단위 테스트시 StepScope 컨텍스트를 생성
  * 해당 컨텍스트를 통해 JobParameter등을 단위 테스트에서 DI 받을 수 있음
* JobScopeTestExecutionListener
  * 배치 단위 테스트시 JobScope 컨텍스트를 생성
  * 해당 컨텍스트를 통해 JobParameter등을 단위 테스트에서 DI 받을 수 있음

여기서 ```JobLauncherTestUtils```와 ```JobRepositoryTestUtils```는 통합 테스트에 필요한 Bean들이며, ```StepScopeTestExecutionListener```와 ```JobScopeTestExecutionListener```는 단위 테스트 환경에서 필요한 Bean 들 입니다.  
  
스프링 배치 테스트 코드 작성에 필요한 Bean들을 미리 다 제공해준다고 생각하시면 됩니다.  
자 그럼 ```@SpringBatchTest``` 를 이용해 코드를 개선해보겠습니다.  

```java
@RunWith(SpringRunner.class)
@SpringBatchTest // (1)
@SpringBootTest(classes={BatchJpaTestConfiguration.class, TestBatchConfig.class}) // (2)
public class BatchIntegrationTestJobConfigurationNewTest {
    ...
}
```

(1) ```@SpringBatchTest```

* 앞에서 언급한대로 Spring Batch 4.1 버전에 새롭게 추가된 어노테이션
* 현재 테스트에선 ```JobLauncherTestUtils```를 지원 받기 위해 사용됩니다.

(2) ```TestBatchConfig.class```

* ```@SpringBatchTest``` 로 인해 불필요한 설정이 제거된 Config 클래스

새롭게 추가될 ```TestBatchConfig``` 클래스의 코드는 아래가 전부입니다.

```java
@Configuration
@EnableAutoConfiguration
@EnableBatchProcessing
public class TestBatchConfig {}
```

기존에 생성해주던 ```JobLauncherTestUtils``` 가 모두 ```@SpringBatchTest```를 통해 자동 Bean으로 등록되니 더이상 직접 생성해줄 필요가 없습니다.  
조금 더 간편해졌죠?  
  
![3](./images/3.png)

### 10-1-3. @SpringBootTest가 필수인가요?

아마 ```@SpringBatchTest```와 ```@ContextConfiguration``` 를 사용하면 굳이 ```@SpringBootTest```가 필요한가? 라는 의문이 드실수 있습니다.  
  
실제로 저도 그렇게 생각했고, 스프링 배치 공식 문서에서도 비슷하게 가이드 하고 있었습니다.  

![4](./images/4.png)

([End-To-End Testing of Batch Jobs](https://docs.spring.io/spring-batch/4.2.x/reference/html/testing.html#endToEndTesting))

헌데 JPA를 비롯해서 **자동 설정이 많이 필요한** 의존성들이 있는 프로젝트라면 ```@SpringBootTest```가 필요한 경우가 많습니다.  
  
저 같은 경우 스프링 배치 통합 테스트가 필요할때라면 그냥 마음편하게 ```@SpringBootTest```을 사용합니다.  
사용하지 않을 경우 아래와 같이 **전체 테스트 수행시 다양한 에러**가 발생합니다.  
(이외에도 **어떤 스프링 라이브러리들을 의존하고 있냐**에 따라 다양한 에러가 발생합니다.)  

```java
InstanceAlreadyExistsException: com.zaxxer.hikari:name=dataSource,type=HikariDataSource
```

아무래도 ```@SpringBootTest```가 해주던 많은 자동 설정들이 지원이 되지 않기 때문에 어쩔수 없는 일입니다.  
  
아래는 stackoverflow에 올라온 질문에 대해 **스프링 배치 팀의 개발자인** [beans](https://github.com/benas)가 답변을 남긴 것인데요.  
beans 역시 그냥 ```@SpringBootTest```를 사용하라고 합니다.

[spring-batch-end-to-end-test-configuration-not-working](https://stackoverflow.com/questions/55871880/spring-batch-end-to-end-test-configuration-not-working)

어떻게든 수동으로 환경을 만들어서 통합 테스트를 수행할 순 있겠지만, 그 비용이 너무 많이 들기 때문에 마음 편하게 ```@SpringBootTest```를 사용하시는걸 추천드립니다.

### 마무리

통합 테스트에 대해 알아보았습니다.  
분량이 많다 보니 **단위 테스트는 다음편**에서 다뤄볼 예정입니다.  
다음 편에서 다룰 단위 테스트는 **Reader로 실행되는 쿼리만 어떻게 검증할 것인가**, **JobParameter는 단위테스트에서 어떻게 주입할 수 있는가** 등등을 다뤄보겠습니다.
