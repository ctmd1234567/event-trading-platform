package com.eventplatform;

import com.eventplatform.order.OutboxPublisher;
import com.eventplatform.config.QueueConfig;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.core.*;
import java.sql.Timestamp;
import java.util.List;
import static org.mockito.Mockito.*;

class OutboxPublisherTest {
    @Test void brokerOutageLeavesEventPendingForRetry() {
        var db=mock(JdbcTemplate.class); var rabbit=mock(RabbitTemplate.class);
        when(db.queryForList(anyString(),eq(Long.class))).thenReturn(List.of(42L));
        when(db.update(anyString(),any(Timestamp.class),eq(42L))).thenReturn(1);
        doThrow(new org.springframework.amqp.AmqpException("offline")).when(rabbit)
            .convertAndSend(eq(QueueConfig.EXCHANGE),eq(QueueConfig.ROUTING_KEY),eq("42"),any(MessagePostProcessor.class),any(CorrelationData.class));
        new OutboxPublisher(db,rabbit).publish();
        verify(db).update(contains("last_error='Publish failed"),eq(42L));
        verify(db,never()).update(contains("completed=TRUE"),eq(42L));
    }
    @Test void publisherConfirmationStillWaitsForConsumerCompletion() {
        var db=mock(JdbcTemplate.class); var rabbit=mock(RabbitTemplate.class);
        when(db.queryForList(anyString(),eq(Long.class))).thenReturn(List.of(42L));
        when(db.update(anyString(),any(Timestamp.class),eq(42L))).thenReturn(1);
        doAnswer(call -> { CorrelationData data=call.getArgument(4); data.getFuture().complete(new CorrelationData.Confirm(true,null)); return null; })
            .when(rabbit).convertAndSend(eq(QueueConfig.EXCHANGE),eq(QueueConfig.ROUTING_KEY),eq("42"),any(MessagePostProcessor.class),any(CorrelationData.class));
        new OutboxPublisher(db,rabbit).publish();
        verify(db).update("UPDATE tb_outbox_event SET last_error=NULL WHERE id=?",42L);
        verify(db,never()).update(contains("completed=TRUE"),eq(42L));
    }
}
