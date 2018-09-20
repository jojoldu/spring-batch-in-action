# 8. Writer

앞서 Reader에 대해서 배웠습니다.  
Writer는 Reader, Prcessor와 함께 ChunkOrientedTasklet을 구성하는 3 요소입니다.  
여기서 Processor가 아닌 Writer를 우선 선택한 이유가 궁금 하실수 있습니다.  
  
이유는 **Processor는 선택**이기 때문입니다.  
Reader와 Writer는 ChunkOrientedTasklet에서 필수 요소입니다.  
  
하지만 Processor는 없어도 ChunkOrientedTasklet는 구성할 수 있습니다.  
그래서 Writer를 먼저 다뤄보겠습니다.  
  
## 8-1. ItemWriter 소개

ItemWriter는 Spring Batch에서 사용하는 **출력** 기능입니다.  
Spring Batch가 처음 나왔을 때, ItemWriter는 ItemReader와 마찬가지로 item을 하나씩 다루었습니다.  
그러나 Spring Batch2와 청크 (Chunk) 기반 처리의 도입으로 인해 ItemWriter에도 큰 변화가 있었습니다.  
  
이 업데이트 이후 부터 ItemWriter는 item 하나를 작성하지 않고 **Chunk 단위로 묶인 item List**를 다룹니다.  
이 때문에 ItemWriter 인터페이스는 ItemReader 인터페이스와 약간 다릅니다.  

![itemwriter1](./images/8/itemwriter1.png)

[쳅터 7](https://jojoldu.tistory.com/336)을 보신 분들은 아시겠지만, Reader의 ```read()```는 Item 하나를 반환하는 반면, Writer의 ```write()```는 인자로 Item List를 받습니다.  

이를 그림으로 표현하면 아래와 같습니다.

![write-process](./images/8/write-process.png)

* ItemReader를 통해 각 항목을 개별적으로 읽고 이를 처리하기 위해 ItemProcessor에 전달합니다.  
* 이 프로세스는 청크의 Item 개수 만큼 처리 될 때까지 계속됩니다.  
* 청크 단위만큼 처리가 완료되면 Writer에 전달되어 Writer에 명시되어있는대로 일괄처리합니다.

즉, Reader와 Processor를 거쳐 처리된 Item을 Chunk 단위 만큼 쌓은 뒤 이를 Writer에 전달하는 것입니다.  

> 위 내용은 이미 쳅터6 Chunk 지향 처리에서 상세하게 언급되었습니다.

Spring Batch는 다양한 Output 타입을 처리 할 수 있도록 많은 Writer를 제공합니다.  
Reader와 마찬가지로, 모든 내용을 다루기는 어렵기 때문에 Database와 관련된 내용들만 다루겠습니다.

## 8-2. Database Writer

Java 세계에서는 JDBC 또는 ORM을 사용하여 관계형 데이터베이스에 접근합니다.  
Spring Batch는 JDBC와 ORM 모두 Writer를 제공합니다.  




Writer는 Chunk단위의 마지막 요소입니다.  
그래서 Database의 영속성과 관련해서는 항상 마지막에 Flush를 해줘야만 합니다.  

예를 들어 아래와 같이 영속성을 사용하는 JPA, Hibernate의 경우 ItemWriter 구현체에서는 ```flush()```와 ```session.clear()```가 따라옵니다. 

![flush1](./images/8/flush1.png)

(JpaItemWriter)

![flush2](./images/8/flush2.png)

(HibernateItemWriter)  
  
Writer가 받은 모든 Item이 처리 된 후, Spring Batch는 현재 트랜잭션을 커밋합니다.  

데이터베이스와 관련된 Writer는 아래와 같이 3가지가 있습니다.

* JdbcItemWriter
* HibernateItemWriter
* JpaItemWriter

## 8-3. JdbcItemWriter

![jdbcwrite](./images/8/jdbcwrite.png)

## Custom ItemWriter

Reader와 달리 Writer의 경우 Custom하게 구현해야할 일이 많습니다.

> 물론 Reader 역시 조회용 프레임워크를 어떤걸 쓰는지에 따라 Reader를 Custom 하게 구현해야할 수도 있습니다.  
예를 들면 Querydsl용 ItemReader를 만든다거나, JOOQ용 ItemReader를 만드는 등이 있을 수 있습니다.

## 주의 사항

* [Writer에 List형 Item을 전달하고 싶을때](https://jojoldu.tistory.com/140)