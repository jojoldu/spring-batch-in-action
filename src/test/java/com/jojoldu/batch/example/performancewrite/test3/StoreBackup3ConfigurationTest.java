package com.jojoldu.batch.example.performancewrite.test3;

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.entity.product.ProductRepository;
import com.jojoldu.batch.entity.product.Store;
import com.jojoldu.batch.entity.product.StoreRepository;
import com.jojoldu.batch.entity.product.backup.ProductBackupRepository;
import com.jojoldu.batch.entity.product.backup.StoreBackup;
import com.jojoldu.batch.entity.product.backup.StoreBackupRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jojoldu@gmail.com on 20/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestBatchConfig.class, StoreBackup3Configuration.class})
@SpringBatchTest
@TestPropertySource(properties = {"logging.level.org.springframework.jdbc.core.JdbcTemplate=debug"})
public class StoreBackup3ConfigurationTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreBackupRepository storeBackupRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductBackupRepository productBackupRepository;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @AfterEach
    public void after() throws Exception {
        productRepository.deleteAllInBatch();
        productBackupRepository.deleteAllInBatch();

        storeRepository.deleteAllInBatch();
        storeBackupRepository.deleteAllInBatch();
    }

    @Test
    public void H2_Store가_StoreBackup으로_이관된다() throws Exception {
        //given
        String name = "a";

        storeRepository.save(new Store(name));
        storeRepository.save(new Store(name));

        JobParameters jobParameters = new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString("storeName", name)
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        List<StoreBackup> storeBackups = storeBackupRepository.findAll();
        assertThat(storeBackups).hasSize(2);
    }
}
