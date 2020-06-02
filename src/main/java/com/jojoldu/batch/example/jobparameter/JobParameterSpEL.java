package com.jojoldu.batch.example.jobparameter;

/**
 * Created by jojoldu@gmail.com on 25/03/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
public interface JobParameterSpEL {
    String LOCAL_DATE = "#{ T(java.time.LocalDate).parse(jobParameters[createDate], T(java.time.format.DateTimeFormatter).ofPattern('yyyy-MM-dd'))}";

}
