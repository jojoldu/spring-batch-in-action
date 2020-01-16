package com.jojoldu.spring.springbatchinaction.querydsl.reader;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManagerFactory;
import java.util.function.Function;

public class QuerydslNoOffsetPagingItemReader<T extends BaseEntityId> extends QuerydslPagingItemReader<T> {

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
    @SuppressWarnings("unchecked")
    protected void doReadPage() {

        clearIfTransacted();

        JPAQuery<T> query = createQuery().limit(getPageSize());

        initResults();

        fetchQuery(query);

        resetCurrentId();
    }

    @Override
    protected JPAQuery<T> createQuery() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        return queryFunction.apply(queryFactory)
                .where(options.whereExpression(currentId, getPage()))
                .orderBy(options.orderExpression());
    }

    private void resetCurrentId() {
        if (!CollectionUtils.isEmpty(results)) {
            currentId = results.get(results.size() - 1).getId();
            if (logger.isDebugEnabled()) {
                logger.debug("Current Id " + currentId);
            }
        }
    }

}
