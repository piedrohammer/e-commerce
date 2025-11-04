package com.hammer.ecommerce.controllers;

import com.hammer.ecommerce.dto.CreateReviewRequestDTO;
import com.hammer.ecommerce.dto.ProductRatingDTO;
import com.hammer.ecommerce.dto.ReviewResponseDTO;
import com.hammer.ecommerce.dto.UpdateReviewRequestDTO;
import com.hammer.ecommerce.repositories.UserRepository;
import com.hammer.ecommerce.service.ReviewService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Avaliações", description = "Endpoints para gerenciamento de avaliações de produtos")
public class ReviewController {

    // Os reviews são acessíveis via /api/products/{productId}/reviews,
    // que já está liberado como público para leitura. Para criar/atualizar/deletar, é necessário autenticação.

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @Operation(summary = "Criar avaliação",
            description = "Cria uma avaliação para um produto. Usuário deve ter comprado o produto (pedido com status PAID, SHIPPED ou DELIVERED). " +
                    "Cada usuário pode avaliar um produto apenas uma vez",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Avaliação criada com sucesso",
                    content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Usuário já avaliou este produto ou não comprou o produto",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(
            @Parameter(description = "ID do produto a ser avaliado") @PathVariable Long productId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da avaliação (rating de 1 a 5 e comentário opcional)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateReviewRequestDTO.class))
            )
            @Valid @RequestBody CreateReviewRequestDTO request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        ReviewResponseDTO review = reviewService.createReview(productId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @Operation(summary = "Listar avaliações do produto",
            description = "Retorna todas as avaliações de um produto com paginação. Endpoint público, não requer autenticação")
    @ApiResponse(responseCode = "200", description = "Lista de avaliações retornada com sucesso")
    @GetMapping
    public ResponseEntity<Page<ReviewResponseDTO>> findByProductId(
            @Parameter(description = "ID do produto") @PathVariable Long productId,
            @Parameter(description = "Número da página (começa em 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewResponseDTO> reviews = reviewService.findByProductId(productId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "Ver rating do produto",
            description = "Retorna a média de avaliações e o total de avaliações do produto. Endpoint público, não requer autenticação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rating retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = ProductRatingDTO.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content)
    })
    @GetMapping("/rating")
    public ResponseEntity<ProductRatingDTO> getProductRating(
            @Parameter(description = "ID do produto") @PathVariable Long productId) {
        ProductRatingDTO rating = reviewService.getProductRating(productId);
        return ResponseEntity.ok(rating);
    }

    @Operation(summary = "Atualizar avaliação",
            description = "Atualiza a avaliação do usuário para o produto. Apenas o autor da avaliação pode atualizá-la",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avaliação atualizada com sucesso",
                    content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @PutMapping
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @Parameter(description = "ID do produto") @PathVariable Long productId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Novos dados da avaliação",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateReviewRequestDTO.class))
            )
            @Valid @RequestBody UpdateReviewRequestDTO request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        ReviewResponseDTO review = reviewService.updateReview(productId, userId, request);
        return ResponseEntity.ok(review);
    }

    @Operation(summary = "Deletar avaliação",
            description = "Remove a avaliação do usuário para o produto. Apenas o autor da avaliação pode deletá-la",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Avaliação deletada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "ID do produto") @PathVariable Long productId,
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
