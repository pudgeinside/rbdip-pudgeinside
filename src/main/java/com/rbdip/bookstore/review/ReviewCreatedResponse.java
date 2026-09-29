package com.rbdip.bookstore.review;


public record ReviewCreatedResponse(Long id) {

    public static ReviewCreatedResponse from(Review review) {
        return new ReviewCreatedResponse(review.getId());
    }
}
