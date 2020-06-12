package com.jojoldu.batch.entity.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Created by jojoldu@gmail.com on 20/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

public interface ProductRepository extends JpaRepository <Product, Long> {

    @Transactional(readOnly = true)
    @Query("SELECT p.name " +
            "FROM Product p " +
            "WHERE p.createDate =:createDate")
    List<String> findAllByCreateDate(@Param("createDate") LocalDate createDate);

}
