package com.eventplatform.order;

import com.eventplatform.config.QueueConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name="app.outbox.enabled", havingValue="true", matchIfMissing=true)
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final JdbcTemplate db;
    private final RabbitTemplate rabbit;
    public OutboxPublisher(JdbcTemplate db, RabbitTemplate rabbit) { this.db=db; this.rabbit=rabbit; }

    @Scheduled(fixedDelayString="${app.outbox.interval-ms:1000}")
    public void publish() {
        // Reconcile until the consumer commits, even after broker confirmation. Duplicate delivery is safe.
        var ids = db.queryForList("SELECT id FROM tb_outbox_event WHERE completed=FALSE AND next_attempt<=CURRENT_TIMESTAMP ORDER BY next_attempt LIMIT 50", Long.class);
        for (Long id : ids) {
            // A database lease allows multiple app instances without holding a transaction during network I/O.
            if (db.update("UPDATE tb_outbox_event SET next_attempt=?,attempts=attempts+1 WHERE id=? AND completed=FALSE AND next_attempt<=CURRENT_TIMESTAMP",
                    Timestamp.from(Instant.now().plusSeconds(60)),id) != 1) continue;
            var correlation = new CorrelationData(UUID.randomUUID().toString());
            try {
                rabbit.convertAndSend(QueueConfig.EXCHANGE,QueueConfig.ROUTING_KEY,id.toString(),message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setMessageId(id.toString());
                    return message;
                },correlation);
                var confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck() || correlation.getReturned()!=null) throw new IllegalStateException("Broker rejected or returned order event");
                db.update("UPDATE tb_outbox_event SET last_error=NULL WHERE id=?",id);
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                db.update("UPDATE tb_outbox_event SET last_error='Publish failed; scheduled retry' WHERE id=?",id);
                log.warn("Order event {} pending; automatic retry in 60 seconds ({})",id,ex.getClass().getSimpleName());
            }
        }
        Integer stale = db.queryForObject("SELECT COUNT(*) FROM tb_outbox_event WHERE completed=FALSE AND attempts>=5",Integer.class);
        if (stale != null && stale > 0) log.warn("{} order events require attention; inspect tb_outbox_event and the failed queue",stale);
    }
}
