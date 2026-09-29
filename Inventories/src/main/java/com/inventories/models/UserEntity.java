package com.inventories.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity(name = "UserEntity")
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String usuario;

    @NotBlank
    private String password;

    @Email
    @NotBlank
    private String email;

}
