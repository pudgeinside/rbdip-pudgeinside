package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingCalculatorCharacterizationTest {

    private final PricingCalculator calculator = new PricingCalculator();

    private BigDecimal total(String price, int quantity, String customerType, String couponCode) {
        return calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(new BigDecimal(price), quantity)),
                customerType, couponCode);
    }

    @Test
    void sumsLineItems() {
        assertThat(total("10.00", 2, "regular", null)).isEqualByComparingTo("20.00");
    }

    @Test
    void returnsZeroForEmptyOrder() {
        assertThat(calculator.calculateOrderTotal(List.of(), "regular", null))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void bulkDiscountAppliesOnlyAboveTenUnits() {
        assertThat(total("10.00", 10, "regular", null)).isEqualByComparingTo("100.00");
        assertThat(total("10.00", 11, "regular", null)).isEqualByComparingTo("104.50");
    }

    @Test
    void appliesVipDiscount() {
        assertThat(total("100.00", 1, "vip", null)).isEqualByComparingTo("90.00");
    }

    @Test
    void appliesWholesaleDiscount() {
        assertThat(total("100.00", 1, "wholesale", null)).isEqualByComparingTo("85.00");
    }

    @Test
    void ignoresUnknownCustomerType() {
        assertThat(total("100.00", 1, "gold", null)).isEqualByComparingTo("100.00");
    }

    @Test
    void subtractsFixedAmountCoupon() {
        assertThat(total("20.00", 1, "regular", "SAVE10")).isEqualByComparingTo("10.00");
    }

    @Test
    void clampsNegativeTotalToZero() {
        assertThat(total("5.00", 1, "regular", "SAVE10")).isEqualByComparingTo("0.00");
    }

    @Test
    void appliesPercentCoupon() {
        assertThat(total("100.00", 1, "regular", "SAVE20PERCENT")).isEqualByComparingTo("80.00");
    }

    @Test
    void ignoresUnknownCoupon() {
        assertThat(total("100.00", 1, "regular", "NOPE")).isEqualByComparingTo("100.00");
    }

    @Test
    void appliesLargeOrderDiscountAboveThreshold() {
        assertThat(total("500.00", 3, "regular", null)).isEqualByComparingTo("1470.00");
    }

    @Test
    void doesNotApplyLargeOrderDiscountAtThreshold() {
        assertThat(total("500.00", 2, "regular", null)).isEqualByComparingTo("1000.00");
    }

    @Test
    void appliesCustomerDiscountBeforeCoupon() {
        assertThat(total("100.00", 1, "vip", "SAVE20PERCENT")).isEqualByComparingTo("72.00");
    }

    @Test
    void appliesLargeOrderDiscountAfterCustomerDiscount() {
        assertThat(total("500.00", 3, "wholesale", null)).isEqualByComparingTo("1249.50");
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        assertThat(total("10.01", 1, "vip", null)).isEqualByComparingTo("9.01");
    }
}
