# PagingItemReader에서 FetchJoin 방지하기

Hibernate (Spring Data JPA) 를 사용하다보면 종종 `HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!` 의 `WARN` (경고) 로그 메세지를 만난다.  
해당 로그는 **페이징 처리할때 여러 엔티티를 Fetch Join 을 하면 발생한다**.  

Fetch Join은 N+1 문제를 해결하는 가장 자주 사용되던 방식이다.  
하지만, 경고 메시지에서 언급했듯이 **페이징 처리시에 사용할 경우 페이징이 전혀 적용되지 않고, 조건에 해당하는 모든 데이터를 가져와 메모리에 올려두고 사용**한다.  
  
조건에 해당 하는 데이터 전체를 가져오기 때문에 당연히 성능 상 이슈가 되며, 이를 메모리에 올려두고 페이징을 처리하니 이중으로 성능에 큰 영향을 준다.  
  
다만, Hibernate 특성상 **일관성을 지향하기 때문에 정상적으로 페이징 처리된 것과 동일한 결과를 반환한다**.  
그러다보니 경고 메세지를 놓치고 테스트 코드를 수행하고 서비스를 배포/운영하게 될 여지가 높다.  

그래서 Hibernate 5.2 버전부터 `query.fail_on_pagination_over_collection_fetch` 옵션을 지원하기 시작했다.

## 

```yaml
spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true
```


## 테스트


### JpaPagingItemReader

```java
@Bean(name = JOB_NAME + "_reader")
@StepScope
public JpaPagingItemReader<Teacher> reader() {
        return new JpaPagingItemReaderBuilder<Teacher>()
        .name(JOB_NAME + "_reader")
        .entityManagerFactory(entityManagerFactory)
        .queryString("SELECT distinct(t) FROM Teacher t JOIN FETCH t.students")
        .pageSize(chunkSize)
        .build();
}
```

```java
javax.persistence.PersistenceException: org.hibernate.HibernateException: firstResult/maxResults specified with collection fetch. In memory pagination was about to be applied. Failing because 'Fail on pagination over collection fetch' is enabled.
```

### JpaCursorItemReader

### HibernatePagingItemReader

### HibernateCursorItemReader

## 마무리

Hibernate를 활용한 API 구현에서는 소량의 데이터만 활용할 여지가 있기 때문에 충ㅂ

