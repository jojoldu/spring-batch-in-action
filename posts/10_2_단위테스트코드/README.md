# 10-2. Spring Batch 단위 테스트 코드

## 10-2. Spring Context 없는 단위 테스트

## 10-3. Spring Context 가 필요한 단위 테스트


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
