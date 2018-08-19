# 7. ItemReader


## 7-1. ItemReader 소개

## DB & JPA

* JpaItemReader
* JpaPagingItemReader
    * IbatisReader는 Spring Batch 4.0에 오면서 삭제됨
* 같은 테이블을 조회 & 수정해야 한다면? ([참고](https://stackoverflow.com/questions/26509971/spring-batch-jpapagingitemreader-why-some-rows-are-not-read))

## Item Stream Interface

* ItemStream?
* 페이징과의 차이점
* JpaCursorItemReader


## 주의 사항

* 절대 절대 JpaRepository로 ItemReader 커스텀하게 쓰지말것
    * 
* 