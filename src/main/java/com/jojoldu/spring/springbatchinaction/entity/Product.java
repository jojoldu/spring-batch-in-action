package com.jojoldu.spring.springbatchinaction.entity;

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
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private long price;
    private LocalDate createDate;

    @Builder
    public Product(String name, long price, LocalDate createDate) {
        this.name = name;
        this.price = price;
        this.createDate = createDate;
    }
}