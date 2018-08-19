# 6. Chunk 지향 처리


## 6-1. 

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
