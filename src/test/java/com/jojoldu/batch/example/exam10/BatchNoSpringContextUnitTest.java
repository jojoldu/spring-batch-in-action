package com.jojoldu.batch.example.exam10;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

/**
 * Created by jojoldu@gmail.com on 09/10/2019
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
public class BatchNoSpringContextUnitTest {

    private DataSource dataSource;

    private ConfigurableApplicationContext context;

    @Before
    public void setUp() {
        this.context = new AnnotationConfigApplicationContext(TestDataSourceConfiguration.class);
        this.dataSource = (DataSource) context.getBean("dataSource");
    }

    @After
    public void tearDown() {
        if(this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void testSimpleScenario() throws Exception {
        JdbcCursorItemReader<Foo> reader = new JdbcCursorItemReaderBuilder<Foo>()
                .dataSource(this.dataSource)
                .name("fooReader")
                .sql("SELECT * FROM FOO ORDER BY FIRST")
                .rowMapper((rs, rowNum) -> {
                    Foo foo = new Foo();

                    foo.setFirst(rs.getInt("FIRST"));
                    foo.setSecond(rs.getString("SECOND"));
                    foo.setThird(rs.getString("THIRD"));

                    return foo;
                })
                .build();

        ExecutionContext executionContext = new ExecutionContext();
        reader.open(executionContext);

        validateFoo(reader.read(), 1, "2", "3");
        validateFoo(reader.read(), 4, "5", "6");
        validateFoo(reader.read(), 7, "8", "9");

        assertNull(reader.read());
    }

    private void validateFoo(Foo item, int first, String second, String third) {
        assertEquals(first, item.getFirst());
        assertEquals(second, item.getSecond());
        assertEquals(third, item.getThird());
    }

    @NoArgsConstructor
    @Setter
    @Getter
    public static class Foo {
        private int first;
        private String second;
        private String third;
    }

    @Configuration
    public static class TestDataSourceConfiguration {

        private static final String CREATE_SQL =
                "CREATE TABLE FOO  ( " +
                "ID BIGINT IDENTITY NOT NULL PRIMARY KEY , " +
                "FIRST BIGINT , " +
                "SECOND VARCHAR(5) NOT NULL, " +
                "THIRD VARCHAR(5) NOT NULL) ;";

        private static final String INSERT_SQL =
                "INSERT INTO FOO (FIRST, SECOND, THIRD) VALUES (1, '2', '3');" +
                        "INSERT INTO FOO (FIRST, SECOND, THIRD) VALUES (4, '5', '6');" +
                        "INSERT INTO FOO (FIRST, SECOND, THIRD) VALUES (7, '8', '9');";

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseFactory databaseFactory = new EmbeddedDatabaseFactory();
            databaseFactory.setDatabaseType(H2);
            return databaseFactory.getDatabase();
        }

        @Bean
        public DataSourceInitializer initializer(DataSource dataSource) {
            DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
            dataSourceInitializer.setDataSource(dataSource);

            Resource create = new ByteArrayResource(CREATE_SQL.getBytes());
            Resource insert = new ByteArrayResource(INSERT_SQL.getBytes());
            dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(create, insert));

            return dataSourceInitializer;
        }

    }
}
