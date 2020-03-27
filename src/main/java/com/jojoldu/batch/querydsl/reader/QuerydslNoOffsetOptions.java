package com.jojoldu.batch.querydsl.reader;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;

public class QuerydslNoOffsetOptions {
    private final NumberPath<Long> id;
    private final WhereExpression where;
    private final OrderExpression order;

    public QuerydslNoOffsetOptions(NumberPath<Long> id, Expression expression) {
        this.id = id;
        this.where = expression.where;
        this.order = expression.order;
    }

    public BooleanExpression whereExpression(Long compare, int page) {
        if(compare == null || page == 0) {
            return null;
        }

        if(where.isGt()) {
            return id.gt(compare);
        }

        return id.lt(compare);
    }

    public OrderSpecifier<Long> orderExpression() {
        if(order.isAsc()) {
            return id.asc();
        }

        return id.desc();
    }

    public enum Expression {
        ASC (WhereExpression.GT, OrderExpression.ASC),
        DESC (WhereExpression.LT, OrderExpression.DESC);

        private final WhereExpression where;
        private final OrderExpression order;

        Expression(WhereExpression where, OrderExpression order) {
            this.where = where;
            this.order = order;
        }
    }

    public enum WhereExpression {
        GT, LT;

        public boolean isGt() {
            return this == GT;
        }
    }

    public enum OrderExpression {
        ASC, DESC;

        public boolean isAsc() {
            return this == ASC;
        }
    }
}
