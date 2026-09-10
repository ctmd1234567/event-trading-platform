package com.eventplatform;

import com.eventplatform.order.OutboxPublisher;
import com.eventplatform.config.QueueConfig;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.core.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.Mockito.*;

class OutboxPublisherTest {
    @Test void brokerOutageLeavesEventPendingForRetry() {
        var db=mock(JdbcTemplate.class); var rabbit=mock(RabbitTemplate.class);
        when(db.update(contains("lease_owner=?"),anyString(),any(Timestamp.class))).thenReturn(1);
        when(db.queryForList(contains("lease_owner=?"),eq(Long.class),anyString())).thenReturn(List.of(42L));
        doThrow(new org.springframework.amqp.AmqpException("offline")).when(rabbit)
            .convertAndSend(eq(QueueConfig.EXCHANGE),eq(QueueConfig.ROUTING_KEY),eq("42"),any(MessagePostProcessor.class),any(CorrelationData.class));
        new OutboxPublisher(db,rabbit,50,60,5).publish();
        verify(db).update(contains("last_error='Publish failed"),eq(42L));
        verify(db,never()).update(contains("completed=TRUE"),eq(42L));
    }
    @Test void publisherConfirmationStillWaitsForConsumerCompletion() {
        var db=mock(JdbcTemplate.class); var rabbit=mock(RabbitTemplate.class);
        when(db.update(contains("lease_owner=?"),anyString(),any(Timestamp.class))).thenReturn(1);
        when(db.queryForList(contains("lease_owner=?"),eq(Long.class),anyString())).thenReturn(List.of(42L));
        doAnswer(call -> { CorrelationData data=call.getArgument(4); data.getFuture().complete(new CorrelationData.Confirm(true,null)); return null; })
            .when(rabbit).convertAndSend(eq(QueueConfig.EXCHANGE),eq(QueueConfig.ROUTING_KEY),eq("42"),any(MessagePostProcessor.class),any(CorrelationData.class));
        new OutboxPublisher(db,rabbit,50,60,5).publish();
        verify(db).update(contains("last_error=NULL"),anyString(),eq(42L));
        verify(db,never()).update(contains("completed=TRUE"),eq(42L));
    }

    @Test void sendsWholeBatchBeforeWaitingForPublisherConfirms() {
        var db=mock(JdbcTemplate.class); var rabbit=mock(RabbitTemplate.class);
        when(db.update(contains("lease_owner=?"),anyString(),any(Timestamp.class))).thenReturn(2);
        when(db.queryForList(contains("lease_owner=?"),eq(Long.class),anyString())).thenReturn(List.of(41L,42L));
        List<CorrelationData> correlations = new ArrayList<>();
        AtomicInteger sends = new AtomicInteger();
        doAnswer(call -> {
            CorrelationData data=call.getArgument(4);
            correlations.add(data);
            if (sends.incrementAndGet() == 2) {
                correlations.forEach(item -> item.getFuture().complete(new CorrelationData.Confirm(true,null)));
            }
            return null;
        }).when(rabbit).convertAndSend(eq(QueueConfig.EXCHANGE),eq(QueueConfig.ROUTING_KEY),anyString(),any(MessagePostProcessor.class),any(CorrelationData.class));

        new OutboxPublisher(db,rabbit,200,60,1).publish();

        verify(db).update(contains("LIMIT 200"),anyString(),any(Timestamp.class));
        verify(rabbit,times(2)).convertAndSend(eq(QueueConfig.EXCHANGE),eq(QueueConfig.ROUTING_KEY),anyString(),any(MessagePostProcessor.class),any(CorrelationData.class));
        verify(db).update(contains("last_error=NULL"),anyString(),eq(41L),eq(42L));
    }
}
