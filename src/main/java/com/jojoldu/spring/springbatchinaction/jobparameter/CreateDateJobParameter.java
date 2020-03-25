package com.jojoldu.spring.springbatchinaction.jobparameter;

import com.jojoldu.spring.springbatchinaction.reader.jpa.ProductStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Getter
@NoArgsConstructor
public class CreateDateJobParameter {

    @Value("#{ T(java.time.LocalDate).parse(jobParameters[createDate], T(java.time.format.DateTimeFormatter).ofPattern('yyyy-MM-dd'))}")

//    @Value("#{ T(java.time.LocalDate).parse(jobParameters[createDate])}")
    private LocalDate createDate;

    @Value("#{jobParameters[status]}")
    private ProductStatus status;

//    @Value("#{jobParameters[createDate]}")
//    public void setCreateDate(String createDate) {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        this.createDate = LocalDate.parse(createDate, formatter);
//    }
}
