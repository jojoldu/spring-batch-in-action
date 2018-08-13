# 5. Spring Batch Scope & Job Parameter

이번 시간에는 Spring Batch의 Scope에 대해서 배워보겠습니다.  
여기서 말하는 Scope란 ```@StepScope```, ```@JobScope```를 얘기하는데요.  
무의식적으로 사용하는 이 어노테이션들이 실제로 어떤 일들을 하는지 알아보겠습니다.  
그리고 이 둘과 떨어질 수 없는 **Job Parameter**도 함께 배워보겠습니다.

## @StepScope & @JobScope 소개

Spring Batch는 ```@StepScope```와 ```@JobScope``` 라는 아주 특별한 Bean Scope를 지원합니다.  

> Bean Scope란 **Bean의 생명주기**를 얘기합니다.  
예를 들어 Bean Scope의 기본값인 singleton의 경우 해당 웹 어플리케이션에서 **유일하게 존재**합니다.  
이외에도 Request 요청당 생성되는 Request등 다양한 Scope가 있습니다.


## Bean Scope

@JobScope와 @StepScope가 ```proxyMode = ScopedProxyMode.TARGET_CLASS```를 사용하기 때문에 주의하실 점이 있습니다.  
그 부분은 이미 포스팅한적이 있어 링크로 대체합니다.  
꼭 보셔야합니다. 

* [@JobScope & @StepScope 사용시 주의 사항](http://jojoldu.tistory.com/132)
  

## Late Binding (늦은 할당)

Spring Batch의 

```java

@Slf4j
@RequiredArgsConstructor
@RestController
public class JobLauncherController {
  
    private final JobLauncher jobLauncher;
    private final Job job;
     
    @GetMapping("/launchjob")
    public String handle(@RequestParam("fileName") String fileName) throws Exception {
  
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                                    .addString("input.file.name", fileName)
                                    .addLong("time", System.currentTimeMillis())
                                    .toJobParameters();
            jobLauncher.run(job, jobParameters);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
  
        return "Done";
    }
}
```

## JobParameter vs 시스템 변수

JobParameter

```java
@Bean
@StepScope
public FlatFileItemReader<Partner> reader(
        @Value("#{jobParameters[pathToFile]}") String pathToFile){
    FlatFileItemReader<Partner> itemReader = new FlatFileItemReader<Partner>();
    itemReader.setLineMapper(lineMapper());
    itemReader.setResource(new ClassPathResource(pathToFile));
    return itemReader;
}
```

시스템 변수

> 여기에서 얘기하는 시스템 변수는 application.properties와 ```-D``` 옵션으로 실행하는 변수까지 포함합니다.

```java
@Bean
@ConfigurationProperties(prefix = "my.prefix")
protected class JobProperties {

    String pathToFile;

    ...getters/setters
}

@Autowired
private JobProperties jobProperties;

@Bean
public FlatFileItemReader<Partner> reader() {
    FlatFileItemReader<Partner> itemReader = new FlatFileItemReader<Partner>();
    itemReader.setLineMapper(lineMapper());
    String pathToFile = jobProperties.getPathToFile();
    itemReader.setResource(new ClassPathResource(pathToFile));
    return itemReader;
}
```

두 가지 방식에는 몇 가지 차이점이 있습니다.  
  
일단 첫번째로, 두번째 방식 (환경 변수)을 사용할 경우 **Spring Batch의 Job Parameter 관련 기능을 못쓰게** 됩니다.  
예를 들어, Spring Batch는 같은 JobParameter로 같은 Job을 두 번 실행하지 않습니다.  
하지만 환경 변수를 사용하게 될 경우 이 기능이 전혀 작동하지 않습니다.  
  
둘째, Command line이 아닌 다른 방법으로 작업을 시작하기가 어렵습니다.  
(또는 적어도 동일한 글로벌 상태를 제공하는 다른 방법을 구현해야합니다).  
동시에 여러 작업을 실행하려는 경우 또는 기능 테스트를 통해 작업을 테스트하려는 경우 문제가 발생할 수 있습니다.  
  
셋째, ```reader``` Bean의 범위가 서로 다릅니다.  
첫번째 예제는 Bean의 범위가 ```@StepScope``` 입니다.  
두번째 예제는 Bean의 범위가 ```singleton``` 입니다.
 
두번째의 경우 Bean의 인스턴스는 **서버에서 하나만 존재**합니다.  
첫번째의 경우 Bean을 요청하는 **모든 Step에서 새 인스턴스가 생성** 될 것입니다.  
(다르게 범위가 지정된 bean에 bean을 주입 할 수 있도록 단일 범위의 프록시가 있습니다.)

마지막으로, ```@StepScope``` 단계를 ExecutionContext거치지 않고도 단계에서 객체를 삽입 할 수 ChunkContext있으므로 코드와 테스트 작성이이 단순해집니다.

추가로. 첫 번째 코드에서, ```reader```는 ```@JobScope``` 만 있어도 JobParameter를 받을 수 있습니다.