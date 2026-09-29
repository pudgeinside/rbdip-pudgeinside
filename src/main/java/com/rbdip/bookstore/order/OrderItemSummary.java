package com.rbdip.bookstore.order;


public record OrderItemSummary(String productName, int quantity) {

    public static OrderItemSummary from(OrderItem item) {
        return new OrderItemSummary(item.getProductName(), item.getQuantity());
    }
}
