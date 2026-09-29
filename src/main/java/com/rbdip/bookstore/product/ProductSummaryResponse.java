package com.rbdip.bookstore.product;


public record ProductSummaryResponse(Long id, String name, String price) {

    public static ProductSummaryResponse from(Product product) {
        String price = product.getPrice().toString();
        return new ProductSummaryResponse(product.getId(), product.getName(), price);
    }
}
