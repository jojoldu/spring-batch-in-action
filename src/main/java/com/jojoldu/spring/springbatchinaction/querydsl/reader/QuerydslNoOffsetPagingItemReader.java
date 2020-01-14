package com.jojoldu.spring.springbatchinaction.querydsl.reader;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.util.ClassUtils;

import javax.persistence.EntityManagerFactory;
import java.util.function.Function;

public class QuerydslNoOffsetPagingItemReader<T extends BaseEntityId> extends QuerydslPagingItemReader<T> {

    private Object lock = new Object();
    private volatile int current = 0;
    private volatile int page = 0;
    private long currentId = 0;
    private QuerydslNoOffsetOptions options;

    private QuerydslNoOffsetPagingItemReader() {
        super();
        setName(ClassUtils.getShortName(QuerydslNoOffsetPagingItemReader.class));
    }

    public QuerydslNoOffsetPagingItemReader(EntityManagerFactory entityManagerFactory, int pageSize, QuerydslNoOffsetOptions options, Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
        this();
        super.entityManagerFactory = entityManagerFactory;
        super.queryFunction = queryFunction;
        this.options = options;
        setPageSize(pageSize);
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

    private T readItem() {
        int next = current++;
        if (next < results.size()) {
            return results.get(next);
        }

        return null;

    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doReadPage() {

        clearIfTransacted();

        JPAQuery<T> query = createQuery().limit(getPageSize());

        initResults();

        fetchQuery(query);

        resetCurrentId();
    }

    private JPAQuery<T> createQuery() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        return queryFunction.apply(queryFactory)
                .where(options.whereExpression(currentId, page))
                .orderBy(options.orderExpression());
    }

    private void resetCurrentId() {
        if (results.size() > 0) {
            currentId = results.get(results.size() - 1).getId();
        }
    }

}
