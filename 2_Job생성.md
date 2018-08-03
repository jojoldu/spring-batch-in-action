# 2. Batch Job 실행해보기

이번 시간에는 간단한 Spring Batch Job을 생성 & 실행하면서 전반적인 내용을 공부해보겠습니다.

> 작업한 모든 코드는 [Github](https://github.com/jojoldu/spring-batch-in-action)에 있으니 참고하시면 됩니다.  

## 2-1. Spring Batch 프로젝트 생성하기

기본적인 프로젝트 개발 환경은 다음과 같습니다.

* IntelliJ IDEA 2018.2
* Spring Boot 2.0.4
* Java8
* Gradle

> lombok 기능을 많이 사용합니다.  
lombok 플러그인을 본인의 IDE에 맞게 설치하시면 좋습니다 :)  
[Intellij IDEA](http://blog.woniper.net/229), [Eclipse](http://countryxide.tistory.com/16)


이를 기반으로 프로젝트 생성을 시작하겠습니다.  

> 저는 IntelliJ Ultimate (유료) 버전에서 실행하지만, Eclipse도 화면 구성이 크게 다르진 않을것 같습니다.

먼저 Spring Boot 프로젝트를 나타내는 Spring Initializr (Spring Boot)를 선택합니다.

![project1](./images/2/project1.png)

본인만의 Group, Artifact 를 선택하시고 Gradle 프로젝트를 선택합니다.  
이후 Spring 의존성을 선택하는 화면에선 아래와 같이 선택합니다.

![project1](./images/2/project2.png)

> 만약 본인의 프로젝트가 **JPA만 쓰고 있다면 JDBC를 선택하지 않으셔도** 됩니다.  
혹은 **JPA를 쓰지 않는다면 JPA를 선택하지 않으셔도** 됩니다.

build.gradle은 아래와 같은 형태가 됩니다.

```groovy
buildscript {
    ext {
        springBootVersion = '2.0.4.RELEASE'
    }
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
    }
}

apply plugin: 'java'
apply plugin: 'eclipse'
apply plugin: 'org.springframework.boot'
apply plugin: 'io.spring.dependency-management'

group = 'com.jojoldu.spring'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
    mavenCentral()
}


dependencies {
    compile('org.springframework.boot:spring-boot-starter-batch')
    compile('org.springframework.boot:spring-boot-starter-data-jpa')
    compile('org.springframework.boot:spring-boot-starter-jdbc')
    runtime('com.h2database:h2')
    runtime('mysql:mysql-connector-java')
    compileOnly('org.projectlombok:lombok')
    testCompile('org.springframework.boot:spring-boot-starter-test')
    testCompile('org.springframework.batch:spring-batch-test')
}

```

> group (com.jojoldu.spring) 은 본인만의 group으로 사용하시면 됩니다.

그리고 패키지 안에 있는 Application.java를 열어보시면 아래처럼 ```Main``` 메소드가 있음을 알 수 있습니다.

![3](./images/2/project3.png)

전형적인 Spring Boot 코드이죠?  
그럼 이제 간단한 Spring Batch Job을 생성해보겠습니다.

## 2-2. Simple Job 생성하기

Batch Job을 만들기전에, ```BatchApplication.java```에 다음과 같이 **Spring Batch 기능 활성화** 어노테이션 (```@EnableBatchProcessing```)을 추가합니다.

![simplejob1](./images/2/simplejob1.png)

이 어노테이션을 선언하면, Spring Batch의 여러 기능들을 사용할 수 있게 됩니다.  
선언하지 않으시면 Spring Batch 기능을 사용할수 없기 때문에 **필수로 선언**하셔야만 합니다.  
  
설정이 끝나셨으면 패키지 아래에 ```job``` 패키지를 생성하고, ```SimpleJobConfiguration.java``` 파일을 생성합니다.

![simpleJob2](./images/2/simplejob2.png)

생성한 Java 파일 안에 ```simpleJob``` 이란 이름의 간단한 Spring Batch 코드를 작성해봅니다.

```java

@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class SimpleJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory; // 생성자 DI 받음
    private final StepBuilderFactory stepBuilderFactory; // 생성자 DI 받음

    @Bean
    public Job simpleJob() {
        return jobBuilderFactory.get("simpleJob")
                .start(simpleStep1())
                .build();
    }

    @Bean
    public Step simpleStep1() {
        return stepBuilderFactory.get("simpleStep1")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is Step1");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

* ```@Configuration```
    * Spring Batch의 모든 Job은 ```@Configuration```으로 등록해서 사용합니다.
* ```jobBuilderFactory.get("simpleJob")```
    * ```simpleJob``` 이란 이름의 Batch Job을 생성합니다.
    * job의 이름은 별도로 지정하지 않고, 이렇게 Builder를 통해 지정합니다.
* ```stepBuilderFactory.get("simpleStep1")```
    * ```simpleStep1``` 이란 이름의 Batch Step을 생성합니다.
    * ```jobBuilderFactory.get("simpleJob")```와 마찬가지로 Builder를 통해 이름을 지정합니다.
* ```.tasklet((contribution, chunkContext))```
    * Step 안에서 수행될 기능들을 명시합니다.
    * Tasklet은 Step안에서 단일로 수행될 커스텀한 기능들을 작성할때 사용하는 단위입니다.
    * 여기서는 Batch가 수행되면 ```log.info(">>>>> This is Step1")``` 가 출력되도록 합니다.
 
Batch Job을 생성하는 simpleJob 코드를 보시면 simpleStep1을 품고 있음을 알 수 있습니다.  
Spring Batch에서 **Job은 하나의 배치 작업 단위**를 얘기하는데요.  
Job 안에는 아래처럼 여러 Step이 존재하고, Step 안에 Tasklet 혹은 Reader & Processor & Writer 묶음이 존재합니다.

![jobstep](./images/2/jobstep.png)

Job안에 여러 Step이 있다는건 쉽게 이해되지만, Step이 품고 있는 단위가 애매하게 보이실 수 있습니다.  
  
Tasklet 하나와 Reader & Processor & Writer 한 묶음이 같은 레벨입니다.  
그래서 **Reader & Processor가 끝나고 Tasklet으로 마무리 짓는 등으로 만들순 없다**는걸 꼭 명심해주셔야 합니다.  

> Tasklet은 어찌보면 Spring MVC의 ```@Component```, ```@Bean```과 비슷한 역할이라고 보셔도 될 것 같습니다.  
명확한 역할은 없지만, 개발자가 지정한 커스텀한 기능을 위한 단위로 봐도 무방할것 같습니다.

자 그럼 한번 이 간단한 Spring Batch 어플리케이션을 실행해보겠습니다.  
처음 만들어졌던 ```BatchApplication.java```의 ```main``` 메소드를 실행하면 Batch가 실행됩니다.

![simpleJob5](./images/2/simpleJob5.png)

실행해보시면 아래처럼 ```log.info(">>>>> This is Step1")```가 잘 수행되어 로그가 찍힌것을 알 수 있습니다.

![simpleJob6](./images/2/simpleJob6.png)

처음으로 Spring Batch 프로그램을 작성해보았습니다!  
아주 간단한 실습을 진행해봤는데요.  
이제 조금 이론적인 내용을 한번 보겠습니다.

## 2-3. Spring Batch 메타 테이블 엿보기

이전 과정에서 굉장히 간단하게 Spring Batch가 수행되었습니다.  
**Spring Batch는 어플리케이션 코드만 작성하면 되는구나**! 라고 생각하실수 있으실텐데요.  
실제로는 그렇지 않습니다.  
Spring Batch에선 메타 데이터를 관리하는 테이블이 필요합니다.

> 메타 데이터란, **데이터를 설명하는 데이터**라고 보시면 됩니다.  
위키피디아보다 나무위키가 더 설명이 잘되어있어 [나무위키 링크](https://namu.wiki/w/%EB%A9%94%ED%83%80%EB%8D%B0%EC%9D%B4%ED%84%B0)를 첨부합니다.

Spring Batch의 메타 데이터는 다음과 같은 내용들을 담고 있습니다.

* 이전에 실행한 Job이 어떤 것들이 있는지 
* 최근 실패한 Batch Parameter가 어떤것들이 있고, 성공한 Job은 어떤것들이 있는지
* 다시 실행한다면 어디서 부터 시작하면 될지 
* 어떤 Job에 어떤 Step들이 있었고, Step들 중 성공한 Step과 실패한 Step들은 어떤것들이 있는지

등등 Batch 어플리케이션을 운영하기 위한 메타데이터가 여러 테이블에 나눠져 있습니다.  
메타 테이블 구조는 아래와 같은데요.

![domain1](./images/2/domain1.png)

( 출처: [metaDataSchema](https://docs.spring.io/spring-batch/3.0.x/reference/html/metaDataSchema.html) )  
  
이 테이블들은 이미 Spring Batch에 해당 스키마가 존재하고 있고, 이를 그대로 복사해서 ```create table``` 하면 됩니다.  
  
본인의 IDE에서 파일 검색으로 ```schema-```를 해보시면 Spring Batch에서 지원하는 여러 스키마 파일을 볼 수 있습니다.

![schema](./images/2/schema.png)

H2의 경우 해당 스키마가 자동으로 생성되지만, **그 외 나머지 MySQL, MariaDB등과 같은 DB에서는 직접 생성해야만** 합니다.  

본인의 PC에 MySQL을 설치하시고 Spring Batch가 MySQL을 사용하도록 설정을 추가해보겠습니다.   
  
프로젝트의 ```src/main/resources/application.yml``` 에 아래처럼 Datasource 설정을 추가하겠습니다.  


```yaml
spring:
  profiles:
    active: local

---
spring:
  profiles: local
  datasource:
    hikari:
      jdbc-url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
      username: sa
      password:
      driver-class-name: org.h2.Driver
---
spring:
  profiles: mysql
  datasource:
    hikari:
      jdbc-url: jdbc:mysql://localhost:3306/spring_batch
      username: jojoldu
      password: jojoldu1
      driver-class-name: com.mysql.jdbc.Driver
```

profile을 local로 두면 H2 DB를 사용하는 것이며, mysql로 두면 MySQL을 사용하겠습니다.




