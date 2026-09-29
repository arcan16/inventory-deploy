package com.inventories.dto.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record UpdateUserDTO(@NotNull Long id,
                            String usuario,
                            String password,
                            @Email String email) {
}
