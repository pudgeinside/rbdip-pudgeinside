package com.rbdip.bookstore.review;

import com.rbdip.bookstore.contract.PurchaseVerification;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final PurchaseVerification purchaseVerification;

    public ReviewService(ReviewRepository reviewRepository, PurchaseVerification purchaseVerification) {
        this.reviewRepository = reviewRepository;
        this.purchaseVerification = purchaseVerification;
    }

    public Review addReview(Long productId, String authorName, Integer rating, String comment) {
        boolean verifiedPurchase = purchaseVerification.hasPurchased(productId);
        log.debug("Adding review for product {} (verified purchase: {})", productId, verifiedPurchase);
        Review review = new Review(productId, authorName == null ? "anonymous" : authorName, rating, comment);
        return reviewRepository.save(review);
    }

    public List<Review> listReviews(Long productId) {
        return reviewRepository.findByProductId(productId);
    }
}
