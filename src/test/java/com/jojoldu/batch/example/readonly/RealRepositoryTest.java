package com.jojoldu.batch.example.readonly;

import com.jojoldu.batch.entity.product.Product;
import com.jojoldu.batch.entity.product.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

/**
 * Created by jojoldu@gmail.com on 20/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles(profiles = "real")
public class RealRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @AfterEach
    public void after() throws Exception {
        productRepository.deleteAllInBatch();
    }

    @Test
    void 트랜잭션_readOnly_테스트() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        String name = "a";
        int expected1 = 1000;
        int expected2 = 2000;
        productRepository.save(new Product(name, expected1, txDate));
        productRepository.save(new Product(name, expected2, txDate));

        //when
        productRepository.findAllByCreateDate(txDate);
        //then
    }
}
