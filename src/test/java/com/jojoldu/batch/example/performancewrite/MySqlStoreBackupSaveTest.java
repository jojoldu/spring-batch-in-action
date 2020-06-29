package com.jojoldu.batch.example.performancewrite;

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.entity.product.Store;
import com.jojoldu.batch.entity.product.StoreRepository;
import com.jojoldu.batch.example.performancewrite.test1.StoreBackup1Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jojoldu@gmail.com on 20/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestBatchConfig.class, StoreBackup1Configuration.class})
@SpringBatchTest
@ActiveProfiles(profiles = "real")
public class MySqlStoreBackupSaveTest {

    @Autowired
    private StoreRepository storeRepository;


    @Test
    public void MySQL_Store를_저장한다() throws Exception {
        //given
        String name = "a";
        List<Store> stores = new ArrayList<>();

        int count = 100_000;
        for (int i = 0; i < count; i++) {
             stores.add(new Store(name));
        }

        storeRepository.saveAll(stores);

        //then
        List<Store> result = storeRepository.findAll();
        assertThat(result).hasSize(count);
    }
}
