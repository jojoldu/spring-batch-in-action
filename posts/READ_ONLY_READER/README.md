# Spring Batch Reader에서 Read DB 사용하기 (feat. AWS Aurora)


![rds](./images/rds.png)

```java
spring:
  datasource:
    hikari:
      jdbc-url: jdbc:mysql:aurora://~~~ (1)
      username: ~~
      password: ~~
      driver-class-name: org.mariadb.jdbc.Driver (2)
```

(1) ```jdbc:mysql:aurora```

(2) ```org.mariadb.jdbc.Driver```

```java
public interface ShopRepository extends JpaRepository<Shop, Long> {
    @Transactional(readOnly = true)
    List<Shop> findAllByShopNo(Long shopNo);
}
```

```java
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

        return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), jpaProperties.getProperties(), null)
                .dataSource(dataSource)
                .properties(hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings()))
                .persistenceUnit("master")
                .packages(PACKAGE)
                .build();
    }
```