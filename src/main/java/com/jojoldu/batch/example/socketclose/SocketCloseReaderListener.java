package com.jojoldu.batch.example.socketclose;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemReadListener;

/**
 * Created by jojoldu@gmail.com on 15/09/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@Slf4j
public class SocketCloseReaderListener<T> implements ItemReadListener<T> {

    @Override
    public void beforeRead() {
        log.info("beforeRead start");
        try {
            Thread.sleep(70_000);// 2.5% 버퍼 대비 넉넉하게 70초로
        } catch (InterruptedException e) {
            throw new IllegalStateException("Listener Exception");
        }
        log.info("beforeRead end");
    }

    @Override
    public void afterRead(Object item) {

    }

    @Override
    public void onReadError(Exception ex) {

    }
}
