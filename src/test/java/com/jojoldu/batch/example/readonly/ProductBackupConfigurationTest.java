package com.jojoldu.batch.example.readonly;

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.entity.product.Product;
import com.jojoldu.batch.entity.product.ProductBackup;
import com.jojoldu.batch.entity.product.ProductBackupRepository;
import com.jojoldu.batch.entity.product.ProductRepository;
import com.zaxxer.hikari.HikariDataSource;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.jojoldu.batch.config.BatchJpaConfiguration.READER_ENTITY_MANAGER_FACTORY;
import static com.jojoldu.batch.config.DataSourceConfiguration.READER_DATASOURCE;
import static java.time.format.DateTimeFormatter.ofPattern;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jojoldu@gmail.com on 20/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestBatchConfig.class, ProductBackupConfiguration.class})
@SpringBatchTest
public class ProductBackupConfigurationTest {
    public static final DateTimeFormatter FORMATTER = ofPattern("yyyy-MM-dd");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductBackupRepository productBackupRepository;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier(READER_ENTITY_MANAGER_FACTORY)
    EntityManagerFactory readerEmf;

    @Autowired
    DataSource dataSource;

    @Autowired
    @Qualifier(READER_DATASOURCE)
    DataSource readerDataSource;

    @AfterEach
    public void after() throws Exception {
        productRepository.deleteAllInBatch();
        productBackupRepository.deleteAllInBatch();
    }

    @Test
    void readerEmf의_jpaProperties_옵션을_확인한다() throws Exception {
        //given
        Map<String, Object> properties = readerEmf.getProperties();

        //when
        String autoCommit = (String) properties.get("hibernate.connection.provider_disables_autocommit");
        String showSql = (String) properties.get("hibernate.show_sql");

        //then
        assertThat(autoCommit).isEqualTo("true");
        assertThat(showSql).isEqualTo("true");
    }

    @Test
    void readOnly_옵션_적용() throws Exception {
        //given
        HikariDataSource ds = (HikariDataSource) dataSource;
        Connection dsConnection = ds.getConnection();

        HikariDataSource readerDs = (HikariDataSource) readerDataSource;
        Connection readerDsConnection = readerDs.getConnection();

        //then
        assertThat(ds.isReadOnly()).isFalse();
        assertThat(readerDs.isReadOnly()).isTrue();

        assertThat(dsConnection.isReadOnly()).isFalse();
        assertThat(readerDsConnection.isReadOnly()).isTrue();
    }

    @Test
    public void H2_Product가_ProductBackup으로_이관된다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        String name = "a";
        int expected1 = 1000;
        int expected2 = 2000;
        productRepository.save(new Product(name, expected1, txDate));
        productRepository.save(new Product(name, expected2, txDate));

        JobParameters jobParameters = new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString("txDate", txDate.format(FORMATTER))
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        List<ProductBackup> backups = productBackupRepository.findAll();
        assertThat(backups.size()).isEqualTo(2);
    }
}
