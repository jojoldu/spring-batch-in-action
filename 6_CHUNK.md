# 6. Chunk 지향 처리
 
이번 시간부터 Step의 구성 요소를 Tasklet이 아닌 Reader/Processor/Writer로 시작합니다.  


## 6-1. Chunk?

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

음.. 사실 저는 이 설명이 적절하다고 생각하진 않습니다.  

* SimpleChunkProcessor.transform
* ChunkOrientedTasklet.execute
* SimpleChunkProcessor.provide
