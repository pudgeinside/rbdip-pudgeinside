package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Модуль расчёта цены заказа. Намеренно почти не покрыт тестами и
 * содержит magic numbers / нечитаемые ветвления скидок - цель для
 * характеризационных тестов (ЛР2) и mutation-testing гейта PIT (ЛР5).
 */
@Component
public class PricingCalculator {

    public record LineItem(BigDecimal price, int quantity) {
    }

    private static final String CUSTOMER_TYPE_VIP = "vip";
    private static final String CUSTOMER_TYPE_WHOLESALE = "wholesale";

    private static final String COUPON_SAVE10 = "SAVE10";
    private static final String COUPON_SAVE20PERCENT = "SAVE20PERCENT";

    private static final int BULK_QUANTITY_THRESHOLD = 10;
    private static final BigDecimal BULK_DISCOUNT_RATE = new BigDecimal("0.95");

    private static final BigDecimal VIP_DISCOUNT_RATE = new BigDecimal("0.90");
    private static final BigDecimal WHOLESALE_DISCOUNT_RATE = new BigDecimal("0.85");

    private static final BigDecimal SAVE10_AMOUNT = BigDecimal.TEN;
    private static final BigDecimal SAVE20PERCENT_RATE = new BigDecimal("0.80");

    private static final BigDecimal LARGE_ORDER_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal LARGE_ORDER_RATE = new BigDecimal("0.98");

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = sumLines(items);

        total = applyCustomerTypeDiscount(total, customerType);
        total = applyCoupon(total, couponCode);
        total = applyMinimumTotal(total);
        total = applyLargeOrderDiscount(total);

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumLines(List<LineItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem item : items) {
            total = total.add(lineTotal(item));
        }
        return total;
    }

    private BigDecimal lineTotal(LineItem item) {
        BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
        if (item.quantity() > BULK_QUANTITY_THRESHOLD) {
            linePrice = linePrice.multiply(BULK_DISCOUNT_RATE);
        }
        return linePrice;
    }

    private BigDecimal applyCustomerTypeDiscount(BigDecimal total, String customerType) {
        if (CUSTOMER_TYPE_VIP.equals(customerType)) {
            return total.multiply(VIP_DISCOUNT_RATE);
        }
        if (CUSTOMER_TYPE_WHOLESALE.equals(customerType)) {
            return total.multiply(WHOLESALE_DISCOUNT_RATE);
        }
        return total;
    }

    private BigDecimal applyCoupon(BigDecimal total, String couponCode) {
        if (COUPON_SAVE10.equals(couponCode)) {
            return total.subtract(SAVE10_AMOUNT);
        }
        if (COUPON_SAVE20PERCENT.equals(couponCode)) {
            return total.multiply(SAVE20PERCENT_RATE);
        }
        return total;
    }

    private BigDecimal applyMinimumTotal(BigDecimal total) {
        return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
    }

    private BigDecimal applyLargeOrderDiscount(BigDecimal total) {
        return total.compareTo(LARGE_ORDER_THRESHOLD) > 0 ? total.multiply(LARGE_ORDER_RATE) : total;
    }
}
