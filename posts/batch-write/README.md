# Spring Batch JPA에서의 Write 성능 향상 전략


* AWS RDS Aurora r5.large

## 1. Merge vs Persist

[Spring Batch 4.2 버전에 도입](https://spring.io/blog/2019/09/17/spring-batch-4-2-0-rc1-is-released#faster-writes-with-the-code-jpaitemwriter-code) 되었습니다.

> Spring Boot 2.2.8 부터 사용 가능합니다.

이 함수를 JpaItemWriter사용하여 ```EntityManager.merge```
JPA 지속성 컨텍스트에서 항목을 씁니다.  
이는 항목의 지속적 상태를 알 수 없거나 업데이트 된 것으로 알려진 경우에 적합합니다. 
그러나 데이터가 새로운 것으로 알려져 있고 삽입으로 간주되어야하는 많은 파일 처리 작업에서는 ```EntityManager.merge```는 효율적이지 않습니다.

이 릴리스에서는 이러한 시나리오 보다는 JpaItemWriter사용 persist하기 위해 새로운 옵션을 도입했습니다. 
이 새로운 옵션을 사용하면 JpaItemWriter벤치 마크 jpa-writer-benchmark 에 따라 데이터베이스에 백만 개의 항목을 삽입 하는 데 사용하는 파일 수집 작업 이 2 배 빠릅니다 .

연관된 객체가 proxy면 proxy객체를 확인하려고 select 쿼리를 조회한다.

이 클래스는 merge 대신 persist를 호출한다. 따라서 항상 새로운 객체를 저장할 때만 사용해야 한다.
저장할 엔티티가 영속성 컨텍스트에 있다. -> dirty checking
저장할 엔티티가 영속성 컨텍스트에 없다. -> persist 호출

## 2. JPQL vs SQL

엔티티가 지속될 때마다 Hibernate는 엔티티의 Map 역할을하는 현재 실행중인 지속성 컨텍스트에이를 첨부해야합니다.  
Map 키는 엔티티 유형 (자바 클래스) 및 엔티티 ID로 구성됩니다.
IDENTITY 열의 경우 식별자 값을 알 수있는 유일한 방법은 SQL INSERT를 실행하는 것입니다.  
따라서 INSERT는 persist 메소드가 호출 될 때 실행되며 플러시 시간까지 비활성화 할 수 없습니다.
이러한 이유로 Hibernate는 IDENTITY 생성기 전략을 사용하여 엔티티에 대한 JDBC 일괄 삽입을 비활성화합니다.

batch 형태의 SQL로 재작성 하는 것입니다. 
기본값이 false 이기 때문에 multi value 형태로 실행하기 위해서는 변경이 필요합니다. JDBC URL에 rewriteBatchedStatements=true 옵션을 추가하고 다시 실행해보겠습니다.
 
[Hibernate 공식문서](https://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/Hibernate_User_Guide.html#batch-session-batch-insert)
[Why you should never use the TABLE identifier generator with JPA and Hibernate](https://vladmihalcea.com/why-you-should-never-use-the-table-identifier-generator-with-jpa-and-hibernate/)

## 3. 최종 비교
