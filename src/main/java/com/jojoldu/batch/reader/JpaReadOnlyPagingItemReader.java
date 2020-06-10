package com.jojoldu.batch.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Created by jojoldu@gmail.com on 03/06/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

/**
 * 안되네
 */
@Slf4j
public class JpaReadOnlyPagingItemReader<T> extends JpaPagingItemReader<T> {

    private TransactionTemplate readerTransactionTemplate;
    private PlatformTransactionManager transactionManager;

    public JpaReadOnlyPagingItemReader() {
    }

    public JpaReadOnlyPagingItemReader(PlatformTransactionManager transactionManager) {
        setTransacted(false);
        setReaderTransactionTemplate(transactionManager);
    }

    public void setReaderTransactionTemplate(PlatformTransactionManager transactionManager) {
        this.readerTransactionTemplate = new TransactionTemplate(transactionManager);
        this.readerTransactionTemplate.setReadOnly(true);
        this.readerTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactionManager = transactionManager;
    }

    @Override
    protected void doReadPage() {
        readerTransactionTemplate.executeWithoutResult(status -> super.doReadPage());

//        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
//        definition.setReadOnly(true);
//        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//        TransactionStatus tx = transactionManager.getTransaction(definition);
//        super.doReadPage();
//        tx.flush();

    }

}
