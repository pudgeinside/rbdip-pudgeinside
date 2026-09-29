package com.rbdip.bookstore.review;


public record CreateReviewRequest(String authorName, Integer rating, String comment) {
}
