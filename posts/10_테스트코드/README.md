# 10. Spring Batch 테스트 코드

배치 애플리케이션이 웹 애플리케이션 보다 어려운 점을 꼽으로하고 하면 QA를 많이들 얘기합니다.  
  
일반적으로 웹 애플리케이션의 경우 전문 테스터 분들 혹은 QA 분들이 전체 기능을 검증을 해주시는 반면, 
배치 애플리케이션의 경우 DB의 최종상태라던가 메세징큐의 발행내역 등 **개발자들이 직접 확인해주는 것** 외에는 검증 방법이 딱히 없습니다.  
(별도의 어드민을 제공하는것도 포함입니다.)  
  
더군다나 개발자가 로컬 환경에서 배치 애플리케이션을 수행하는 것도 많은 수작업이 필요합니다.  
수정/삭제 등의 배치 애플리케이션이라면 **한번 수행할때마다 로컬 DB의 데이터를 원복**하고 다시 수행하는 작업을 반복해야 합니다.  
  
그래서 테스트 코드의 필요성이 많이 강조됩니다.  
  
다행이라면 배치 애플리케이션은 웹 애플리케이션 보다 테스트 코드 작성이 좀 더 수월하고, 한번 작성하게 되면 그 효과가 좋습니다.  
  
아무래도 UI 검증이 필요한 웹 애플리케이션에 비해 Java 코드에 대한 검증만 필요한 배치 애플리케이션의 테스트 코드가 좀 더 수월합니다.  
  
이 챕터에서는 JUnit & Mockito 프레임워크와  ```@SpringBootTest``` 와 H2를 이용한 테스트 환경 등에 대해서는 별도로 설명하지 않습니다.  
  
해당 프레임워크에 대한 기본적인 사용법은 이미 충분히 많은 자료들이 있으니 참고해서 봐주시면 됩니다.  
  
## 10-1. 통합 테스트

개인적인 생각으로 스프링 배치 테스트 코드는 ItemReader의 단위 테스트를 작성하는 것 보다 **통합 테스트 코드 작성이 좀 더 쉽다**고 생각합니다.  
  
스프링 배치 모듈들 사이에서 ItemReader만 뽑아내 **쿼리를 테스트 해볼 수 있는 환경**을 Setup 하려면 여러가지 장치가 필요합니다.  
  
그래서 먼저 해볼것은 스프링 배치의 통합 테스트 입니다.  
  
### 10-1-1. 4.0.x 이하 버전

4.1 보다 아래 버전의 스프링 배치를 사용하신다면 다음과 같이 통합 테스트를 사용할 수 있습니다.

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

        JobParameters jobParameters = new JobParametersBuilder() // (3)
                .addString("orderDate", orderDate.format(FORMATTER))
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters); // (4)

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


(1) ```@SpringBootTest(classes={...})```: 통합 테스트 실행시 사용할 Java 설정들을 선택합니다.

* ```BatchJpaTestConfiguration``` : 테스트할 Batch Job
* ```TestBatchLegacyConfig``` : 배치 테스트 환경
  * 아래에서 좀 더 자세히 설명드리겠습니다.

(2) ```JobLauncherTestUtils```: Batch Job을 테스트 환경에서 실행할 Utils 클래스입니다.

* CLI 등으로 실행하는 Job을 테스트 환경에서 실행할 수 있도록 지원합니다.

TestBatchLegacyConfig 의 코드는 아래와 같이 구성합니다.

```java
@Configuration
@EnableAutoConfiguration
@EnableBatchProcessing
public class TestBatchLegacyConfig {

    @Bean
    public JobLauncherTestUtils jobLauncherTestUtils() {
        return new JobLauncherTestUtils();
    }
}
```

  
이전에는 ```@ConditionalOnProperty```와 ```@TestPropertySource``` 를 사용하여 **특정 Batch Job**만 설정을 불러와 배치를 테스트 했습니다.  
  
다만 이 방식에도 여러 단점들이 있는데요.  

1. 흔히 말하는 행사 코드가 많이 필요합니다.

* Batch Job에는 ```@ConditionalOnProperty(name = "job.name", havingValue = job명)```, 테스트 코드에서는 ```@TestPropertySource(properties = {"job.name=" + job명})``` 등의 코드가 항상 필요합니다.

2. 전체 테스트 수행시 매번 Spring Context가 새로 실행됩니다.

* 앞에서 얘기한 행사코드인 ```@TestPropertySource```로 인해 전체 테스트 수행시에는 매번 Spring의 Context가 다시 생성됩니다.
* 단일 테스트 속도는 빠르나 전체 테스트에선 아무래도 느릴수 밖에 없습니다.

1번의 문제인 **많은 행사코드** 문제는 그나마 해결이 가능한데요.  
  
