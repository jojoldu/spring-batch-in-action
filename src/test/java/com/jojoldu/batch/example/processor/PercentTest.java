package com.jojoldu.batch.example.processor;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by jojoldu@gmail.com on 2018. 10. 22.
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

public class PercentTest {

    @Test
    public void 짝수를_숫자2로_퍼센트로_나누면_0이된다() {
        //given
        long id = 2;

        //when
        long remain = id % 2;

        //then
        assertThat(remain, is(0L));
    }
}
