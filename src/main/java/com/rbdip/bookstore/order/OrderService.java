package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
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
    private final CustomerRepository customerRepository;

    public OrderService(
            OrderRequestValidator validator,
            OrderItemResolver itemResolver,
            PricingCalculator pricingCalculator,
            OrderWriter orderWriter,
            OrderConfirmationNotifier notifier,
            CustomerRepository customerRepository) {
        this.validator = validator;
        this.itemResolver = itemResolver;
        this.pricingCalculator = pricingCalculator;
        this.orderWriter = orderWriter;
        this.notifier = notifier;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        validator.validate(request);

        List<ResolvedOrderItem> lines = itemResolver.resolve(request.items());
        BigDecimal total = pricingCalculator.calculateOrderTotal(
                lines.stream().map(ResolvedOrderItem::pricingLineItem).toList(),
                customerTypeOrDefault(request.customerType()),
                request.couponCode());

        Customer customer = resolveCustomer(
                request.customerFullName(), request.customerAddress(), request.customerPhone());

        Order order = orderWriter.write(Order.newOrder(customer), lines);

        notifier.sendConfirmation(request.customerFullName(), order.getId(), total);

        return order;
    }

    private Customer resolveCustomer(String fullName, String address, String phone) {
        CustomerName name = CustomerName.parse(fullName);
        return customerRepository.findByFirstNameAndLastName(name.firstName(), name.lastName()).stream()
                .filter(candidate -> Objects.equals(candidate.getAddress(), address)
                        && Objects.equals(candidate.getPhone(), phone))
                .findFirst()
                .orElseGet(() -> customerRepository.save(
                        Customer.of(name.firstName(), name.lastName(), address, phone)));
    }

    private String customerTypeOrDefault(String customerType) {
        return customerType == null ? DEFAULT_CUSTOMER_TYPE : customerType;
    }
}
