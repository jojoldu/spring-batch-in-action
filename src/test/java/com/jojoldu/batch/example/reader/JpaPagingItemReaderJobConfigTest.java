package com.jojoldu.batch.example.reader;

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.entity.pay.Pay;
import com.jojoldu.batch.entity.pay.PayRepository;
import com.jojoldu.batch.example.reader.jpa.JpaPagingItemReaderJobConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBatchTest
@SpringBootTest(classes = {JpaPagingItemReaderJobConfig.class, TestBatchConfig.class})
public class JpaPagingItemReaderJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PayRepository payRepository;

    @AfterEach
    public void tearDown() throws Exception {
        payRepository.deleteAllInBatch();
    }

    @SuppressWarnings("Duplicates")
    @Test
    void JPA_Paging_조회() throws Exception {
        //given
        for (long i = 0; i < 10; i++) {
            payRepository.save(new Pay(i * 1000, String.valueOf(i), LocalDateTime.now()));
        }

        JobParameters jobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
                .addString("version", "1")
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}

