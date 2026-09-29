package com.rbdip.bookstore.review;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/products/{productId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewCreatedResponse addReview(
            @PathVariable Long productId, @RequestBody CreateReviewRequest request) {
        Review review = reviewService.addReview(productId, request.authorName(), request.rating(), request.comment());
        return ReviewCreatedResponse.from(review);
    }

    @GetMapping("/products/{productId}/reviews")
    public List<ReviewSummaryResponse> listReviews(@PathVariable Long productId) {
        return reviewService.listReviews(productId).stream()
                .map(ReviewSummaryResponse::from)
                .toList();
    }
}
