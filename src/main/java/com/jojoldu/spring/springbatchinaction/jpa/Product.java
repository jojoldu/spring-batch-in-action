package com.jojoldu.spring.springbatchinaction.jpa;

/**
 * Created by jojoldu@gmail.com on 20/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private long price;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "product_id")
    private Store store;

    public Product(String name, long price) {
        this.name = name;
        this.price = price;
    }

    public void changeStore(Store store) {
        this.store = store;
    }
}
