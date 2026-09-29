package com.inventories.config;

import com.inventories.models.UserEntity;
import com.inventories.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if(userRepository.count() == 0) {
            UserEntity admin = new UserEntity();

            admin.setUsuario("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setEmail("admin@inventory.com");

            userRepository.save(admin);
        }
    }
}
