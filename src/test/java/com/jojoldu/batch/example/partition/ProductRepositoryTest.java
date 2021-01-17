package com.jojoldu.batch.example.partition;

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.entity.product.Product;
import com.jojoldu.batch.entity.product.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jojoldu@gmail.com on 20/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestBatchConfig.class, PartitionLocalConfiguration.class})
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @AfterEach
    public void after() throws Exception {
        productRepository.deleteAllInBatch();
    }

    @Test
    void MAX_ID_찾기() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        String name = "a";
        int expected1 = 1000;
        int expected2 = 2000;
        productRepository.save(new Product(name, expected1, txDate));
        Product expectEntity = productRepository.save(new Product(name, expected2, txDate));

        //when
        Long maxId = productRepository.findMaxId(txDate, txDate.plusDays(1));
        //then
        assertThat(maxId).isEqualTo(expectEntity.getId());
    }

    @Test
    void MIN_ID_찾기() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        String name = "a";
        int expected1 = 1000;
        int expected2 = 2000;
        Product expectEntity = productRepository.save(new Product(name, expected2, txDate));
        productRepository.save(new Product(name, expected1, txDate));

        //when
        Long maxId = productRepository.findMinId(txDate, txDate.plusDays(1));
        //then
        assertThat(maxId).isEqualTo(expectEntity.getId());
    }
}
