package com.hammer.ecommerce.controllers;

import com.hammer.ecommerce.dto.cart.AddToCartRequestDTO;
import com.hammer.ecommerce.dto.cart.CartResponseDTO;
import com.hammer.ecommerce.dto.cart.UpdateCartItemRequestDTO;
import com.hammer.ecommerce.repositories.UserRepository;
import com.hammer.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Carrinho", description = "Endpoints para gerenciamento do carrinho de compras")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @Operation(summary = "Ver carrinho",
            description = "Retorna o carrinho do usuário autenticado com todos os itens")
    @ApiResponse(responseCode = "200", description = "Carrinho retornado com sucesso")
    @GetMapping
    public ResponseEntity<CartResponseDTO> getCart(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        CartResponseDTO cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }

    @Operation(summary = "Adicionar produto ao carrinho",
            description = "Adiciona um produto ao carrinho ou incrementa a quantidade se já existir")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto adicionado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Estoque insuficiente ou produto inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content)
    })
    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addToCart(
            @Valid @RequestBody AddToCartRequestDTO request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        CartResponseDTO cart = cartService.addToCart(userId, request);
        return ResponseEntity.ok(cart);
    }

    @Operation(summary = "Atualizar quantidade de item",
            description = "Atualiza a quantidade de um item no carrinho")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quantidade atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Estoque insuficiente ou item não pertence ao usuário", content = @Content),
            @ApiResponse(responseCode = "404", description = "Item não encontrado", content = @Content)
    })
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDTO> updateCartItem(
            @Parameter(description = "ID do item no carrinho") @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequestDTO request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        CartResponseDTO cart = cartService.updateCartItem(userId, itemId, request);
        return ResponseEntity.ok(cart);
    }

    @Operation(summary = "Remover item do carrinho",
            description = "Remove um item específico do carrinho")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item removido com sucesso"),
            @ApiResponse(responseCode = "400", description = "Item não pertence ao usuário", content = @Content),
            @ApiResponse(responseCode = "404", description = "Item não encontrado", content = @Content)
    })
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDTO> removeCartItem(
            @Parameter(description = "ID do item no carrinho") @PathVariable Long itemId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        CartResponseDTO cart = cartService.removeCartItem(userId, itemId);
        return ResponseEntity.ok(cart);
    }

    @Operation(summary = "Limpar carrinho",
            description = "Remove todos os itens do carrinho")
    @ApiResponse(responseCode = "204", description = "Carrinho limpo com sucesso")
    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        cartService.clearCart(userId);
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
