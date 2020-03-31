# Spring Batch에서 Multi Thread로 Step 실행하기



## PagingItemReader

쓰레드에 안전합니다

* [JdbcPagingItemReader](https://docs.spring.io/spring-batch/docs/current/api/org/springframework/batch/item/database/JdbcPagingItemReader.html)

## CursorItemReader

SynchronizedItemStreamReader 로 Wrapping 하여 처리한다.

### PoolSize를 setter로 받는 이유

개발 환경에서는 1개의 쓰레드로, 운영에선 10개의 쓰레드로 실행해야 될 수 있기 때문입니다.  
몇개의 쓰레드풀을 쓸지를 요청자가 결정할 수 있도록 chunkSize와 마찬가지로 환경변수로 받아서 사용합니다.

## 마무리

* [Spring 공식문서](https://docs.spring.io/spring-batch/docs/current/reference/html/scalability.html#multithreadedStep)