package com.rbdip.bookstore.product;

import java.math.BigDecimal;


public record CreateProductRequest(String name, BigDecimal price, String description) {
}
