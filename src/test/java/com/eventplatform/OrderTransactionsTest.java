package com.eventplatform;

import com.eventplatform.order.OrderTransactions;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringJUnitConfig(OrderTransactionsTest.Config.class)
class OrderTransactionsTest {
    @Configuration @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:orders;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","sa",""); }
        @Bean JdbcTemplate db(DataSource source) { return new JdbcTemplate(source); }
        @Bean PlatformTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean OrderTransactions orders(JdbcTemplate db) { return new OrderTransactions(db); }
    }
    @org.springframework.beans.factory.annotation.Autowired OrderTransactions orders;
    @org.springframework.beans.factory.annotation.Autowired JdbcTemplate db;
    @BeforeEach void schema() {
        db.execute("DROP ALL OBJECTS");
        db.execute("CREATE TABLE tb_voucher(id BIGINT PRIMARY KEY,status INT)");
        db.execute("CREATE TABLE tb_seckill_voucher(voucher_id BIGINT PRIMARY KEY,stock INT,begin_time TIMESTAMP,end_time TIMESTAMP)");
        db.execute("CREATE TABLE tb_voucher_order(id BIGINT PRIMARY KEY,user_id BIGINT,voucher_id BIGINT,CONSTRAINT uk_order UNIQUE(user_id,voucher_id))");
        db.execute("CREATE TABLE tb_order_request(id BIGINT PRIMARY KEY,user_id BIGINT,voucher_id BIGINT,state VARCHAR(16),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(user_id,voucher_id))");
        db.execute("CREATE TABLE tb_outbox_event(id BIGINT PRIMARY KEY,completed BOOLEAN DEFAULT FALSE,next_attempt TIMESTAMP,attempts INT DEFAULT 0,last_error VARCHAR(255))");
        db.update("INSERT INTO tb_voucher VALUES(1,1)");
        db.update("INSERT INTO tb_seckill_voucher VALUES(1,5,DATEADD('DAY',-1,CURRENT_TIMESTAMP),DATEADD('DAY',1,CURRENT_TIMESTAMP))");
    }
    int stock() { return db.queryForObject("SELECT stock FROM tb_seckill_voucher WHERE voucher_id=1",Integer.class); }
    @Test void reservationAndRedeliveryAreIdempotent() {
        long id=orders.reserve(7,1);
        assertThat(orders.reserve(7,1)).isEqualTo(id);
        assertThat(stock()).isEqualTo(4);
        orders.fulfill(id); orders.fulfill(id);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM tb_voucher_order",Integer.class)).isEqualTo(1);
        assertThat(orders.status(id,7)).containsEntry("state","COMPLETED");
        assertThatThrownBy(() -> orders.status(id,8)).isInstanceOf(ResponseStatusException.class);
        assertThat(db.queryForObject("SELECT completed FROM tb_outbox_event WHERE id=?",Boolean.class,id)).isTrue();
    }
    @Test void outboxFailureRollsBackInventoryAndRequest() {
        db.execute("DROP TABLE tb_outbox_event");
        assertThatThrownBy(() -> orders.reserve(7,1)).isInstanceOf(RuntimeException.class);
        assertThat(stock()).isEqualTo(5);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM tb_order_request",Integer.class)).isZero();
    }
    @Test void consumerFailureRollsBackOrderAndCanRetry() {
        long id=orders.reserve(7,1);
        db.execute("ALTER TABLE tb_order_request ADD CONSTRAINT only_pending CHECK(state='PENDING')");
        assertThatThrownBy(() -> orders.fulfill(id)).isInstanceOf(RuntimeException.class);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM tb_voucher_order",Integer.class)).isZero();
        db.execute("ALTER TABLE tb_order_request DROP CONSTRAINT only_pending");
        orders.fulfill(id);
        assertThat(orders.status(id,7)).containsEntry("state","COMPLETED");
    }
    @Test void rejectsClosedFutureAndDisabledVouchers() {
        db.update("UPDATE tb_seckill_voucher SET begin_time=DATEADD('DAY',1,CURRENT_TIMESTAMP)");
        assertThatThrownBy(() -> orders.reserve(7,1)).isInstanceOf(ResponseStatusException.class);
        db.update("UPDATE tb_seckill_voucher SET begin_time=DATEADD('DAY',-2,CURRENT_TIMESTAMP),end_time=DATEADD('DAY',-1,CURRENT_TIMESTAMP)");
        assertThatThrownBy(() -> orders.reserve(7,1)).isInstanceOf(ResponseStatusException.class);
        db.update("UPDATE tb_seckill_voucher SET end_time=DATEADD('DAY',1,CURRENT_TIMESTAMP)");
        db.update("UPDATE tb_voucher SET status=2");
        assertThatThrownBy(() -> orders.reserve(7,1)).isInstanceOf(ResponseStatusException.class);
        assertThat(stock()).isEqualTo(5);
    }
    @Test void concurrentBuyersCannotOversell() throws Exception {
        try(var pool=Executors.newFixedThreadPool(10)) {
            List<Future<Long>> futures=new ArrayList<>();
            for(int i=0;i<20;i++) { final int user=i+1; futures.add(pool.submit(() -> {
                try { return orders.reserve(user,1); } catch(ResponseStatusException soldOut) { return null; }
            })); }
            List<Long> accepted=new ArrayList<>();
            for(var future:futures) { Long id=future.get(15,TimeUnit.SECONDS); if(id!=null) accepted.add(id); }
            assertThat(accepted).hasSize(5).doesNotHaveDuplicates();
            assertThat(stock()).isZero();
        }
    }
    @Test void concurrentSameUserConsumesOneStock() throws Exception {
        try(var pool=Executors.newFixedThreadPool(8)) {
            List<Future<Long>> futures=new ArrayList<>();
            for(int i=0;i<8;i++) futures.add(pool.submit(() -> orders.reserve(7,1)));
            Set<Long> ids=new HashSet<>();
            for(var f:futures) ids.add(f.get(15,TimeUnit.SECONDS));
            assertThat(ids).hasSize(1);
            assertThat(stock()).isEqualTo(4);
        }
    }
}
