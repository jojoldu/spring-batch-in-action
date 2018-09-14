package com.jojoldu.spring.springbatchinaction.reader.jpa;

import com.jojoldu.spring.springbatchinaction.TestJobLauncher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.CoreMatchers.is;

/**
 * Created by jojoldu@gmail.com on 20/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class JpaJobConfigurationTest {

    @Autowired
    @Qualifier("jpaPagingItemReaderJob")
    private Job job;

    @Autowired
    private TestJobLauncher testJobLauncher;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreProductRepository storeProductRepository;


    @Test
    public void JobParameter생성시점() throws Exception {
        //given
        createStore();

        JobLauncherTestUtils jobLauncherTestUtils = testJobLauncher.getJobLauncherTestUtils(job);
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addString("version", "1");

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(builder.toJobParameters());

        //then
        Assert.assertThat(jobExecution.getStatus(), is(BatchStatus.COMPLETED));
        Assert.assertThat(storeProductRepository.findAll().size(), is(100));
    }

    private void createStore() {
        for(int i=1;i<=100;i++){
            Store store = new Store("store-"+i);
            store.addProduct(new Product("product-"+i, i));
            storeRepository.save(store);
        }
    }

}
