package com.rbdip.bookstore.order;

import java.util.List;


public record OrderSummaryResponse(
        Long id, String customerFullName, String status, List<OrderItemSummary> items) {

    public static OrderSummaryResponse from(Order order, List<OrderItem> items) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getCustomerFullName(),
                order.getStatus(),
                items.stream().map(OrderItemSummary::from).toList());
    }
}
