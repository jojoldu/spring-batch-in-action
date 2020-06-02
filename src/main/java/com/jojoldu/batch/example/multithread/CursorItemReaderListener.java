package com.jojoldu.batch.example.multithread;

import com.jojoldu.batch.entity.product.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemReadListener;

/**
 * Created by jojoldu@gmail.com on 15/04/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@Slf4j
public class CursorItemReaderListener implements ItemReadListener<Product> {

    @Override
    public void beforeRead() {

    }

    @Override
    public void afterRead(Product item) {
        log.info("Reading Item id={}", item.getId());
    }

    @Override
    public void onReadError(Exception ex) {

    }
}
