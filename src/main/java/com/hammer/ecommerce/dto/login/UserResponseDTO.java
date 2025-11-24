package com.hammer.ecommerce.dto.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    //
    private Long id;
    private String name;
    private String email;
    private String cpf;
    private String phone;
    private String role;
    private LocalDateTime createdAt;
}