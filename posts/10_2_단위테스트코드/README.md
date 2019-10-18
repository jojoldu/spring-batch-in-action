# 10.1. Spring Batch 단위 테스트 코드

웹 애플리케이션을 개발하다보면 통합 테스트 보다, **단위 테스트가 훨씬 더 많이 작성**됩니다.  
단위 테스트로 많은 코드를 검증 후, 통합 테스트 코드를 통해 각 단위가 합쳐졌을때 잘 작동 되는지 검증하곤 하는데요.  
  
스프링 배치를 이용한 **배치 애플리케이션**에서는 많은 분들이 **통합 테스트만** 작성할때가 많습니다.  
[전편](https://jojoldu.tistory.com/455)에서 말씀드린것처럼 스프링 배치의 단위 테스트 작성이 통합 테스트 보다 복잡하기 때문입니다.  
  
그래서 이번 챕터에서는

* Reader의 쿼리가 잘 작동되었는지는 어떻게 확인하지?
* StepScope를 통한 JobParameter가 잘 할당 된다는 것은 어떻게 확인하지?
* Reader & Writer 없이 어떻게 Processor를 테스트하지?

등등 여러 부분을 잘개 쪼개서 테스트할 수 있는 방법들을 소개드립니다.

## 10-2. Reader 테스트

Reader의 단위 테스트는 다음을 보고 싶을때가 많습니다.

* 내가 작성한 Reader의 Query가 잘 작동하는지

그래서 Reader의 단위 테스트 방법은 이 부분에 초점을 맞춰서 진행할 예정입니다.  
다만, 

> 참고로 아래 모든 단위 테스트들에는 H2 의존성이 필수입니다.
> ```compile('com.h2database:h2')```



### StepScope 가 필요 없는 단위 테스트

> 이 방식은 **JPA에서는 사용하기가 어렵습니다**.  
> Spring Data Jpa를 통해 생성되는 여러 환경들을 본인이 직접 다 구성해야되기 때문입니다.  
> 그래서 JPA를 쓰지 않고, **JdbcTemplate**로 배치 환경을 구성하시는 분들이 참고해보시면 좋을것 같습니다.


```java
public class BatchNoSpringContextUnitTest2 {

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private ConfigurableApplicationContext context;
    private LocalDate orderDate;
    private BatchJdbcTestConfiguration job;

    @Before
    public void setUp() {
        this.context = new AnnotationConfigApplicationContext(TestDataSourceConfiguration.class); // (1)
        this.dataSource = (DataSource) context.getBean("dataSource"); // (2)
        this.jdbcTemplate = new JdbcTemplate(this.dataSource); // (3)
        this.orderDate = LocalDate.of(2019, 10, 6);
        this.job = new BatchJdbcTestConfiguration(null, null, dataSource); // (4)
        this.job.setChunkSize(10); // (5)
    }

    @After // context 초기화
    public void tearDown() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void 기간내_Sales가_집계되어_SalesSum이된다() throws Exception {
        // given
        long amount1 = 1000;
        long amount2 = 100;
        long amount3 = 10;
        jdbcTemplate.update("insert into sales (order_date, amount, order_no) values (?, ?, ?)", orderDate, amount1, "1");
        jdbcTemplate.update("insert into sales (order_date, amount, order_no) values (?, ?, ?)", orderDate, amount2, "2");
        jdbcTemplate.update("insert into sales (order_date, amount, order_no) values (?, ?, ?)", orderDate, amount3, "3");

        JdbcPagingItemReader<SalesSum> reader = job.batchJdbcUnitTestJobReader(orderDate.format(FORMATTER));
        reader.afterPropertiesSet(); // Query 생성
        reader.open(new ExecutionContext()); // Step Execution Context 할당

        // when
        SalesSum readResult = reader.read();

        // then
        assertThat(readResult.getAmountSum()).isEqualTo(amount1 + amount2 + amount3); // 첫번째 결과 조회
        assertThat(reader.read()).isNull(); // 더이상 읽을게 없어 null
    }

    @Configuration
    public static class TestDataSourceConfiguration {

        private static final String CREATE_SQL =
                        "create table IF NOT EXISTS `sales` (id bigint not null auto_increment, amount bigint not null, order_date date, order_no varchar(255), primary key (id)) engine=InnoDB;";

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseFactory databaseFactory = new EmbeddedDatabaseFactory();
            databaseFactory.setDatabaseType(H2);
            return databaseFactory.getDatabase();
        }

        @Bean
        public DataSourceInitializer initializer(DataSource dataSource) {
            DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
            dataSourceInitializer.setDataSource(dataSource);

            Resource create = new ByteArrayResource(CREATE_SQL.getBytes());
            dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(create));

            return dataSourceInitializer;
        }
    }
}
```

코드가 너무 길어서 부분적으로 나눠서 설명 드리겠습니다.  

#### setUp


```java
java.lang.IllegalArgumentException: pageSize must be greater than zero
```

그래서 저 같은 경우 pageSize 값에 사용되는 chunkSize를 **setter 인잭션**을 사용하도록 했습니다.

```java
    private int chunkSize;

    @Value("${chunkSize:1000}") // setter 인잭션
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
    ...

    @Bean
    @StepScope
    public JdbcPagingItemReader<SalesSum> batchJdbcUnitTestJobReader(
            @Value("#{jobParameters[orderDate]}") String orderDate) throws Exception {
        ...

        return ...
                .pageSize(chunkSize)
                .fetchSize(chunkSize)
                ...;
    }
```

#### 테스트 메소드

#### 테스트 Config



![1](./images/1.png)

### StepScope 를 이용한 단위 테스트


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
    StepScopeTestExecutionListener.class }) // 
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
