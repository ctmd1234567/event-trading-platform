package com.eventplatform.listener;

import com.eventplatform.config.QueueConfig;
import com.eventplatform.order.OrderTransactions;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SeckillVoucherListener {
    private final OrderTransactions orders;
    public SeckillVoucherListener(OrderTransactions orders) { this.orders = orders; }
    @RabbitListener(queues = QueueConfig.QUEUE)
    public void receive(String orderId) {
        orders.fulfill(Long.parseLong(orderId));
        // Spring AUTO acknowledgement happens after the proxied transactional method has committed.
    }
}
