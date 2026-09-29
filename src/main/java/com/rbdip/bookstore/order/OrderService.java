package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * God-класс: валидация, расчёт цены, персистентность и "уведомление
 * клиента" смешаны в одном методе. Цель для рефакторинга по SRP в ЛР1.
 */
@Service
public class OrderService {

    private static final String DEFAULT_CUSTOMER_TYPE = "regular";

    private final OrderRequestValidator validator;
    private final OrderItemResolver itemResolver;
    private final PricingCalculator pricingCalculator;
    private final OrderWriter orderWriter;
    private final OrderConfirmationNotifier notifier;

    public OrderService(
            OrderRequestValidator validator,
            OrderItemResolver itemResolver,
            PricingCalculator pricingCalculator,
            OrderWriter orderWriter,
            OrderConfirmationNotifier notifier) {
        this.validator = validator;
        this.itemResolver = itemResolver;
        this.pricingCalculator = pricingCalculator;
        this.orderWriter = orderWriter;
        this.notifier = notifier;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        validator.validate(request);

        List<ResolvedOrderItem> lines = itemResolver.resolve(request.items());
        BigDecimal total = pricingCalculator.calculateOrderTotal(
                lines.stream().map(ResolvedOrderItem::pricingLineItem).toList(),
                customerTypeOrDefault(request.customerType()),
                request.couponCode());

        Order order = orderWriter.write(
                Order.newOrder(request.customerFullName(), request.customerAddress(), request.customerPhone()),
                lines);

        notifier.sendConfirmation(request.customerFullName(), order.getId(), total);

        return order;
    }

    private String customerTypeOrDefault(String customerType) {
        return customerType == null ? DEFAULT_CUSTOMER_TYPE : customerType;
    }
}
