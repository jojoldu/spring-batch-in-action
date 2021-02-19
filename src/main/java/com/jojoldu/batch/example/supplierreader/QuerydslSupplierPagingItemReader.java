package com.jojoldu.batch.example.supplierreader;


import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;

@Slf4j
public class QuerydslSupplierPagingItemReader<T> extends AbstractPagingItemReader<T> {

    private final BiFunction<Integer, Integer, Collection<T>> repositorySupplier;

    @Builder
    public QuerydslSupplierPagingItemReader(BiFunction<Integer, Integer, Collection<T>> repositorySupplier, int pageSize) {

        this.repositorySupplier = repositorySupplier;
        this.setPageSize(pageSize);
    }

    @Override
    protected void doReadPage() {
        initializeResults();

        int offset = getPage() * getPageSize();
        results.addAll(repositorySupplier.apply(offset, getPageSize()));
    }

    private void initializeResults() {
        if (results == null) {
            results = new ArrayList<>();
        } else {
            results.clear();
        }
    }

    @Override
    protected void doJumpToPage(int itemIndex) {
    }
}

