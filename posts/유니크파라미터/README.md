# Spring Batch의 유니크 Job Parameter 활용하기

Spring Batch의 경우 일반적으로 **동일 Job Parameter로 실행시** 어떻게 처리할 것인지에 대해 여러가지 옵션을 제공합니다.

* 해당 파라미터로 최근 실패한 이력이 있다면 이어서 실행할 것인지
* 해당 파라미터로 최근 실패 혹은 성공한 이력이 있다면 실행하지 않을 것인지
* 해당 파라미터로 최근 실행한 이력이 있어도 무시하고 다시 실행할 것인지

등등이 있습니다.  
  
대부분의 경우 동일 Job Parameter 실행을 막곤 하는데요.  
(중복 데이터가 쌓일 수가 있기 때문에)  
  
일부 배치에서는 **동일 Job Parameter로 계속 실행이 될 수 있길** 원하기도 합니다.  
예를 들어 특정 데이터에 대한 검증 로직 혹은 데이터 갱신 배치 등이 이에 해당 됩니다.  
  
이번 글에서는 바로 이렇게 **동일 Job Parameter로 계속 실행이 되는 방법**에 대한 방법과 이를 활용하는 패턴들을 알아보겠습니다.  

## 1. RunIdIncrementer

Spring Batch에서는 **동일 파라미터인데 다시 실행하고 싶을때** 사용하라는 의미로 ```RunIdIncrementer```를 제공합니다.  
  
실제로 사용 방법은 아래처럼 Job 의 ```incrementer``` 옵션에 추가하여 사용합니다.

```java
public Job job() {
    return jobBuilderFactory.get(JOB_NAME)
            .start(step(null))
            .incrementer(new RunIdIncrementer())
            .build();
}
```

Spring Boot 1.5.x (Spring Batch 3.x) 까지는 위와 같이 해결이 가능했습니다만, Spring Boot 2 (Spring Batch 4) 로 버전업이 되면서 큰 버그가 하나 생겼습니다.  

> 사실 버그라기 보다는 의도한 동작이였지만, 실제로 그렇게 원하는 사람이 없었다가 맞는것 같습니다.

### 1-1. Spring Boot 2.0.x에서 발생하는 버그

Spring Boot 2.0.x (Spring Batch 4.0.x)

Spring Boot 2.0.x 버그 해결책

### 1-2. Spring Boot 2.1.0 이상부터는?

위의 버그는 **Spring Boot 2.1.0 (Spring Batch 4.1.0)** 에서 해결된 문제 입니다.  

관련 PR

* [Spring Boot 수정](https://github.com/spring-projects/spring-boot/pull/14933)
* [Spring Batch 수정](https://github.com/spring-projects/spring-batch/pull/660)

그래서 최신의 Spring Boot를 사용하시는 분들은 기존처럼 ```RunIdIncrementer```를 사용하시면 됩니다.


## 테스트 코드


## 개발 & 운영 다르게 활용해야할 때

예를 들어 실제 운영에서는 **같은 파라미터로 중복 실행을 막아야**하지만 개발 환경에서는 언제든 잦은 QA와 테스트를 위해 언제든 재실행이 되어야 한다면 어떻게 해야할까요?  

