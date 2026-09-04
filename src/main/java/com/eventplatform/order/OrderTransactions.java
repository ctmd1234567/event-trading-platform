package com.eventplatform.order;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class OrderTransactions {
    private final JdbcTemplate db;
    public OrderTransactions(JdbcTemplate db) { this.db = db; }

    /** A durable reservation, inventory change and outgoing event share one database transaction. */
    @Transactional
    public long reserve(long userId, long voucherId) {
        // Serialize reservations for a voucher; the database remains the inventory authority.
        var stocks = db.queryForList("SELECT stock, begin_time, end_time FROM tb_seckill_voucher WHERE voucher_id=? FOR UPDATE", voucherId);
        if (stocks.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found");
        List<Long> existing = db.queryForList("SELECT id FROM tb_order_request WHERE user_id=? AND voucher_id=?", Long.class, userId, voucherId);
        if (!existing.isEmpty()) return existing.getFirst();
        existing = db.queryForList("SELECT id FROM tb_voucher_order WHERE user_id=? AND voucher_id=?", Long.class, userId, voucherId);
        if (!existing.isEmpty()) return existing.getFirst();
        int changed = db.update("""
            UPDATE tb_seckill_voucher SET stock=stock-1
            WHERE voucher_id=? AND stock>0 AND begin_time<=CURRENT_TIMESTAMP AND end_time>CURRENT_TIMESTAMP
            AND EXISTS (SELECT 1 FROM tb_voucher WHERE id=? AND status=1)
            """, voucherId, voucherId);
        if (changed != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "The sale is unavailable, ended, or out of stock");
        long id = IdWorker.getId();
        db.update("INSERT INTO tb_order_request(id,user_id,voucher_id,state) VALUES (?,?,?,'PENDING')", id,userId,voucherId);
        db.update("INSERT INTO tb_outbox_event(id,next_attempt) VALUES (?,CURRENT_TIMESTAMP)", id);
        return id;
    }

    /** Lock and state check make redelivery a no-op; commit before acknowledging the message. */
    @Transactional
    public void fulfill(long id) {
        var requests = db.queryForList("SELECT user_id,voucher_id,state FROM tb_order_request WHERE id=? FOR UPDATE", id);
        if (requests.isEmpty()) throw new IllegalArgumentException("Unknown order request");
        Map<String,Object> request = requests.getFirst();
        if ("COMPLETED".equals(request.get("state"))) return;
        long user = ((Number) request.get("user_id")).longValue();
        long voucher = ((Number) request.get("voucher_id")).longValue();
        db.update("INSERT INTO tb_voucher_order(id,user_id,voucher_id) VALUES (?,?,?)", id,user,voucher);
        db.update("UPDATE tb_order_request SET state='COMPLETED',updated_at=CURRENT_TIMESTAMP WHERE id=?", id);
        db.update("UPDATE tb_outbox_event SET completed=TRUE WHERE id=?", id);
    }

    public Map<String,Object> status(long id, long userId) {
        var rows = db.queryForList("SELECT id,state,created_at,updated_at FROM tb_order_request WHERE id=? AND user_id=?", id,userId);
        if (!rows.isEmpty()) return rows.getFirst();
        var orders = db.queryForList("SELECT id FROM tb_voucher_order WHERE id=? AND user_id=?", id,userId);
        if (!orders.isEmpty()) return Map.of("id",id,"state","COMPLETED");
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found");
    }
}
