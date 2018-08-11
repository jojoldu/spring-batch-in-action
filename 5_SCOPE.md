# StepScope & JobScope

## Spring Batch Late Binding (늦은 할당)

여기에서 값 null을 가진 메소드 리더를 호출합니다.
이제는 프록시 만 만들어지기 때문에 실제 리더 객체는 나중에 만들어지며 그 때 표현식이 pathToFile 값을 주입하는 데 사용되므로 괜찮습니다.

조금 이상하게 보일지라도 독자적인 구성 방법을 알고 싶다면이 메서드로 뛰어 들기 만하면됩니다.

## JobParameter vs Spring Properties

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

Spring Properties

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
첫번째의 경우 Bean을 요청하는 모든 Step에서 새 인스턴스가 생성 될 것입니다.  
(다르게 범위가 지정된 bean에 bean을 주입 할 수 있도록 단일 범위의 프록시가 있습니다.).

마지막으로, ```@StepScope``` 단계를 ExecutionContext거치지 않고도 단계에서 객체를 삽입 할 수 ChunkContext있으므로 코드와 테스트 작성이이 단순해집니다.

추가로. 첫 번째 코드에서, ```reader```는 ```@JobScope``` 만 있어도 JobParameter를 받을 수 있습니다.