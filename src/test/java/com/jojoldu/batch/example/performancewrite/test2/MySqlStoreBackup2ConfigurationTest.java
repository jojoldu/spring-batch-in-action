package com.jojoldu.batch.example.performancewrite.test2;

import com.jojoldu.batch.TestBatchConfig;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jojoldu@gmail.com on 20/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestBatchConfig.class, StoreBackup2Configuration.class})
@SpringBatchTest
@ActiveProfiles(profiles = "real")
public class MySqlStoreBackup2ConfigurationTest {

    @Autowired
    private StoreBackupRepository storeBackupRepository;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @AfterEach
    public void after() throws Exception {
        storeBackupRepository.deleteAllInBatch();
    }

    @Test
    public void MySQL_Store가_StoreBackup으로_이관된다() throws Exception {
        //given
        String name = "a";

        int count = 100_000;

        JobParameters jobParameters = new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString("storeName", name)
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        List<StoreBackup> storeBackups = storeBackupRepository.findAll();
        assertThat(storeBackups).hasSize(count);
    }
}
