package com.hammer.ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewRequestDTO {

    @NotNull(message = "Avaliação é obrigatória")
    @Min(value = 1, message = "Avaliação deve ser no mínimo 1")
    @Max(value = 5, message = "Avaliação deve ser no máximo 5")
    private Integer rating;

    @Size(max = 1000, message = "Comentário deve ter no máximo 1000 caracteres")
    private String comment;
}