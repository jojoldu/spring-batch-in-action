package com.jojoldu.batch.querydsl.reader;

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.reader.jpa.StoreRepository;
import com.jojoldu.batch.reader.jpa.Product;
import com.jojoldu.batch.reader.jpa.ProductRepository;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDate;

import static com.jojoldu.batch.reader.jpa.QProduct.product;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestBatchConfig.class)
public class QuerydslNoOffsetPagingItemReaderTest {

    @Autowired
    private JPAQueryFactory queryFactory;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManagerFactory emf;

    @After
    public void tearDown() throws Exception {
        productRepository.deleteAll();
        storeRepository.deleteAll();
    }

    @Test
    public void 쿼리생성후_체이닝여부_확인() {
        //given
        LocalDate startDate = LocalDate.of(2020,1,11);
        LocalDate endDate = LocalDate.of(2020,1,11);
        JPAQuery<Product> query = queryFactory
                .selectFrom(product)
                .where(product.createDate.between(startDate, endDate))
                .orderBy(product.createDate.asc());

        NumberPath<Long> id = product.id;
        BooleanExpression where = id.gt(1);
        OrderSpecifier<Long> order = id.asc();

        //when
        query.where(where).orderBy(order);

        //then
        assertThat(query.toString()).contains("product.id >");
        assertThat(query.toString()).contains("product.id asc");
    }

    @Test
    public void reader가_정상적으로_값을반환한다() throws Exception {
        //given
        LocalDate txDate = LocalDate.of(2020,10,12);
        String name = "a";
        int expected1 = 1000;
        int expected2 = 2000;
        productRepository.save(new Product(name, expected1, txDate));
        productRepository.save(new Product(name, expected2, txDate));

        QuerydslNoOffsetOptions options = new QuerydslNoOffsetOptions(product.id, QuerydslNoOffsetOptions.Expression.ASC);

        int chunkSize = 1;

        QuerydslNoOffsetPagingItemReader<Product> reader = new QuerydslNoOffsetPagingItemReader<>(emf, chunkSize, options, queryFactory -> queryFactory
                        .selectFrom(product)
                        .where(product.createDate.eq(txDate)));

        reader.open(new ExecutionContext());

        //when
        Product read1 = reader.read();
        Product read2 = reader.read();
        Product read3 = reader.read();

        //then
        assertThat(read1.getPrice()).isEqualTo(1000L);
        assertThat(read2.getPrice()).isEqualTo(2000L);
        assertThat(read3).isNull();
    }

}
