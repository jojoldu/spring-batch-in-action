# 향상된 QuerydslPagingItemReader

## 1. 개요

MySQL 특성상 페이징이 뒤로 갈수록 느려진다.

이는 offset의 작동원리 때문인데, 아래와 같은 쿼리 형태가 offset이 커질수록 느리다는 의미이다.

```sql
SELECT  *
FROM  items
WHERE  messy_filtering
ORDER BY id DESC
OFFSET  $M  LIMIT $N
```

이 문제를 해결하기 위해서는 2가지 해결책이 있다.


### 1) 서브쿼리 + Join 으로 해결하기

참고: https://elky84.github.io/2018/10/05/mysql/

```sql
SELECT  *
FROM  items as i
JOIN (SELECT id
        FROM items
        WHERE messy_filtering
        ORDER BY id DESC
        OFFSET  $M  LIMIT $N) as temp on temp.id = i.id
```

### 2) offset을 제거한 페이징쿼리 사용하기

참고: http://mysql.rjweb.org/doc.php/pagination

```sql
SELECT  *
FROM  items
WHERE  messy_filtering AND id < 마지막조회ID
ORDER BY id DESC
LIMIT $N
```

여기서 1번을 사용할 순 없다.

이는 JPQL 에서 from절의 서브쿼리를 지원하지 않기 때문이다.

그래서 2번의 방식으로 해결한다.

2번은 RepositoryItemReader를 사용하기 보다는 (이러면 매번 사용하는 사람이 BatchItemReader를 직접 구현해야한다)

QuerydslPagingItemReader와 마찬가지로 NoOffsetReader를 만들어 이를 사용하게 할 계획이다.


## 2. QuerydslNoOffsetPagingItemReader

```java
public class QuerydslNoOffsetPagingItemReader<T extends BaseEntityId> extends AbstractPagingItemReader <T>  {

    private final Map<String, Object> jpaPropertyMap = new HashMap<>();
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private Function<JPAQueryFactory, JPAQuery<T>> queryFunction;
    private boolean transacted = true;//default value
    private Object lock = new Object();
    private volatile int current = 0;
    private volatile int page = 0;
    private long currentId = 0;
    private QuerydslNoOffsetOptions options;

    private QuerydslNoOffsetPagingItemReader() {
        setName(ClassUtils.getShortName(QuerydslNoOffsetPagingItemReader.class));
    }

    public QuerydslNoOffsetPagingItemReader(EntityManagerFactory entityManagerFactory, int pageSize, QuerydslNoOffsetOptions options, Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
        this();
        this.entityManagerFactory = entityManagerFactory;
        this.queryFunction = queryFunction;
        this.options = options;
        setPageSize(pageSize);
    }

    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    @Override
    protected void doOpen() throws Exception {
        super.doOpen();

        entityManager = entityManagerFactory.createEntityManager(jpaPropertyMap);
        if (entityManager == null) {
            throw new DataAccessResourceFailureException("Unable to obtain an EntityManager");
        }
    }

    @Override
    protected T doRead() throws Exception {

        synchronized (lock) {

            if (results == null || current >= getPageSize()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Reading page=" + page + ", currentId=" + currentId);
                }

                doReadPage();
                page++;
                if (current >= getPageSize()) {
                    current = 0;
                }
            }

            return readItem();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doReadPage() {

        if (transacted) {
            entityManager.clear();
        }//end if

        JPAQuery<T> query = createQuery().limit(getPageSize());

        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<T>();
        } else {
            results.clear();
        }

        if (!transacted) {
            List<T> queryResult = query.fetch();
            for (T entity : queryResult) {
                entityManager.detach(entity);
                results.add(entity);
            }
        } else {
            results.addAll(query.fetch());
        }

        resetCurrentId();
    }

    private JPAQuery<T> createQuery() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        return queryFunction.apply(queryFactory)
                .where(options.whereExpression(currentId, page))
                .orderBy(options.orderExpression());
    }

    private T readItem() {
        int next = current++;
        if (next < results.size()) {
            return results.get(next);
        }
        else {
            return null;
        }
    }

    private void resetCurrentId() {
        if(results.size() > 0) {
            currentId = results.get(results.size()-1).getId();
        }
    }

    @Override
    protected void doJumpToPage(int itemIndex) {
    }

    @Override
    protected void doClose() throws Exception {
        entityManager.close();
        super.doClose();
    }
}
```

## 참고

* [http://mysql.rjweb.org/doc.php/pagination](http://mysql.rjweb.org/doc.php/pagination)