package com.jojoldu.batch.example.jobparameter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Created by jojoldu@gmail.com on 27/03/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class LocalDateConverter {
    private static final String LOCAL_DATE_PATTERN = "yyyy-MM-dd";

    public static LocalDate convert(String source) {
        return LocalDate.parse(source, DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN));
    }
}
