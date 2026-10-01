package com.inventories;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// EnableScheduling: InventoryLockService libera cada minuto los bloqueos de inventario abandonados.
@EnableScheduling
@SpringBootApplication
public class InventoriesApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoriesApplication.class, args);
	}

}
