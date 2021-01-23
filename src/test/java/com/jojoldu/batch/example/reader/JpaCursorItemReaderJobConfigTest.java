package com.jojoldu.batch.example.reader;

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.entity.pay.Pay;
import com.jojoldu.batch.entity.pay.PayRepository;
import com.jojoldu.batch.example.reader.jpa.JpaCursorItemReaderJobConfig;
import com.jojoldu.batch.example.reader.jpa.JpaPagingItemReaderJobConfig;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by jojoldu@gmail.com on 18/10/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@RunWith(SpringRunner.class)
@SpringBatchTest
@SpringBootTest(classes = {JpaCursorItemReaderJobConfig.class, TestBatchConfig.class})
public class JpaCursorItemReaderJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PayRepository payRepository;

    @After
    public void tearDown() throws Exception {
        payRepository.deleteAllInBatch();
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void JPA_페이징_조회() throws Exception {
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
        assertThat(jobExecution.getStatus(), is(BatchStatus.COMPLETED));
    }

}
