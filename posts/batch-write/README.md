# Spring Batch JPA에서의 Write 성능 향상 전략

대규모 데이터를 처리하는 Spring Batch 에서 배치 성능은 중요한 요소입니다.  
배치 성능에 있어서 튜닝 요소는 크게 2가지로 정리 될 수 있습니다.  

* Reader를 통한 데이터 조회
* Writer를 통한 데이터 등록/수정

여기서 Reader의 경우엔 여러가지 Select Query 튜닝을 통한 개선 이야기가 많이 공유되어있습니다.

> Querydsl을 통한 Paging, No Offset 조회 방법은 [이전 포스팅](https://jojoldu.tistory.com/473) 을 참고하시면 됩니다.

반면 Writer의 경우에는 Reader에 비해서는 공유된 내용이 많지 않습니다.  
그래서 이번 시간에는 **Spring Batch**와 **JPA**를 사용하는 경우에 어떻게 개선할 수 있을지 실제 비교를 해가며 정리하였습니다.  
  
모든 테스트는 아래 환경에서 동일하게 수행하였습니다.

* AWS RDS Aurora r5.large
* Macbook Pro 
  * 32 GB RAM
  * 2.9 GHz Intel Core i7
  * MacOS Mojave 10.14.6

## 1. Merge vs Persist

JPA에서 **Merge는 Insert에서 비효율적**으로 작동을 합니다.  

> Merge는 Entity의 persistent 상태를 알 수 없거나 이미 저장된 것을 변경하는데 유용합니다.

다만, Spring Batch에서는 JpaItemWriter를 통한 write 작업이 신규 생성되는 Entity를 저장하는 기능만 필요할 것인지 알 수가 없으니 ```Merge``` 를 기본 Mode로 작동을 하여 신규저장과 Update에 대한 모든 기능을 처리하도록 구성되었습니다.  
  
그러던 중, [Spring Batch 4.2 버전](https://spring.io/blog/2019/09/17/spring-batch-4-2-0-rc1-is-released#faster-writes-with-the-code-jpaitemwriter-code)에 **선택적으로 Persist 모드**를 선택할 수 있도록 개편되었습니다.

> Spring Boot 2.2.8 부터 사용 가능합니다.


> **항상 새로운 객체를 저장할 때만** 사용해야 합니다.  
> ID가 있는 Entity를 저장할 경우 에러가 발생합니다.

이 함수를 JpaItemWriter사용하여 ```EntityManager.merge```
JPA 지속성 컨텍스트에서 항목을 씁니다.  

이는 
그러나 데이터가 새로운 것으로 알려져 있고 삽입으로 간주되어야하는 많은 파일 처리 작업에서는 ```EntityManager.merge```는 효율적이지 않습니다.

이 릴리스에서는 이러한 시나리오 보다는 JpaItemWriter사용 persist하기 위해 새로운 옵션을 도입했습니다. 
이 새로운 옵션을 사용하면 JpaItemWriter벤치 마크 jpa-writer-benchmark 에 따라 데이터베이스에 백만 개의 항목을 삽입 하는 데 사용하는 파일 수집 작업 이 2 배 빠릅니다 .

연관된 객체가 proxy면 proxy객체를 확인하려고 select 쿼리를 조회한다.

만약 연관된 객체가 Proxy이면 Proxy 객체를 확인하려고 Select 쿼리를 사용합니다만, 



## 2. JPA vs SQL

일반적으로 Batch Insert라 하면 아래와 같은 쿼리를 이야기 합니다.

```sql
INSERT INTO person (name) VALUES
('name1'),
('name2'),
('name3');
```

이렇게 할 경우 

JPA에서는 이런 방식을 공식인 옵션으로 지원을 하는데요.

```yml
spring.jpa.properties.hibernate.jdbc.batch_size
```

* [jpa-hibernate-batch-insert-update](https://www.baeldung.com/jpa-hibernate-batch-insert-update)

JPA에서는 
JPA에서는 Auto Increment일 경우 Batch Insert가 작동하지 않습니다.  
이는 

* [Hibernate 공식문서](https://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/Hibernate_User_Guide.html#batch-session-batch-insert)

그럼 ID 생성 전략을 Auto Increment가 아닌 Table (Sequence)를 선택하면 되지 않을까 생각하게 되는데요.  
아래 글에서 자세하게 설명하고 있지만, **성능상 이슈**와 **Dead Lock에 대한 이슈**로 Auto Increment를 강력하게 추천합니다.

* [Why you should never use the TABLE identifier generator with JPA and Hibernate](https://vladmihalcea.com/why-you-should-never-use-the-table-identifier-generator-with-jpa-and-hibernate/)

그래서 Batch Insert가 안된다고 하여 ID 생성 전략을 변경하는 것은 더 큰 위험이 발생할 수 있습니다.  
  

엔티티가 지속될 때마다 Hibernate는 엔티티의 Map 역할을하는 현재 실행중인 지속성 컨텍스트에이를 첨부해야합니다.  
Map 키는 엔티티 유형 (자바 클래스) 및 엔티티 ID로 구성됩니다.
IDENTITY 열의 경우 식별자 값을 알 수있는 유일한 방법은 SQL INSERT를 실행하는 것입니다.  
따라서 INSERT는 persist 메소드가 호출 될 때 실행되며 플러시 시간까지 비활성화 할 수 없습니다.
이러한 이유로 Hibernate는 IDENTITY 생성기 전략을 사용하여 엔티티에 대한 JDBC 일괄 삽입을 비활성화합니다.

batch 형태의 SQL로 재작성 하는 것입니다. 

> 혹시나 MySQL에서 실행중인 쿼리를 확인했을때 Batch Insert 쿼리가 아니라 단일 Insert 쿼리가 실행중이라면 Spring Boot의 Jdbc-url값에 ```rewriteBatchedStatements``` 옵션 (기본값이 ```false```) 이 ```true``` 인지 확인해보시면 좋습니다.
> 적용방법: ```jdbc:mysql:://DB주소:포트/스키마?rewriteBatchedStatements=true```

 

## 3. 최종 비교

* 부모 Entity는 JpaItemWriter를 이용하여 ChunkSize별로 저장하여 PK값과 Entity를 확보
* PK가 확보된 부모 Entity를 통해 자식 Entity들을 생성 (부모 ID값을 갖고 생성)
* 자식 Entity들은 JdbcItemWriter를 통해 Bulk Insert
