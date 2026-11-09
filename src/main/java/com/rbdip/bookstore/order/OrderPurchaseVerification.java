package com.rbdip.bookstore.order;

import com.rbdip.bookstore.contract.PurchaseVerification;
import org.springframework.stereotype.Component;

@Component
public class OrderPurchaseVerification implements PurchaseVerification {

    private final OrderItemRepository orderItemRepository;

    public OrderPurchaseVerification(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public boolean hasPurchased(Long productId) {
        return orderItemRepository.existsByProductId(productId);
    }
}
