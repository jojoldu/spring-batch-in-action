package com.jojoldu.spring.springbatchinaction.querydsl;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class QuerydslNoOffsetPagingItemReader<T extends BaseEntityId> extends AbstractPagingItemReader<T> {

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
