package com.hammer.ecommerce;

import com.hammer.ecommerce.dto.CreateReviewRequestDTO;
import com.hammer.ecommerce.dto.ProductRatingDTO;
import com.hammer.ecommerce.dto.ReviewResponseDTO;
import com.hammer.ecommerce.dto.UpdateReviewRequestDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.*;
import com.hammer.ecommerce.repositories.*;
import com.hammer.ecommerce.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Product product;
    private Review review;
    private Order order;
    private OrderItem orderItem;
    private CreateReviewRequestDTO createReviewRequest;
    private UpdateReviewRequestDTO updateReviewRequest;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setName("João Silva");

        Category category = new Category();
        category.setId(1L);

        product = new Product();
        product.setId(1L);
        product.setName("Mouse Gamer");
        product.setPrice(new BigDecimal("150.00"));
        product.setCategory(category);

        review = new Review();
        review.setId(1L);
        review.setProduct(product);
        review.setUser(user);
        review.setRating(5);
        review.setComment("Excelente produto!");

        order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setStatus(OrderStatus.DELIVERED);

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setProduct(product);
        orderItem.setOrder(order);
        orderItem.setQuantity(1);

        order.getOrderItems().add(orderItem);

        createReviewRequest = new CreateReviewRequestDTO();
        createReviewRequest.setRating(5);
        createReviewRequest.setComment("Excelente produto!");

        updateReviewRequest = new UpdateReviewRequestDTO();
        updateReviewRequest.setRating(4);
        updateReviewRequest.setComment("Bom produto!");
    }

    @Test
    @DisplayName("Deve criar avaliação com sucesso")
    void testCreateReview_Success() {

        // Arrange
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(order));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reviewRepository.existsByProductIdAndUserId(1L, 1L)).thenReturn(false);
        when(orderRepository.findByUserId(1L, Pageable.unpaged())).thenReturn(orderPage);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // Act
        ReviewResponseDTO result = reviewService.createReview(1L, 1L, createReviewRequest);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Excelente produto!", result.getComment());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar avaliação duplicada")
    void testCreateReview_Duplicate() {

        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reviewRepository.existsByProductIdAndUserId(1L, 1L)).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            reviewService.createReview(1L, 1L, createReviewRequest);
        });

        assertEquals("Você já avaliou este produto", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao avaliar produto não comprado")
    void testCreateReview_NotPurchased() {

        // Arrange
        Page<Order> emptyOrderPage = new PageImpl<>(Arrays.asList());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reviewRepository.existsByProductIdAndUserId(1L, 1L)).thenReturn(false);
        when(orderRepository.findByUserId(1L, Pageable.unpaged())).thenReturn(emptyOrderPage);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            reviewService.createReview(1L, 1L, createReviewRequest);
        });

        assertEquals("Você só pode avaliar produtos que comprou", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao avaliar produto inexistente")
    void testCreateReview_ProductNotFound() {

        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.createReview(999L, 1L, createReviewRequest);
        });
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao avaliar com usuário inexistente")
    void testCreateReview_UserNotFound() {

        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.createReview(1L, 999L, createReviewRequest);
        });
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve listar avaliações do produto")
    void testFindByProductId_Success() {

        // Arrange
        Page<Review> reviewPage = new PageImpl<>(Arrays.asList(review));
        when(productRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findByProductId(1L, Pageable.unpaged())).thenReturn(reviewPage);

        // Act
        Page<ReviewResponseDTO> result = reviewService.findByProductId(1L, Pageable.unpaged());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(5, result.getContent().get(0).getRating());
        verify(reviewRepository, times(1)).findByProductId(1L, Pageable.unpaged());
    }

    @Test
    @DisplayName("Deve lançar exceção ao listar avaliações de produto inexistente")
    void testFindByProductId_ProductNotFound() {

        // Arrange
        when(productRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.findByProductId(999L, Pageable.unpaged());
        });
        verify(reviewRepository, never()).findByProductId(any(), any());
    }

    @Test
    @DisplayName("Deve buscar rating do produto")
    void testGetProductRating_Success() {

        // Arrange
        when(productRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findAverageRatingByProductId(1L)).thenReturn(4.5);
        when(reviewRepository.countByProductId(1L)).thenReturn(10L);

        // Act
        ProductRatingDTO result = reviewService.getProductRating(1L);

        // Assert
        assertNotNull(result);
        assertEquals(4.5, result.getAverageRating());
        assertEquals(10L, result.getTotalReviews());
        verify(reviewRepository, times(1)).findAverageRatingByProductId(1L);
        verify(reviewRepository, times(1)).countByProductId(1L);
    }

    @Test
    @DisplayName("Deve retornar rating zero quando não há avaliações")
    void testGetProductRating_NoReviews() {

        // Arrange
        when(productRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findAverageRatingByProductId(1L)).thenReturn(null);
        when(reviewRepository.countByProductId(1L)).thenReturn(0L);

        // Act
        ProductRatingDTO result = reviewService.getProductRating(1L);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getAverageRating());
        assertEquals(0L, result.getTotalReviews());
    }

    @Test
    @DisplayName("Deve atualizar avaliação com sucesso")
    void testUpdateReview_Success() {

        // Arrange
        when(reviewRepository.findByProductIdAndUserId(1L, 1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // Act
        ReviewResponseDTO result = reviewService.updateReview(1L, 1L, updateReviewRequest);

        // Assert
        assertNotNull(result);
        assertEquals(4, review.getRating());
        assertEquals("Bom produto!", review.getComment());
        verify(reviewRepository, times(1)).save(review);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar avaliação inexistente")
    void testUpdateReview_NotFound() {

        // Arrange
        when(reviewRepository.findByProductIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.updateReview(999L, 1L, updateReviewRequest);
        });
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve deletar avaliação com sucesso")
    void testDeleteReview_Success() {

        // Arrange
        when(reviewRepository.existsByProductIdAndUserId(1L, 1L)).thenReturn(true);

        // Act
        reviewService.deleteReview(1L, 1L);

        // Assert
        verify(reviewRepository, times(1)).existsByProductIdAndUserId(1L, 1L);
        verify(reviewRepository, times(1)).deleteByProductIdAndUserId(1L, 1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar avaliação inexistente")
    void testDeleteReview_NotFound() {

        // Arrange
        when(reviewRepository.existsByProductIdAndUserId(999L, 1L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.deleteReview(999L, 1L);
        });
        verify(reviewRepository, never()).deleteByProductIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("Deve permitir avaliar produto de pedido PAID")
    void testCreateReview_PaidOrder() {

        // Arrange
        order.setStatus(OrderStatus.PAID);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(order));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reviewRepository.existsByProductIdAndUserId(1L, 1L)).thenReturn(false);
        when(orderRepository.findByUserId(1L, Pageable.unpaged())).thenReturn(orderPage);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // Act
        ReviewResponseDTO result = reviewService.createReview(1L, 1L, createReviewRequest);

        // Assert
        assertNotNull(result);
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve permitir avaliar produto de pedido SHIPPED")
    void testCreateReview_ShippedOrder() {

        // Arrange
        order.setStatus(OrderStatus.SHIPPED);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(order));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reviewRepository.existsByProductIdAndUserId(1L, 1L)).thenReturn(false);
        when(orderRepository.findByUserId(1L, Pageable.unpaged())).thenReturn(orderPage);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // Act
        ReviewResponseDTO result = reviewService.createReview(1L, 1L, createReviewRequest);

        // Assert
        assertNotNull(result);
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("Não deve permitir avaliar produto de pedido PENDING")
    void testCreateReview_PendingOrder() {

        // Arrange
        order.setStatus(OrderStatus.PENDING);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(order));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reviewRepository.existsByProductIdAndUserId(1L, 1L)).thenReturn(false);
        when(orderRepository.findByUserId(1L, Pageable.unpaged())).thenReturn(orderPage);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            reviewService.createReview(1L, 1L, createReviewRequest);
        });

        assertEquals("Você só pode avaliar produtos que comprou", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }
}
