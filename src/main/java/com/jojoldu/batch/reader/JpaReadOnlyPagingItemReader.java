package com.jojoldu.batch.reader;

import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Created by jojoldu@gmail.com on 03/06/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
public class JpaReadOnlyPagingItemReader<T> extends JpaPagingItemReader<T>  {

    private TransactionTemplate readerTransactionTemplate;

    public JpaReadOnlyPagingItemReader() {
    }

    public JpaReadOnlyPagingItemReader(PlatformTransactionManager transactionManager) {
        setTransacted(false);
        setReaderTransactionTemplate(transactionManager);
    }

    public void setReaderTransactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setReadOnly(true);
        transactionTemplate.setTransactionManager(transactionManager);
        this.readerTransactionTemplate = transactionTemplate;
    }

    @Override
    protected void doReadPage() {
        readerTransactionTemplate.execute(status -> {
            super.doReadPage();
            return null;
        });
    }

}
