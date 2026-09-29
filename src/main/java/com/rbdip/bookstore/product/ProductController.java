package com.rbdip.bookstore.product;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCreatedResponse createProduct(@RequestBody CreateProductRequest request) {
        Product saved = productRepository.save(
                new Product(request.name(), request.price(), request.description()));
        return ProductCreatedResponse.from(saved);
    }

    @GetMapping("/products")
    public List<ProductSummaryResponse> listProducts() {
        return productRepository.findAll().stream()
                .map(ProductSummaryResponse::from)
                .toList();
    }
}
