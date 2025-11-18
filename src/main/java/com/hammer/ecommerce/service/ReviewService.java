package com.hammer.ecommerce.service;

import com.hammer.ecommerce.dto.review.CreateReviewRequestDTO;
import com.hammer.ecommerce.dto.product.ProductRatingDTO;
import com.hammer.ecommerce.dto.review.ReviewResponseDTO;
import com.hammer.ecommerce.dto.review.UpdateReviewRequestDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.OrderStatus;
import com.hammer.ecommerce.model.Product;
import com.hammer.ecommerce.model.Review;
import com.hammer.ecommerce.model.User;
import com.hammer.ecommerce.repositories.OrderRepository;
import com.hammer.ecommerce.repositories.ProductRepository;
import com.hammer.ecommerce.repositories.ReviewRepository;
import com.hammer.ecommerce.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public ReviewResponseDTO createReview(Long productId, Long userId, CreateReviewRequestDTO request) {

        // Buscar produto
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));

        // Buscar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Verificar se usuário já avaliou este produto
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new BusinessException("Você já avaliou este produto");
        }

        // Verificar se usuário comprou o produto (opcional - pode comentar se não quiser essa regra)
        boolean hasPurchased = hasUserPurchasedProduct(userId, productId);
        if (!hasPurchased) {
            throw new BusinessException("Você só pode avaliar produtos que comprou");
        }

        // Criar review
        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        review = reviewRepository.save(review);
        return convertToDTO(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> findByProductId(Long productId, Pageable pageable) {

        // Verificar se produto existe
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Produto não encontrado");
        }

        return reviewRepository.findByProductId(productId, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public ProductRatingDTO getProductRating(Long productId) {

        // Verificar se produto existe
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Produto não encontrado");
        }

        Double averageRating = reviewRepository.findAverageRatingByProductId(productId);
        Long totalReviews = reviewRepository.countByProductId(productId);

        return new ProductRatingDTO(productId, averageRating != null ? averageRating : 0.0, totalReviews);
    }

    @Transactional
    public ReviewResponseDTO updateReview(Long productId, Long userId, UpdateReviewRequestDTO request) {
        Review review = reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada"));

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        review = reviewRepository.save(review);
        return convertToDTO(review);
    }

    @Transactional
    public void deleteReview(Long productId, Long userId) {
        if (!reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new ResourceNotFoundException("Avaliação não encontrada");
        }

        reviewRepository.deleteByProductIdAndUserId(productId, userId);
    }

    // Método auxiliar para verificar se usuário comprou o produto
    private boolean hasUserPurchasedProduct(Long userId, Long productId) {

        // Buscar pedidos do usuário com status DELIVERED ou PAID
        return orderRepository.findByUserId(userId, Pageable.unpaged())
                .stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED ||
                        order.getStatus() == OrderStatus.PAID ||
                        order.getStatus() == OrderStatus.SHIPPED)
                .flatMap(order -> order.getOrderItems().stream())
                .anyMatch(item -> item.getProduct().getId().equals(productId));
    }

    private ReviewResponseDTO convertToDTO(Review review) {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(review.getId());
        dto.setProductId(review.getProduct().getId());
        dto.setProductName(review.getProduct().getName());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
