package com.hammer.ecommerce.controllers;

import com.hammer.ecommerce.dto.address.AddressRequestDTO;
import com.hammer.ecommerce.dto.address.AddressResponseDTO;
import com.hammer.ecommerce.repositories.UserRepository;
import com.hammer.ecommerce.service.AddressService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Endereços", description = "Endpoints para gerenciamento de endereços de entrega")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    @Operation(summary = "Listar meus endereços",
            description = "Retorna todos os endereços cadastrados pelo usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Lista de endereços retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<AddressResponseDTO>> findAll(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<AddressResponseDTO> addresses = addressService.findAllByUser(userId);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Buscar endereço por ID",
            description = "Retorna os detalhes de um endereço específico do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Endereço encontrado",
                    content = @Content(schema = @Schema(implementation = AddressResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Endereço não pertence ao usuário", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponseDTO> findById(
            @Parameter(description = "ID do endereço") @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        AddressResponseDTO address = addressService.findById(id, userId);
        return ResponseEntity.ok(address);
    }

    @Operation(summary = "Criar endereço",
            description = "Cadastra um novo endereço de entrega para o usuário. Se for o primeiro endereço, será marcado como padrão automaticamente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Endereço criado com sucesso",
                    content = @Content(schema = @Schema(implementation = AddressResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (CEP, UF, etc)", content = @Content)
    })
    @PostMapping
    public ResponseEntity<AddressResponseDTO> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do endereço a ser criado",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AddressRequestDTO.class))
            )
            @Valid @RequestBody AddressRequestDTO request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        AddressResponseDTO address = addressService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    @Operation(summary = "Atualizar endereço",
            description = "Atualiza os dados de um endereço existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Endereço atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = AddressResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou endereço não pertence ao usuário",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<AddressResponseDTO> update(
            @Parameter(description = "ID do endereço") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Novos dados do endereço",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AddressRequestDTO.class))
            )
            @Valid @RequestBody AddressRequestDTO request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        AddressResponseDTO address = addressService.update(id, request, userId);
        return ResponseEntity.ok(address);
    }

    @Operation(summary = "Deletar endereço",
            description = "Remove um endereço. Se for o endereço padrão, outro será marcado como padrão automaticamente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Endereço deletado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Endereço não pertence ao usuário", content = @Content),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do endereço") @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        addressService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Definir como endereço padrão",
            description = "Marca um endereço como padrão para entregas. O endereço anterior deixa de ser padrão")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Endereço definido como padrão com sucesso",
                    content = @Content(schema = @Schema(implementation = AddressResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Endereço não pertence ao usuário", content = @Content),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado", content = @Content)
    })
    @PatchMapping("/{id}/set-default")
    public ResponseEntity<AddressResponseDTO> setAsDefault(
            @Parameter(description = "ID do endereço") @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        AddressResponseDTO address = addressService.setAsDefault(id, userId);
        return ResponseEntity.ok(address);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"))
                .getId();
    }
}