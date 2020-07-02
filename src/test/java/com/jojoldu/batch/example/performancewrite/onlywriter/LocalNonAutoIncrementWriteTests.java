package com.jojoldu.batch.example.performancewrite.onlywriter;

/**
 * Created by jojoldu@gmail.com on 01/07/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.entity.person.Person2;
import com.jojoldu.batch.entity.person.Person2Repository;
import com.jojoldu.batch.entity.person.PersonRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestBatchConfig.class})
@Transactional
@DirtiesContext
public class LocalNonAutoIncrementWriteTests {

    private static final long TEST_COUNT = 10;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private Person2Repository person2Repository;

    @Autowired
    private DataSource dataSource;

    @AfterEach
    public void after() {
        personRepository.deleteAllInBatch();
        person2Repository.deleteAllInBatch();
    }

    @Test
    public void non_auto_increment_test_merge() throws Exception {
        // given
        JpaItemWriter<Person2> writer = new JpaItemWriterBuilder<Person2>()
                .entityManagerFactory(this.entityManagerFactory)
                .build();

        writer.afterPropertiesSet();
        List<Person2> items = new ArrayList<>();
        for (long i = 0; i < TEST_COUNT; i++) {
            items.add(new Person2(i, "foo" + i));
        }

        // when
        writer.write(items);
    }

    @Test
    public void non_auto_increment_test_persist() throws Exception {
        // given
        JpaItemWriter<Person2> writer = new JpaItemWriterBuilder<Person2>()
                .usePersist(true)
                .entityManagerFactory(this.entityManagerFactory)
                .build();

        writer.afterPropertiesSet();
        List<Person2> items = new ArrayList<>();
        for (long i = 0; i < TEST_COUNT; i++) {
            items.add(new Person2(i, "foo" + i));
        }

        // when
        writer.write(items);
    }

    @Test
    public void non_auto_increment_test_jdbc() throws Exception {
        //given
        JdbcBatchItemWriter<Person2> writer = new JdbcBatchItemWriterBuilder<Person2>()
                .dataSource(dataSource)
                .sql("insert into person(id, name) values (:id, :name)")
                .beanMapped()
                .build();

        writer.afterPropertiesSet();
        List<Person2> items = new ArrayList<>();
        for (long i = 0; i < TEST_COUNT; i++) {
            items.add(new Person2(i, "foo" + i));
        }

        // when
        writer.write(items);
    }
}
