# Spring Batch Reader로 Read DB 사용하기 (feat. AWS Aurora)


```java
spring:
  datasource:
    hikari:
      jdbc-url: jdbc:mysql:aurora://~~~
      username: ~~
      password: ~~
      driver-class-name: org.mariadb.jdbc.Driver
```

```java
public interface ShopRepository extends JpaRepository<Shop, Long> {
    @Transactional(readOnly = true)
    List<Shop> findAllByShopNo(Long shopNo);
}
```