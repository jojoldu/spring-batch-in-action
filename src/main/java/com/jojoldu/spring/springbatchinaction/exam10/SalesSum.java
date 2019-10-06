package com.jojoldu.spring.springbatchinaction.exam10;

/**
 * Created by jojoldu@gmail.com on 06/10/2019
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
public class SalesSum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate startDate;
    private LocalDate endDate;
    private long amountSum;

    @Builder
    public SalesSum(LocalDate startDate, LocalDate endDate, long amountSum) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.amountSum = amountSum;
    }
}
