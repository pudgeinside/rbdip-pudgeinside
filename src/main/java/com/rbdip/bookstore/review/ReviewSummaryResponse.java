package com.rbdip.bookstore.review;


public record ReviewSummaryResponse(String authorName, Integer rating, String comment) {

    public static ReviewSummaryResponse from(Review review) {
        return new ReviewSummaryResponse(
                review.getAuthorName(), review.getRating(), review.getComment() == null ? "" : review.getComment());
    }
}
