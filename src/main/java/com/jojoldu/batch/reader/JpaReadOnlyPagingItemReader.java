package com.jojoldu.batch.reader;

import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by jojoldu@gmail.com on 03/06/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
public class JpaReadOnlyPagingItemReader<T> extends JpaPagingItemReader<T>  {

    @Transactional(readOnly = true)
    @Override
    protected void doReadPage() {
        super.doReadPage();
    }

}
