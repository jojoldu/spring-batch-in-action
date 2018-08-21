# 6. Chunk 지향 처리
 
이번 시간부터 Step의 구성 요소를 Tasklet이 아닌 Reader/Processor/Writer로 시작합니다.  
Spring Batch를 쓰는 가장 큰 이유인 Chunk!  
Chnunk가 무엇인지 한번 살펴보겠습니다.

## 6-1. Chunk?

Spring Batch는 **Chunk 지향 처리** 스타일을 지향합니다.  
여기서 트랜잭션이라는게 중요한데요.  
이 트랜잭션 내에서 실패하는게 있다면 롤백이 됩니다.  
즉, Chunk 단위로 트랜잭션을 수행하기 때문에 실패할 경우엔 해당 Chunk 만큼만 롤백이 되고, 이전에 커밋된 트랜잭션 범위까지는 반영이 된다는 것입니다.  
각 데이터 row는 여전히 ​​개별적으로 읽혀지고 처리되지만, 하나의 청크에 대한 모든 기록은 커밋 될 때 즉시 발생합니다

Chunk 지향 처리를 Java 코드로 표현하면 아래처럼 될 것 같습니다.

```java

List items = new Arraylist();
for(int i = 0; i < chunkSize; i++){
    Object item = itemReader.read()
    Object processedItem = itemProcessor.process(item);
    items.add(processedItem);
}
itemWriter.write(items);
```

> 음 개인적인 생각이지만 [Spring Batch 공식 문서의 그림과 예제코드](https://docs.spring.io/spring-batch/4.0.x/reference/html/index-single.html#chunkOrientedProcessing)는 오해의 소지가 있다고 생각합니다.  
자칫 잘못하면 Chunk 단위로 읽는게 아닌 개별 건건으로 reader에서 읽는다고 생각할 수 있어서요.  
실제 쿼리르 확인해보시면 아시겠지만, Chnunk만큼 읽어 들인 뒤, reader -> processor로 넘겨주고 있습니다.  
공식 문서의 그림과 코드는 각 Chunk 사이클 내부에서 처리되는 경우를 보여드린것이라고 생각하시면 됩니다.

음.. 사실 저는 이 설명이 적절하다고 생각하진 않습니다.  

* SimpleChunkProcessor.transform
* ChunkOrientedTasklet.execute
* SimpleChunkProcessor.provide
