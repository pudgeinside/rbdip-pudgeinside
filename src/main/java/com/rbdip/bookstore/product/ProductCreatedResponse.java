package com.rbdip.bookstore.product;


public record ProductCreatedResponse(Long id, String name) {

    public static ProductCreatedResponse from(Product product) {
        return new ProductCreatedResponse(product.getId(), product.getName());
    }
}
