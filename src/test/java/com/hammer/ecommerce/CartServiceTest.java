package com.hammer.ecommerce;

import com.hammer.ecommerce.dto.cart.AddToCartRequestDTO;
import com.hammer.ecommerce.dto.cart.CartResponseDTO;
import com.hammer.ecommerce.dto.cart.UpdateCartItemRequestDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.*;
import com.hammer.ecommerce.repositories.*;
import com.hammer.ecommerce.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Cart cart;
    private Product product;
    private CartItem cartItem;
    private AddToCartRequestDTO addToCartRequest;
    private UpdateCartItemRequestDTO updateCartItemRequest;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setName("João Silva");

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);

        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");

        product = new Product();
        product.setId(1L);
        product.setName("Mouse Gamer");
        product.setPrice(new BigDecimal("150.00"));
        product.setStockQuantity(20);
        product.setActive(true);
        product.setCategory(category);

        cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        addToCartRequest = new AddToCartRequestDTO();
        addToCartRequest.setProductId(1L);
        addToCartRequest.setQuantity(2);

        updateCartItemRequest = new UpdateCartItemRequestDTO();
        updateCartItemRequest.setQuantity(3);
    }

    @Test
    @DisplayName("Deve buscar carrinho existente")
    void testGetCart_ExistingCart() {

        // Arrange
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));

        // Act
        CartResponseDTO result = cartService.getCart(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(cartRepository, times(1)).findByUserIdWithItems(1L);
    }

    @Test
    @DisplayName("Deve criar carrinho se não existir")
    void testGetCart_CreateNewCart() {

        // Arrange
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartResponseDTO result = cartService.getCart(1L);

        // Assert
        assertNotNull(result);
        verify(cartRepository, times(1)).findByUserIdWithItems(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Deve adicionar produto ao carrinho com sucesso")
    void testAddToCart_Success() {

        // Arrange
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartResponseDTO result = cartService.addToCart(1L, addToCartRequest);

        // Assert
        assertNotNull(result);
        verify(productRepository, times(1)).findById(1L);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Deve incrementar quantidade ao adicionar produto existente")
    void testAddToCart_IncrementExisting() {

        // Arrange
        cart.getItems().add(cartItem);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartResponseDTO result = cartService.addToCart(1L, addToCartRequest);

        // Assert
        assertNotNull(result);
        assertEquals(4, cartItem.getQuantity());
        verify(cartItemRepository, times(1)).save(cartItem);
    }

    @Test
    @DisplayName("Deve lançar exceção ao adicionar produto inativo")
    void testAddToCart_InactiveProduct() {

        // Arrange
        product.setActive(false);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            cartService.addToCart(1L, addToCartRequest);
        });

        assertEquals("Produto não está disponível", exception.getMessage());
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao adicionar quantidade maior que estoque")
    void testAddToCart_InsufficientStock() {

        // Arrange
        addToCartRequest.setQuantity(25);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Stub leniente — não gera UnnecessaryStubbingException mesmo se não for usado
        lenient().when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            cartService.addToCart(1L, addToCartRequest);
        });

        assertTrue(exception.getMessage().contains("Estoque insuficiente"));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao adicionar produto inexistente")
    void testAddToCart_ProductNotFound() {

        // Arrange
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        addToCartRequest.setProductId(999L);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.addToCart(1L, addToCartRequest);
        });
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Deve atualizar quantidade do item no carrinho")
    void testUpdateCartItem_Success() {

        // Arrange
        cart.getItems().add(cartItem);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartResponseDTO result = cartService.updateCartItem(1L, 1L, updateCartItemRequest);

        // Assert
        assertNotNull(result);
        assertEquals(3, cartItem.getQuantity());
        verify(cartItemRepository, times(1)).save(cartItem);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar item de outro carrinho")
    void testUpdateCartItem_WrongCart() {

        // Arrange
        Cart otherCart = new Cart();
        otherCart.setId(2L);
        cartItem.setCart(otherCart);

        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            cartService.updateCartItem(1L, 1L, updateCartItemRequest);
        });

        assertEquals("Item não pertence ao seu carrinho", exception.getMessage());
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar com estoque insuficiente")
    void testUpdateCartItem_InsufficientStock() {

        // Arrange
        updateCartItemRequest.setQuantity(25);
        cart.getItems().add(cartItem);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            cartService.updateCartItem(1L, 1L, updateCartItemRequest);
        });

        assertTrue(exception.getMessage().contains("Estoque insuficiente"));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Deve remover item do carrinho")
    void testRemoveCartItem_Success() {

        // Arrange
        cart.getItems().add(cartItem);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartResponseDTO result = cartService.removeCartItem(1L, 1L);

        // Assert
        assertNotNull(result);
        verify(cartItemRepository, times(1)).delete(cartItem);
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    @DisplayName("Deve lançar exceção ao remover item inexistente")
    void testRemoveCartItem_NotFound() {

        // Arrange
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.removeCartItem(1L, 999L);
        });
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    @DisplayName("Deve limpar carrinho")
    void testClearCart_Success() {

        // Arrange
        cart.getItems().add(cartItem);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        cartService.clearCart(1L);

        // Assert
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(cart);
    }
}