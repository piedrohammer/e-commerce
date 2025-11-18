package com.hammer.ecommerce.service;

import com.hammer.ecommerce.dto.cart.AddToCartRequestDTO;
import com.hammer.ecommerce.dto.cart.CartItemResponseDTO;
import com.hammer.ecommerce.dto.cart.CartResponseDTO;
import com.hammer.ecommerce.dto.cart.UpdateCartItemRequestDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.Cart;
import com.hammer.ecommerce.model.CartItem;
import com.hammer.ecommerce.model.Product;
import com.hammer.ecommerce.model.User;
import com.hammer.ecommerce.repositories.CartItemRepository;
import com.hammer.ecommerce.repositories.CartRepository;
import com.hammer.ecommerce.repositories.ProductRepository;
import com.hammer.ecommerce.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponseDTO getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return convertToDTO(cart);
    }

    @Transactional
    public CartResponseDTO addToCart(Long userId, AddToCartRequestDTO request) {
        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));

        // Verifica se produto está ativo
        if (!product.getActive()) {
            throw new BusinessException("Produto não está disponível");
        }

        // Verifica estoque
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BusinessException("Estoque insuficiente. Disponível: " + product.getStockQuantity());
        }

        // Verifica se o item já existe no carrinho
        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existingItem != null) {
            // Atualizar quantidade
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            // Verifica estoque novamente
            if (product.getStockQuantity() < newQuantity) {
                throw new BusinessException("Estoque insuficiente. Disponível: " + product.getStockQuantity());
            }

            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            // Criar novo item
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());

            cart.addItem(cartItem);
            cartItemRepository.save(cartItem);
        }

        cart = cartRepository.save(cart);
        return convertToDTO(cart);
    }

    @Transactional
    public CartResponseDTO updateCartItem(Long userId, Long itemId, UpdateCartItemRequestDTO request) {
        Cart cart = getOrCreateCart(userId);

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado no carrinho"));

        // Verifica se o item pertence ao carrinho do usuário
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new BusinessException("Item não pertence ao seu carrinho");
        }

        // Verificar estoque
        if (cartItem.getProduct().getStockQuantity() < request.getQuantity()) {
            throw new BusinessException("Estoque insuficiente. Disponível: " +
                    cartItem.getProduct().getStockQuantity());
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        cart = cartRepository.save(cart);
        return convertToDTO(cart);
    }

    @Transactional
    public CartResponseDTO removeCartItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado no carrinho"));

        // Verifica se o item pertence ao carrinho do usuário
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new BusinessException("Item não pertence ao seu carrinho");
        }

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        cart = cartRepository.save(cart);
        return convertToDTO(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCart(userId));
    }

    private Cart createCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    private CartResponseDTO convertToDTO(Cart cart) {
        CartResponseDTO dto = new CartResponseDTO();
        dto.setId(cart.getId());
        dto.setUpdatedAt(cart.getUpdatedAt());

        dto.setItems(cart.getItems().stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList()));

        dto.setTotalAmount(cart.getTotalAmount());
        dto.setTotalItems(cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum());

        return dto;
    }

    private CartItemResponseDTO convertItemToDTO(CartItem item) {
        CartItemResponseDTO dto = new CartItemResponseDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductImageUrl(item.getProduct().getImageUrl());
        dto.setProductPrice(item.getProduct().getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(item.getSubtotal());
        dto.setAvailableStock(item.getProduct().getStockQuantity());
        return dto;
    }
}