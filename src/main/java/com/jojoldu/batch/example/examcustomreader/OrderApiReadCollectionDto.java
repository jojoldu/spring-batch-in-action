package com.jojoldu.batch.example.examcustomreader;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by jojoldu@gmail.com on 05/11/2019
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@Getter
@NoArgsConstructor
public class OrderApiReadCollectionDto {
    private String code;
    private String message;
    private List<OrderApiReadDto> data;
}
