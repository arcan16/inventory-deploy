package com.inventories.dto.users;

import com.inventories.models.UserEntity;

public record UserDTO(Long id,
                      String usuario,
                      String email) {
    public UserDTO(UserEntity user) {
        this(user.getId(), user.getUsuario(), user.getEmail());
    }
}
