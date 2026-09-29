package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;


@Component
public class OrderItemResolver {

    private static final int DEFAULT_QUANTITY = 1;

    private final ProductRepository productRepository;

    public OrderItemResolver(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ResolvedOrderItem> resolve(List<CreateOrderRequest.Item> items) {
        List<ResolvedOrderItem> resolved = new ArrayList<>(items.size());
        for (CreateOrderRequest.Item item : items) {
            resolved.add(resolveOne(item));
        }
        return resolved;
    }

    private ResolvedOrderItem resolveOne(CreateOrderRequest.Item item) {
        Product product = findProduct(item.productId());
        return new ResolvedOrderItem(product, resolveQuantity(item.quantity()));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("product " + productId + " not found"));
    }

    private int resolveQuantity(Integer requested) {
        int quantity = requested == null ? DEFAULT_QUANTITY : requested;
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return quantity;
    }
}
