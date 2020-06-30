package com.jojoldu.batch.example.performancewrite.test3;

import com.jojoldu.batch.entity.product.backup.StoreBackup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by jojoldu@gmail.com on 30/06/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@Slf4j
@RequiredArgsConstructor
@Repository
public class BulkInsertRepository {
    private final JdbcTemplate jdbcTemplate;

    public void saveAll(List<StoreBackup> items) {
        String sql = "insert into store_backup(origin_id, name) values (?, ?)";

        int[] batchUpdate = jdbcTemplate.batchUpdate(sql,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setLong(1, items.get(i).getOriginId());
                        ps.setString(2, items.get(i).getName());
                    }

                    @Override
                    public int getBatchSize() {
                        return items.size();
                    }
                });

        log.info("batchCount: " + batchUpdate[0]);
    }

}
