# 7. ItemReader

앞의 과정들을 통해 Spring Batch가 Chunk 지향 처리를 하고 있으며 이를 Job과 Step으로 구성되어 있음을 배웠습니다.  
Step은 Tasklet 단위로 처리되고, Tasklet 중에서 **ChunkOrientedTasklet**을 통해 Chunk를 처리하며 이를 구성하는 3 요소로 ItemReader, ItemWriter, ItemProcessor가 있음을 배웠습니다.  

> 즉, ItemReader & ItemWriter & ItemProcessor의 묶음 역시 Tasklet이란 이야기입니다.  
이들의 묶음을 ChunkOrientedTasklet에서 관리하기 때문이죠.

이번 시간부터 이 3 요소를 차근차근 배워보겠습니다.

## 7-1. ItemReader 소개

Spring Batch의 Chunk Tasklet은 아래와 같은 과정을 통해 진행됩니다.

![chunk](./images/7/chunk.png)

이번엔 이 과정의 가장 첫번째인 Reader에 대해 알아보겠습니다.  
그림에서 보시는 것처럼 Spring Batch의 ItemReader는 **데이터를 읽어들입니다**.  
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
가장 대표적인 구현체인 JdbcPagingItemReader가 있습니다.  
해당 클래스의 계층 구조를 살펴보면 아래와 같습니다.

![readerlayer](./images/7/readerlayer.png)

ItemReader외에 **ItemStream 인터페이스도 같이 구현**하고 있습니다.  
  
먼저 ItemReader를 살펴보면 ```read()``` 만 가지고 있습니다.

![itemreader](./images/7/itemreader.png)

* ```read()```의 경우 데이터를 읽어오는 메소드입니다.

Reader가 하는 본연의 임무를 담당하는 인터페이스임을 알 수 있습니다.  
  
그럼 ItemStream 인터페이스는 무슨 역할을 할까요?  
ItemStream 인터페이스는 **주기적으로 상태를 저장하고 오류가 발생하면 해당 상태에서 복원**하기 위한 마커 인터페이스입니다.
즉, 배치 프로세스의 실행 컨텍스트와 연계해서 **ItemReader의 상태를 저장하고 실패한 곳에서 다시 실행할 수 있게 해주는 역할**을 합니다.  

![itemstream](./images/7/itemstream.png)

ItemStream의 3개 메소드는 다음과 같은 역할을 합니다.

* ```open()```, ```close()```는 스트림을 열고 닫습니다.  
* ```update()```를 사용하면 Batch 처리의 상태를 업데이트 할 수 있습니다.  

개발자는 **ItemReader와 ItemStream 인터페이스를 직접 구현해서 원하는 형태의 ItemReader**를 만들 수 있습니다.  
다만 Spring Batch에서 대부분의 데이터 형태는 ItemReader로 이미 제공하고 있기 때문에 커스텀한 ItemReader를 구현할 일은 많이 없을 것입니다.  

> 단, 본인의 조회 프레임워크가 Querydsl, Jooq라면 직접 구현해야할 수도 있습니다.  
웬만하면 JdbcItemReader로 해결되지만, **JPA의 영속성 컨텍스트가 지원이 안되서** HibernateItemReader를 이용하여 Reader 구현체를 직접 구현하셔야 합니다.

자 이제 ItemReader의 구현체를 알아볼텐데요.  
여기에서는 **Database의 구현체들만 다뤄보겠습니다**.  
이외에 다른 Reader들 (File, XML, Json) 등은 실제 업무에서 많이 사용되지 않기 때문에 필요하시다면 [공식 문서](https://docs.spring.io/spring-batch/4.0.x/reference/html/readersAndWriters.html#flatFiles)를 통해서 사용하시는걸 권장드립니다.

## 7-2. Database Reader

데이터베이스에 대한 배치 처리는 다른 어플리케이션과 달리 큰 문제점이 있습니다.  
만약 100만 row를 반환하는 쿼리를 수행해야 한다면 일반적인 어플리케이션 구조에서는 100만 row가 다 모일때까지 메모리에 계속해서 보관해야만 합니다.  
Spring Batch는 이 문제를 해결하기 위해 2가지 해결책을 제공합니다.

* Cursor 기반 ItemReader 구현체
    * JdbcCursorItemReader
    * HibernateCursorItemReader
    * StoredProcedureItemReader
* Paging 기반 ItemReader 구현체
    * JdbcPagingItemReader
    * JpaPagingItemReader
  
> IbatisReader는 삭제되었습니다.  
혹시나 필요하신 분들은 JdbcReader류를 사용하시길 권장드립니다.

하나씩 소개 드리겠습니다.

## 7-3. CursorItemReader

Database로 대규모의 데이터를 순차적으로 처리할때 가장 보편적으로 사용되는게 Cursor입니다.  

당연히 Databse Batch에서도 이를 사용하고 있습니다.  

쉽게 생각하시면 Database와 어플리케이션 사이에 통로를 하나 연결하고 하나씩 빨아들인다고 생각하시면 됩니다.
JSP나 Servlet으로 게시판을 작성해보신 분들은 ```ResultSet```을 사용해서 ```next()```로 하나씩 데이터를 가져왔던 것을 기억하시면 됩니다.  

### 7-3-1. JdbcCursorItemReader

```sql
create table pay (
  id         bigint not null auto_increment,
  amount     bigint,
  txName     varchar(255),
  txDateTime datetime,
  primary key (id)
) engine = InnoDB;

insert into pay (amount, txName, txDateTime) VALUES (1000, 'trade1', '2018-09-10 00:00:00');
insert into pay (amount, txName, txDateTime) VALUES (2000, 'trade2', '2018-09-10 00:00:00');
insert into pay (amount, txName, txDateTime) VALUES (3000, 'trade3', '2018-09-10 00:00:00');
insert into pay (amount, txName, txDateTime) VALUES (4000, 'trade4', '2018-09-10 00:00:00');
```

### 7-3-2. HibernateCursorItemReader

### CursorItemReader의 주의 사항

CursorItemReader를 사용하실때는 Database와 SocketTimeout을 충분히 큰 값으로 설정해야만 합니다.  
Cursor는 하나의 Connection으로 Batch가 끝날때까지 사용되기 때문에 Batch가 끝나기전에 Database와 어플리케이션의 Connection이 먼저 끊어질수 있습니다.  
  
그래서 **Batch 수행 시간이 오래 걸리는 경우에는 PagingItemReader를 사용하시는게 낫습니다**.  
Paging의 경우 Chunk 단위로 쿼리를 실행/종료하고 Connection도 맺고 끊기 때문에 아무리 많은 데이터라도 타임아웃과 부하 없이 수행될 수 있습니다.


## 7-4. PagingItemReader

### 7-4-1. JdbcPagingItemReader

### 7-4-2. JpaPagingItemReader

### PagingItemReader 주의 사항

* 정렬 (```Order```) 가 무조건 포함되어 있어야 합니다.
    * [paging시 주의사항](https://jojoldu.tistory.com/166)



* 같은 테이블을 조회 & 수정해야 한다면? ([참고](https://stackoverflow.com/questions/26509971/spring-batch-jpapagingitemreader-why-some-rows-are-not-read))




## 7-4. Custom Item Reader

* 마지막에 null을 반환해야 종료가 됨


## 7-5. ItemReader 주의 사항

* 절대 절대 JpaRepository로 ItemReader 커스텀하게 쓰지말것
    * 
* 