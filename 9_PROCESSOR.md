# 9. ItemProcessor

7,8 장에서는 Chunk 지향 처리에서의 데이터 읽기와 쓰기 부분을 소개 드렸습니다.  
이번 챕터에서는 읽기와 쓰기가 아닌, 가공 (혹은 처리) 단계를 소개드리겠습니다.  
바로 **ItemProcessor**입니다.  

여기서 한가지 드리고 싶은 말씀은 **ItemProcessor는 필수가 아니라는 점**입니다.  
ItemProcessor는 데이터를 가공하거나 필터링하는 역할을 합니다.  
이는 **Writer 부분에서도 충분히 구현 가능**한데요.  

그럼에도 ItemProcessor를 쓰는 것은 Reader, Writer 와는 별도의 단계로 분리되었기 때문에 **비지니스 코드가 섞이는 것을 방지**해주기 때문입니다.  
  
그래서 일반적으로 배치 어플리케이션에서 비즈니스 로직을 추가할때는 가장 먼저 Processor를 고려해보시는 것을 추천드립니다.  
각 계층 (읽기/처리/쓰기)를 분리할 수 있는 좋은 방법입니다.  

* process 단계에서 처리 할 수 있는 비즈니스 로직의 종류 
* 청크 지향 처리에서 ItemProcessor 를 구성하는 방법 
* Spring Batch와 함께 제공되는 ItemProcessor 구현

등등을 살펴보겠습니다.

## 9-1. ItemProcessor 소개

![process](./images/9/process.png)

ItemProcessor는 **Reader에서 넘겨준 데이터 개별건을 가공/처리**해줍니다.  
ItemWriter에서는 ChunkSize 단위로 묶은 데이터를 한번에 처리하는 것과는 대조됩니다.  


일반적으로 ItemProcessor를 사용하는 방법은 2가지 입니다.

* 변환
    * Reader에서 읽은 데이터를 원하는 타입으로 변환해서 Writer에 넘겨 줄 수 있습니다.
* 필터
    * Reader에서 넘겨준 데이터를 Writer로 넘겨줄 것인지를 결정할 수 있습니다.
    * ```null```을 반환하면 Writer에 전달되지 않습니다

## 기본

이 ItemProcessor인터페이스는 두 개의 유형 인수 I와 O다음을 사용합니다 .

```java
package org.springframework.batch.item;

public interface ItemProcessor<I, O> {

  O process(I item) throws Exception;

}
```

스프링 배치 유형의 읽기 항목을 전달 I받는 process방법. 유형 I은 항목 판독기 유형과 호환 가능해야합니다.
이 process메소드는 O,Spring Batch가 항목 작성자에게 보내는 형식 항목을 반환 하며 형식과 호환되는 형식도 반환합니다 O.
당신은 구체적인 유형을 정의 I하고 O사용자의 ItemProcessor구현입니다. 는 IF process메소드가 리턴 null(;이 나중에 더 필터링을 건너 뛰는 다르다), 스프링 배치는 필터링 계약에 의해 정의 된대로, 작가에 항목을 전송하지 않습니다. 다음 목록은 필터링을 구현하는 방법을 보여줍니다 ItemProcessor.

## JpaItemReader 사용시 주의 사항

트랜잭션 범위가 

![writer트랜잭션](./images/9/writer트랜잭션.png)

## 주의 사항

