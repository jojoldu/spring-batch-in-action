package com.jojoldu.batch.example.multithread;

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.entity.product.Product;
import com.jojoldu.batch.entity.product.ProductRepository;
import com.jojoldu.batch.entity.product.ProductStatus;
import com.jojoldu.batch.entity.product.backup.ProductBackup;
import com.jojoldu.batch.entity.product.backup.ProductBackupRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Created by jojoldu@gmail.com on 12/04/2020
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@ExtendWith(SpringExtension.class)
@SpringBatchTest
@SpringBootTest(classes={MultiThreadCursorConfiguration.class, TestBatchConfig.class})
@TestPropertySource(properties = {"chunkSize=1", "poolSize=5"})
public class MultiThreadCursorConfigurationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductBackupRepository productBackupRepository;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @AfterEach
    void after() {
        productRepository.deleteAll();
        productBackupRepository.deleteAll();
    }

    @Test
    public void Cursor_분산처리_된다() throws Exception {
        //given
        LocalDate createDate = LocalDate.of(2020,4,13);
        ProductStatus status = ProductStatus.APPROVE;
        long price = 1000L;
        for (int i = 0; i < 10; i++) {
            productRepository.save(Product.builder()
                    .price(i * price)
                    .createDate(createDate)
                    .status(status)
                    .build());
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("createDate", createDate.toString())
                .toJobParameters();
        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        List<ProductBackup> backups = productBackupRepository.findAll();
        backups.sort(Comparator.comparingLong(ProductBackup::getPrice));

        assertThat(backups).hasSize(10);
        assertThat(backups.get(0).getPrice()).isEqualTo(0L);
        assertThat(backups.get(9).getPrice()).isEqualTo(9000L);
    }

}
