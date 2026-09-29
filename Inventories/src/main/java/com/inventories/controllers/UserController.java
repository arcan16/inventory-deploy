package com.inventories.controllers;

import com.inventories.dto.users.CreateUserDTO;
import com.inventories.dto.users.UpdateUserDTO;
import com.inventories.dto.users.UserDTO;
import com.inventories.models.UserEntity;
import com.inventories.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<?> getAllUsers(@PageableDefault(size = 10, sort = "usuario") Pageable pageable){
        return ResponseEntity.ok(userRepository.findAll(pageable).map(UserDTO::new));
    }

    /**
     * Datos del usuario autenticado (resuelto por JwtAuthorizationFilter en el
     * SecurityContext), para la pantalla de "Mi cuenta" de la app movil.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || authentication.getName() == null)
            return ResponseEntity.status(401).body("{\"err\": \"No autenticado\"}");

        Optional<UserEntity> user = userRepository.findByUsuario(authentication.getName());
        if(user.isEmpty())
            return ResponseEntity.badRequest().body("{\"err\": \"El usuario no existe\"}");
        return ResponseEntity.ok(new UserDTO(user.get()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id){
        Optional<UserEntity> user = userRepository.findById(id);
        if(user.isEmpty())
            return ResponseEntity.badRequest().body("{\"err\": \"El usuario no existe\"}");
        return ResponseEntity.ok(new UserDTO(user.get()));
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserDTO createUserDTO, UriComponentsBuilder uriBuilder){
        if(userRepository.findByUsuario(createUserDTO.usuario()).isPresent())
            return ResponseEntity.badRequest().body("{\"err\": \"El usuario ya existe\"}");

        UserEntity user = new UserEntity();
        user.setUsuario(createUserDTO.usuario());
        user.setPassword(passwordEncoder.encode(createUserDTO.password()));
        user.setEmail(createUserDTO.email());
        userRepository.save(user);

        URI url = uriBuilder.path("/users/{id}").buildAndExpand(user.getId()).toUri();
        return ResponseEntity.created(url).body(new UserDTO(user));
    }

    @PutMapping
    public ResponseEntity<?> updateUser(@Valid @RequestBody UpdateUserDTO updateUserDTO){
        Optional<UserEntity> optionalUser = userRepository.findById(updateUserDTO.id());
        if(optionalUser.isEmpty())
            return ResponseEntity.badRequest().body("{\"err\": \"El usuario no existe\"}");

        UserEntity user = optionalUser.get();

        if(updateUserDTO.usuario() != null && !updateUserDTO.usuario().isBlank()){
            Optional<UserEntity> existing = userRepository.findByUsuario(updateUserDTO.usuario());
            if(existing.isPresent() && !existing.get().getId().equals(user.getId()))
                return ResponseEntity.badRequest().body("{\"err\": \"El usuario ya existe\"}");
            user.setUsuario(updateUserDTO.usuario());
        }
        if(updateUserDTO.password() != null && !updateUserDTO.password().isBlank())
            user.setPassword(passwordEncoder.encode(updateUserDTO.password()));
        if(updateUserDTO.email() != null && !updateUserDTO.email().isBlank())
            user.setEmail(updateUserDTO.email());

        userRepository.save(user);
        return ResponseEntity.ok(new UserDTO(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id){
        Optional<UserEntity> user = userRepository.findById(id);
        if(user.isEmpty())
            return ResponseEntity.badRequest().body("{\"err\": \"El usuario no existe\"}");
        userRepository.deleteById(id);
        return ResponseEntity.ok().body("{\"message\":\"Usuario eliminado correctamente\"}");
    }
}