최근 스프링 배치 공식 문서에서는 ```@ContextConfiguration``` 를 사용하여 행사 코드를 줄였습니다.  
  
이 어노테이션은 ```ApplicationContext``` 에서 관리할 Bean과 Configuration 들을 지정하는 곳 입니다.  
  
해당 어노테이션을 사용하면 **특정 Batch Job**의 설정들만 가져와서 수행할 수 있습니다.

> ```@SpringBootTest(classes={XXX.class, ZZZ.class})``` 를 써보신 분들은 동일하게 작동된다고 생각하시면 됩니다.

이 방식을 선택하나 위에서 언급한 ```ConditionalOnProperty```을 선택하나 **전체 테스트 수행시 Spring Context가 재실행되는 것은 여전**합니다.  
다만, 두번째 방식인 ```@ContextConfiguration```를 선택한다면 테스트 코드를 어떻게 작성하냐에 따라서 하나의 Spring Context를 사용할수도/각자의 Spring Context를 사용할수도 있습니다.  
하나의 Spring Context를 사용하는 방법에 대해서는 **별도의 테스트 포스팅**으로 소개드리겠습니다.  
일단 이 글에서는 처음 스프링 배치 테스트를 작성하는 분들의 기본을 잡는게 목적이기 때문입니다.  


### 4.1.x 이상 버전

스프링 배치 4.1에서 새로운 어노테이션이 추가되었습니다.  
바로 ```@SpringBatchTest```입니다.  


해당 어노테이션은 ApplicationContext 에 자동으로 테스트하기위한 많은 유틸리티를 제공합니다. 

> Tip) 다들 아시겠지만, ApplicationContext 은 Spring의 Bean 컨테이너입니다.  
> 여기에 Spring의 Bean들이 모두 담겨져있고, 가져와서 사용할 수 있다고 보시면 됩니다.

특히 4 개의 빈을 추가합니다

* JobLauncherTestUtils
* JobRepositoryTestUtils
* StepScopeTestExecutionListner
* JobScopeTextExecutionListner





## 단위 테스트

```java
public class StepScopeTestExecutionListener implements TestExecutionListener {
    ...
    protected StepExecution getStepExecution(TestContext testContext) {
		Object target;

		try {
			Method method = TestContext.class.getMethod(GET_TEST_INSTANCE_METHOD);
			target = ReflectionUtils.invokeMethod(method, testContext);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("No such method " + GET_TEST_INSTANCE_METHOD + " on provided TestContext", e);
		}

		ExtractorMethodCallback method = new ExtractorMethodCallback(StepExecution.class, "getStepExecution");
		ReflectionUtils.doWithMethods(target.getClass(), method);
		if (method.getName() != null) {
			HippyMethodInvoker invoker = new HippyMethodInvoker();
			invoker.setTargetObject(target);
			invoker.setTargetMethod(method.getName());
			try {
				invoker.prepare();
				return (StepExecution) invoker.invoke();
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Could not create step execution from method: " + method.getName(),
						e);
			}
		}

		return MetaDataInstanceFactory.createStepExecution();
	}
    ...
}
```

### 4.0.x 이하 버전

```java
@ContextConfiguration
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class,
    StepScopeTestExecutionListener.class })
@RunWith(SpringRunner.class)
public class StepScopeTestExecutionListenerIntegrationTests {

    // This component is defined step-scoped, so it cannot be injected unless
    // a step is active...
    @Autowired
    private ItemReader<String> reader;

    public StepExecution getStepExecution() {
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        execution.getExecutionContext().putString("input.data", "foo,bar,spam");
        return execution;
    }

    @Test
    public void testReader() {
        // The reader is initialized and bound to the input data
        assertNotNull(reader.read());
    }

}
```

### 4.1.x 이상 버전

```java
@SpringBatchTest
@RunWith(SpringRunner.class)
@ContextConfiguration
public class StepScopeTestExecutionListenerIntegrationTests {

    // This component is defined step-scoped, so it cannot be injected unless
    // a step is active...
    @Autowired
    private ItemReader<String> reader;

    public StepExecution getStepExecution() {
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        execution.getExecutionContext().putString("input.data", "foo,bar,spam");
        return execution;
    }

    @Test
    public void testReader() {
        // The reader is initialized and bound to the input data
        assertNotNull(reader.read());
    }

}
```


## @SpringBootTest ?

```java
InstanceAlreadyExistsException: com.zaxxer.hikari:name=dataSource,type=HikariDataSource
```

[spring-batch-end-to-end-test-configuration-not-working](https://stackoverflow.com/questions/55871880/spring-batch-end-to-end-test-configuration-not-working)

## 중복 회피

```java
JobInstanceAlreadyCompleteException: A job instance already exists and is complete for parameters={orderDate=2019-10-06}.  If you want to run this job again, change the parameters.
```