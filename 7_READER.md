# 7. ItemReader

앞의 과정들을 통해 Spring Batch가 Chunk 지향 처리를 하고 있으며 이를 Job과 Step으로 구성되어 있음을 배웠습니다.  
Step은 Tasklet 단위로 처리되고, Tasklet 중에서 **ChunkOrientedTasklet**을 통해 Chunk 처리가 가능하고 이를 구성하는 3요소로 ItemReader, ItemWriter, ItemProcessor가 있음을 배웠습니다.  

> 즉, ItemReader & ItemWriter & ItemProcessor의 묶음 역시 Tasklet이란 이야기입니다.  
이들의 묶음을 ChunkOrientedTasklet에서 관리하기 때문이죠.

이번 시간부터 이 3요소를 차근차근 배워보겠습니다.

## 7-1. ItemReader 소개

Spring Batch의 Chunk Tasklet은 아래와 같은 과정을 통해 진행됩니다.

![chunk](./images/7/chunk.png)

이번엔 이 과정의 가장 첫번째인 Reader에 대해 알아볼텐데요.  
그림에서 보시는것처럼 Spring Batch의 ItemReader는 **데이터를 읽어들입니다**.   
그게 꼭 DB의 데이터만을 얘기하진 않습니다.  
  
File, XML, JSON 등 다른 데이터 소스를 배치 처리의 입력으로 사용할 수 있습니다.  
또한 JMS (Java Message Service)와 같은 다른 유형의 데이터 소스도 지원합니다.  

이외에도 **Spring Batch에서 지원하지 않는 Reader가 필요할 경우 직접 해당 Reader를 만들수도 있습니다**.  
Spring Batch는 이를 위해 Custom Reader 구현체를 만들기 쉽게 제공하고 있습니다.  
  
정리하면 Spring Batch의 Reader에서 읽어올 수 있는 데이터 유형은 다음과 같습니다.

* 입력 데이터에서 읽어오기
* 파일에서 읽어오기
* Database에서 읽어오기
* Java Message Service등 다른 소스에서 읽어오기
* 본인만의 커스텀한 Reader로 읽어오기

이 ItemReader의 구현체들이 어떻게 되어있는지 살펴보겠습니다.  
가장 대표적인 구현체인 JdbcPagingItemReader가 있는데요.  
해당 클래스의 계층 구조를 살펴보면 아래와 같습니다.

![readerlayer](./images/7/readerlayer.png)

ItemReader외에 **ItemStream 인터페이스도 같이 구현**하고 있습니다.  
  
먼저 ItemReader를 살펴보면 ```read()``` 만 가지고 있습니다.

![itemreader](./images/7/itemreader.png)

* ```read()```의 경우 데이터를 읽어오는 메소드입니다.

Reader가 하는 본연의 임무를 담당하는 인터페이스임을 알 수 있는데요.  
  
그럼 ItemStream 인터페이스는 무슨 역할을 할까요?  
ItemStream 인터페이스는 **주기적으로 상태를 저장하고 오류가 발생하면 해당 상태에서 복원**하기 위한 마커 인터페이스입니다.
즉, 배치 프로세스의 실행 컨텍스트와 연계해서 **ItemReader의 상태를 저장하고 실패한 곳에서 다시 실행할 수 있게 해주는 역할**을 합니다.  

![itemstream](./images/7/itemstream.png)

ItemStream의 3개 메소드는 다음과 같은 역할을 합니다.

* ```open()```, ```close()```는 스트림을 열고 닫습니다.  
* ```update()```를 사용하면 Batch 처리의 상태를 업데이트 할 수 있습니다.  

개발자는 ItemReader와 ItemStream 인터페이스를 직접 구현해서 원하는 형태의 ItemReader를 만들 수 있습니다.  
다만 Spring Batch에서 대부분의 데이터 형태는 ItemReader로 이미 제공하고 있기 때문에 커스텀한 ItemReader를 구현할 일은 많이 없을 것입니다.  

> 단, 본인의 조회 프레임워크가 Querydsl, Jooq라면 직접 구현해야할 수도 있습니다.  
웬만하면 JdbcItemReader로 해결되지만, **JPA의 영속성 컨텍스트를 사용하기 위해서**라면 조회 프레임워크의 Reader 구현체를 직접 구현하셔야 합니다.

자 이제 ItemReader의 구현체를 알아볼텐데요.  
여기에서는 Database의 구현체들만 다뤄보겠습니다.  
이외에 다른 Reader들 (File, XML, Json) 등은 실제 업무에서 많이 사용되지 않기 때문에 필요하시다면 [공식 문서](https://docs.spring.io/spring-batch/4.0.x/reference/html/readersAndWriters.html#flatFiles)를 통해서 사용하시는걸 권장드립니다.


## 7-2. Database

* [데이터 베이스](https://docs.spring.io/spring-batch/4.0.x/reference/html/readersAndWriters.html#database)
  
## 7-3. JPA

* JpaItemReader
* JpaPagingItemReader
    * IbatisReader는 현재 (Spring Batch 4.0) 삭제됨
* 같은 테이블을 조회 & 수정해야 한다면? ([참고](https://stackoverflow.com/questions/26509971/spring-batch-jpapagingitemreader-why-some-rows-are-not-read))

* 저장 프로시져 Reader: StoredProcedureItemReader

## CursorItemReader Interface

* Cursor?
* 페이징과의 차이점
* JpaCursorItemReader


## 주의 사항

* 절대 절대 JpaRepository로 ItemReader 커스텀하게 쓰지말것
    * 
* 