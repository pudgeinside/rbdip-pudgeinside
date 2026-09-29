package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;


@Component
public class OrderWriter {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderWriter(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Order write(Order order, List<ResolvedOrderItem> lines) {
        Order saved = orderRepository.save(order);
        for (ResolvedOrderItem line : lines) {
            orderItemRepository.save(line.toOrderItem(saved.getId()));
        }
        return saved;
    }
}
