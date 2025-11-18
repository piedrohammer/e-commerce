package com.hammer.ecommerce.controllers;

import com.hammer.ecommerce.dto.order.CreateOrderRequestDTO;
import com.hammer.ecommerce.dto.order.OrderResponseDTO;
import com.hammer.ecommerce.dto.order.OrderSummaryDTO;
import com.hammer.ecommerce.model.OrderStatus;
import com.hammer.ecommerce.repositories.UserRepository;
import com.hammer.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Endpoints para gerenciamento de pedidos")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @Operation(summary = "Criar pedido (Checkout)",
            description = "Cria um novo pedido a partir dos itens do carrinho. O carrinho é limpo e o estoque é reduzido automaticamente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Carrinho vazio, estoque insuficiente ou endereço inválido",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "ID do endereço de entrega",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateOrderRequestDTO.class))
            )
            @Valid @RequestBody CreateOrderRequestDTO request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        OrderResponseDTO order = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Operation(summary = "Listar meus pedidos",
            description = "Retorna todos os pedidos do usuário autenticado com paginação")
    @ApiResponse(responseCode = "200", description = "Lista de pedidos retornada com sucesso")
    @GetMapping
    public ResponseEntity<Page<OrderSummaryDTO>> findAllByUser(
            @Parameter(description = "Número da página (começa em 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderSummaryDTO> orders = orderService.findAllByUser(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Buscar pedido por ID",
            description = "Retorna os detalhes completos de um pedido específico do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Pedido não pertence ao usuário", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> findById(
            @Parameter(description = "ID do pedido") @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        OrderResponseDTO order = orderService.findById(id, userId);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Cancelar pedido",
            description = "Cancela um pedido pendente ou pago. O estoque dos produtos é devolvido automaticamente. Não é possível cancelar pedidos já enviados ou entregues")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido cancelado com sucesso",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Pedido não pode ser cancelado (já enviado/entregue) ou não pertence ao usuário",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content)
    })
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @Parameter(description = "ID do pedido") @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        OrderResponseDTO order = orderService.cancelOrder(id, userId);
        return ResponseEntity.ok(order);
    }

    // -------- Endpoints ADMIN --------

    @Operation(summary = "[ADMIN] Listar todos os pedidos",
            description = "Retorna todos os pedidos do sistema com paginação (apenas ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão de administrador", content = @Content)
    })
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderSummaryDTO>> findAll(
            @Parameter(description = "Número da página (começa em 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderSummaryDTO> orders = orderService.findAll(pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "[ADMIN] Atualizar status do pedido",
            description = "Atualiza o status de um pedido. Validações: PENDING só pode ir para PAID ou CANCELLED, não pode alterar CANCELLED ou DELIVERED (apenas ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão de administrador", content = @Content)
    })
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDTO> updateStatus(
            @Parameter(description = "ID do pedido") @PathVariable Long id,
            @Parameter(description = "Novo status do pedido", required = true,
                    schema = @Schema(implementation = OrderStatus.class,
                            allowableValues = {"PENDING", "PAID", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"}))
            @RequestParam OrderStatus status) {
        OrderResponseDTO order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(order);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"))
                .getId();
    }
}