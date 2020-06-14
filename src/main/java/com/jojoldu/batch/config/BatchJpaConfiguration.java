package com.jojoldu.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Objects;

import static com.jojoldu.batch.config.BatchJpaConfiguration.MASTER_ENTITY_MANAGER_FACTORY;
import static com.jojoldu.batch.config.BatchJpaConfiguration.MASTER_TX_MANAGER;
import static com.jojoldu.batch.config.BatchJpaConfiguration.PACKAGE;
import static com.jojoldu.batch.config.DataSourceConfiguration.READER_DATASOURCE;

/**
 * Created by jojoldu@gmail.com on 24/05/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties({JpaProperties.class, HibernateProperties.class})
@EnableJpaRepositories(
        basePackages = PACKAGE,
        entityManagerFactoryRef = MASTER_ENTITY_MANAGER_FACTORY, // default와 같아서 생략도 가능
        transactionManagerRef = MASTER_TX_MANAGER
)
public class BatchJpaConfiguration {
    public static final String PACKAGE = "com.jojoldu.batch.entity";
    public static final String MASTER_ENTITY_MANAGER_FACTORY = "entityManagerFactory";
    public static final String READER_ENTITY_MANAGER_FACTORY = "readerEntityManagerFactory";

    public static final String MASTER_TX_MANAGER = "batchTransactionManager";

    private final JpaProperties jpaProperties;
    private final HibernateProperties hibernateProperties;
    private final ObjectProvider<Collection<DataSourcePoolMetadataProvider>> metadataProviders;
    private final EntityManagerFactoryBuilder entityManagerFactoryBuilder;

    @Primary
    @Bean(name = MASTER_ENTITY_MANAGER_FACTORY)
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource) {

        return EntityManagerFactoryCreator.builder()
                .properties(jpaProperties)
                .hibernateProperties(hibernateProperties)
                .metadataProviders(metadataProviders)
                .entityManagerFactoryBuilder(entityManagerFactoryBuilder)
                .dataSource(dataSource)
                .packages(PACKAGE)
                .persistenceUnit("master")
                .build()
                .create();
    }

    @Bean(name = READER_ENTITY_MANAGER_FACTORY)
    public LocalContainerEntityManagerFactoryBean readerEntityManagerFactory(
            @Qualifier(READER_DATASOURCE) DataSource dataSource) {

        return EntityManagerFactoryCreator.builder()
                .properties(jpaProperties)
                .hibernateProperties(hibernateProperties)
                .metadataProviders(metadataProviders)
                .entityManagerFactoryBuilder(entityManagerFactoryBuilder)
                .dataSource(dataSource)
                .packages(PACKAGE)
                .persistenceUnit("reader")
                .build()
                .create();
    }

    @Primary
    @Bean(name = MASTER_TX_MANAGER)
    public PlatformTransactionManager batchTransactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
    }
}
