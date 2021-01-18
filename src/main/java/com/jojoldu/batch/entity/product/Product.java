package com.jojoldu.batch.entity.product;

/**
 * Created by jojoldu@gmail.com on 20/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(indexes = {
        @Index(name = "idx_product_1", columnList = "store_id")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private long price;
    private LocalDate createDate;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "store_id")
    private Store store;

    public Product(long price, LocalDate createDate) {
        this.name = String.valueOf(price);
        this.price = price;
        this.createDate = createDate;
        this.status = ProductStatus.APPROVE;
    }

    public Product(String name, long price, LocalDate createDate) {
        this.name = name;
        this.price = price;
        this.createDate = createDate;
        this.status = ProductStatus.APPROVE;
    }

    @Builder
    public Product(String name, long price, LocalDate createDate, ProductStatus status) {
        this.name = name;
        this.price = price;
        this.createDate = createDate;
        this.status = status;
    }

    public void changeStore(Store store) {
        this.store = store;
    }
}
