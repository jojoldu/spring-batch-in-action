# PagingItemReader에서 FetchJoin 방지하기

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

