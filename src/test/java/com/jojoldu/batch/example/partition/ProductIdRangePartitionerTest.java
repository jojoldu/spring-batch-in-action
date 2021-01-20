package com.jojoldu.batch.example.partition;

import com.jojoldu.batch.entity.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Created by jojoldu@gmail.com on 20/01/2021
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@ExtendWith(MockitoExtension.class)
public class ProductIdRangePartitionerTest {
    private static ProductIdRangePartitioner partitioner;

    @Mock
    private ProductRepository productRepository;

    @Test
    void gridSize에_맞게_id가_분할된다() throws Exception {
        //given
        Mockito.lenient()
                .when(productRepository.findMinId(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(1L);

        Mockito.lenient()
                .when(productRepository.findMaxId(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(10L);

        partitioner = new ProductIdRangePartitioner(productRepository, LocalDate.of(2021,1,20), LocalDate.of(2021,1,21));

        //when
        Map<String, ExecutionContext> executionContextMap = partitioner.partition(5);

        //then
        ExecutionContext partition1 = executionContextMap.get("partition0");
        assertThat(partition1.getLong("minId")).isEqualTo(1L);
        assertThat(partition1.getLong("maxId")).isEqualTo(2L);

        ExecutionContext partition5 = executionContextMap.get("partition4");
        assertThat(partition5.getLong("minId")).isEqualTo(9L);
        assertThat(partition5.getLong("maxId")).isEqualTo(10L);
    }
}
