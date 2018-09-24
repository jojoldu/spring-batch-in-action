# spring-batch-in-action

Spring Batch In Action이 2011년 이후 개정판이 나오지도 않고, 한글 번역판도 없고, 국내 Spring Batch 글 대부분이 튜토리얼이거나 공식문서 번역본이라 답답해서 시작  
커머스나 기타 서비스 시스템이 커질수록 배치 작업이 늘어나는데, 주먹구구식으로 Spring Batch 사용하는 경우가 많음  
Spring MVC는 자료가 정말 많지만 Batch는 너무 없어서 정리도 할겸 시작.  


## 목차

예정

* Simple Batch Job
    * 잡실행 결과 리뷰
    * Job, JobInstance, JobExecution 등등 BATCH_JOB 스키마
        * BatchExecution 테이블 내용물 리뷰
    * BatchStatus 소개
* Batch Job Flow
    * Step
    * Flow
    * split & mutli Thread
    * decide
        * JobExecutionDecider를 통한 Step 분기처리
* Batch Step 소개
    * chunk 지향
    * chunk 지향을 코드로
    * reader processor writer 간략 소개
* JobParameter
    * ```@JobScope```, ```@StepScope```, ```@Value```
    * Build => batch.jar 를 실행 => console에 출력되는 파라미터 확인
    * Job Parameter 중복으로 오류나는것도 확인
    * 테스트 코드로 build 없이 수행하는것 확인
    * [StepScope](https://docs.spring.io/spring-batch/3.0.x/reference/html/configureStep.html)
    * [JobScope](https://docs.spring.io/spring-batch/3.0.x/reference/html/configureStep.html)
* Reader
    * custom reader 생성시 ```read()``` 메소드 오버라이딩 주의 사항
        * ```this.data.hasNext()``` 가 false일 경우 무조건 null 반환하도록
        * null이 반환안되면 무한 읽기 시작됨
    * DB & JPA
        * JpaItemReader
        * JpaPagingItemReader
        * IbatisReader는 Spring Batch 4.0에 오면서 삭제됨
        * 같은 테이블을 조회 & 수정해야 한다면? ([참고](https://stackoverflow.com/questions/26509971/spring-batch-jpapagingitemreader-why-some-rows-are-not-read))
    * File
        * FlatFileItemReader
    * 절대 JpaRepository로 repository 커스텀하게 쓰지말것
    * Read Multiple DataSources
    * Item Stream Interface
        * ItemStream? 
        * 페이징과의 차이점
        * JpaCursorItemReader
    * QuerydslPagingItemReader & QuerydslCursorItemReader
* Writer
    * Simple Writer 소개
        * chunk 차이
        * chunk와 page size 다를때 주의사항 소개
    * DB & JPA
        * JDBC writer
        * JPA Writer
    * Write Multiple DataSources
* Processor
    * Simple Processor
    * Processor에서 Filter 처리
    * Processor에서 Validate
        * ```Validator```
        * ```Validator.setFilter(true)``` 로 실패시 건너가기
    * Composite
        * 여러개의 Processor를 조합하고싶을때
        * ```CompositeItemProcessor```
        * ```List<ItemProcessor<Pay,Pay>> delegates```
    * Processor는 Java8의 Stream 과 유사한 개념으로 보면 좋음
* Transactions
    * [기본](https://blog.codecentric.de/en/2012/03/transactions-in-spring-batch-part-1-the-basics/)
    * [Restart, Cursor 기반 Reader, Listeners](https://blog.codecentric.de/en/2012/03/transactions-in-spring-batch-part-2-restart-cursor-based-reading-and-listeners/)
    * [Skip And Retry](https://blog.codecentric.de/en/2012/03/transactions-in-spring-batch-part-3-skip-and-retry/)
* Error
    * Restart
        * ```restartable=false```?
    * Retry
    * Skip
    * Listeners
* runid
* 테스트 코드
* Multi DataSource
    * Multi Datasource
    * Multi EntityManager
    * Multi TxManaer
* Batch 스케줄링은 어떻게? (젠킨스)


