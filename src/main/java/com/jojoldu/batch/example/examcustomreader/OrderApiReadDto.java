package com.jojoldu.batch.example.examcustomreader;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Created by jojoldu@gmail.com on 05/11/2019
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@Getter
@NoArgsConstructor
public class OrderApiReadDto {
    private String tradeNo;
    private Long amount;

    @Builder
    public OrderApiReadDto(String tradeNo, Long amount) {
        this.tradeNo = tradeNo;
        this.amount = amount;
    }
}
