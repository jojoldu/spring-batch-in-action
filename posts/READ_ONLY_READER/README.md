# Spring Batch에서 Read Only Reader 사용하기


```java
public interface ShopRepository extends JpaRepository<Shop, Long> {
    @Transactional(readOnly = true)
    List<Shop> findAllByShopNo(Long shopNo);
}
```