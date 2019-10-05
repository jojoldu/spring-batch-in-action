package com.jojoldu.spring.springbatchinaction.jobparameter;

import com.jojoldu.spring.springbatchinaction.reader.jpa.Product;
import com.jojoldu.spring.springbatchinaction.reader.jpa.ProductRepository;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;

import static com.jojoldu.spring.springbatchinaction.jobparameter.JobParameterExtendsConfiguration.JOB_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = "job.name="+JOB_NAME)
public class JobParameterExtendsConfigurationTest {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @After
    public void tearDown() throws Exception {
        productRepository.deleteAll();
    }

    @Test
    public void jobParameter정상출력_확인() throws Exception{
        //given
        LocalDate createDate = LocalDate.of(2019,9,26);
        long price = 1000L;
        productRepository.save(Product.builder().price(price).createDate(createDate).build());

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("createDate", createDate.toString())
                .toJobParameters();
        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
