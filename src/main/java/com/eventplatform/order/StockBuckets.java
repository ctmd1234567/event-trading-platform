package com.eventplatform.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Component
public class StockBuckets {
    private final JdbcTemplate db;
    private final int bucketCount;

    public StockBuckets(JdbcTemplate db, @Value("${app.order.stock-bucket-count:16}") int bucketCount) {
        this.db = db;
        this.bucketCount = Math.max(1, bucketCount);
    }

    public int reserve(long userId, long voucherId) {
        int first = Math.floorMod(Long.hashCode(userId), bucketCount);
        for (int offset = 0; offset < bucketCount; offset++) {
            int bucket = (first + offset) % bucketCount;
            int changed = db.update("""
                UPDATE tb_seckill_voucher_bucket SET stock=stock-1
                WHERE voucher_id=? AND bucket_id=? AND stock>0
                AND EXISTS (SELECT 1 FROM tb_seckill_voucher s JOIN tb_voucher v ON v.id=s.voucher_id
                    WHERE s.voucher_id=? AND s.begin_time<=CURRENT_TIMESTAMP AND s.end_time>CURRENT_TIMESTAMP AND v.status=1)
                """, voucherId, bucket, voucherId);
            if (changed == 1) return bucket;
        }
        Integer present = db.queryForObject("SELECT COUNT(*) FROM tb_seckill_voucher WHERE voucher_id=?", Integer.class, voucherId);
        if (present == null || present == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found");
        throw new ResponseStatusException(HttpStatus.CONFLICT, "The sale is unavailable, ended, or out of stock");
    }

    public void initialize(long voucherId, int stock) {
        List<Object[]> rows = new ArrayList<>(bucketCount);
        int base = stock / bucketCount;
        int remainder = stock % bucketCount;
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            rows.add(new Object[]{voucherId, bucket, base + (bucket < remainder ? 1 : 0)});
        }
        db.batchUpdate("INSERT INTO tb_seckill_voucher_bucket(voucher_id,bucket_id,stock) VALUES (?,?,?)", rows);
    }
}
