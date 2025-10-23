package com.hammer.ecommerce.controllers;

import com.hammer.ecommerce.dto.CreateReviewRequestDTO;
import com.hammer.ecommerce.dto.ProductRatingDTO;
import com.hammer.ecommerce.dto.ReviewResponseDTO;
import com.hammer.ecommerce.dto.UpdateReviewRequestDTO;
import com.hammer.ecommerce.repositories.UserRepository;
import com.hammer.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    // Os reviews são acessíveis via /api/products/{productId}/reviews,
    // que já está liberado como público para leitura. Para criar/atualizar/deletar, é necessário autenticação.

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody CreateReviewRequestDTO request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        ReviewResponseDTO review = reviewService.createReview(productId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @GetMapping
    public ResponseEntity<Page<ReviewResponseDTO>> findByProductId(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewResponseDTO> reviews = reviewService.findByProductId(productId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/rating")
    public ResponseEntity<ProductRatingDTO> getProductRating(@PathVariable Long productId) {
        ProductRatingDTO rating = reviewService.getProductRating(productId);
        return ResponseEntity.ok(rating);
    }

    @PutMapping
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateReviewRequestDTO request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        ReviewResponseDTO review = reviewService.updateReview(productId, userId, request);
        return ResponseEntity.ok(review);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long productId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        reviewService.deleteReview(productId, userId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"))
                .getId();
    }
}
