package com.eventplatform;

import com.eventplatform.order.OrderTransactions;
import com.eventplatform.security.AuthCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import javax.sql.DataSource;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

@Testcontainers
@ActiveProfiles("local")
@SpringBootTest
class InfrastructureIT {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("event_trading");
    @Container static GenericContainer<?> redis=new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);
    @Container static RabbitMQContainer rabbit=new RabbitMQContainer("rabbitmq:4.1-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",mysql::getJdbcUrl);
        r.add("spring.datasource.username",mysql::getUsername);
        r.add("spring.datasource.password",mysql::getPassword);
        r.add("spring.data.redis.host",redis::getHost);
        r.add("spring.data.redis.port",() -> redis.getMappedPort(6379));
        r.add("spring.rabbitmq.host",rabbit::getHost);
        r.add("spring.rabbitmq.port",rabbit::getAmqpPort);
        r.add("spring.rabbitmq.username",rabbit::getAdminUsername);
        r.add("spring.rabbitmq.password",rabbit::getAdminPassword);
    }
    @Autowired DataSource source;
    @Autowired JdbcTemplate db;
    @Autowired AuthCodes codes;
    @Autowired OrderTransactions orders;
    @Test void realLuaMigrationAndBrokerRoundTrip() {
        new ResourceDatabasePopulator(new ClassPathResource("db/event_trading.sql"),new ClassPathResource("db/security-upgrade.sql")).execute(source);
        String phone="13900000001";
        Map<?,?> data=(Map<?,?>)codes.send(phone).getData();
        String code=data.get("developmentCode").toString();
        assertThat(codes.consume(phone,code)).isTrue();
        assertThat(codes.consume(phone,code)).isFalse();
        db.update("INSERT INTO tb_voucher(id,shop_id,title,pay_value,actual_value,type,status) VALUES(900001,1,'integration',1,2,1,1)");
        db.update("INSERT INTO tb_seckill_voucher(voucher_id,stock,begin_time,end_time) VALUES(900001,2,DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 1 DAY))");
        long id=orders.reserve(900001,900001);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(orders.status(id,900001)).containsEntry("state","COMPLETED"));
        orders.fulfill(id);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM tb_voucher_order WHERE id=?",Integer.class,id)).isEqualTo(1);
        assertThat(db.queryForObject("SELECT stock FROM tb_seckill_voucher WHERE voucher_id=900001",Integer.class)).isEqualTo(1);
    }
}
