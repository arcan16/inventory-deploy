package com.inventories.infr.services;

import com.inventories.dto.backup.BackupDTO;
import com.inventories.dto.backup.ImportResultDTO;
import com.inventories.models.ProductPresentationsEntity;
import com.inventories.models.ProductsEntity;
import com.inventories.models.enums.ProductPresentation;
import com.inventories.repositories.InventoriesRepository;
import com.inventories.repositories.ProductCountsRepository;
import com.inventories.repositories.ProductPresentationsRepository;
import com.inventories.repositories.ProductsRepository;
import com.inventories.repositories.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/** Reglas de combinacion al importar un respaldo (sin base de datos: repositorios simulados). */
@ExtendWith(MockitoExtension.class)
class DataTransferServiceTest {

    @Mock
    private ProductsRepository productsRepository;
    @Mock
    private ProductPresentationsRepository productPresentationsRepository;
    @Mock
    private InventoriesRepository inventoriesRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private ProductCountsRepository productCountsRepository;

    @InjectMocks
    private DataTransferService service;

    private ProductsEntity white;
    private ProductPresentationsEntity whiteGallon;

    @BeforeEach
    void setUp() {
        white = new ProductsEntity("100", "PINTURA BLANCA");
        whiteGallon = new ProductPresentationsEntity(1L, white, ProductPresentation.FOUR_LITERS, "7500000000001");
    }

    @Test
    void mergesWithoutDeletingAndSkipsInvalidRows() {
        when(productsRepository.findAll()).thenReturn(List.of(white));
        when(productPresentationsRepository.findAll()).thenReturn(List.of(whiteGallon));

        BackupDTO backup = new BackupDTO(BackupDTO.FORMAT, 1, "2026-10-01T10:00:00",
                List.of(new BackupDTO.Product("100", "PINTURA BLANCA MATE"),   // actualiza descripcion
                        new BackupDTO.Product("200", "THINNER"),               // nuevo
                        new BackupDTO.Product("ID-DEMASIADO-LARGO", "X")),     // no cabe en varchar(10)
                List.of(new BackupDTO.Presentation("200", "4 lts", "7500000000001"), // codigo de otra presentacion
                        new BackupDTO.Presentation("200", "1 LT", "7500000000002"),
                        new BackupDTO.Presentation("200", "GARRAFA", null),          // presentacion desconocida
                        new BackupDTO.Presentation("999", "1 LT", null)),             // producto inexistente
                List.of(new BackupDTO.Inventory(7L, "2024-01-04", "4 LTS", "LOCKED",
                        List.of(new BackupDTO.StockRow("100", 2f), new BackupDTO.StockRow("999", 1f)),
                        List.of(new BackupDTO.CountRow("200", 1.5f, "WAREHOUSE"),
                                new BackupDTO.CountRow("100", 1f, "COCINA"),
                                new BackupDTO.CountRow("100", 1000f, "NOTE")))));

        ImportResultDTO result = service.importBackup(backup, null);

        assertEquals(1, result.productsCreated());
        assertEquals(1, result.productsUpdated());
        assertEquals("PINTURA BLANCA MATE", white.getDescription());
        assertEquals(2, result.presentationsCreated());
        assertEquals(1, result.barcodesAssigned());
        assertEquals(1, result.barcodeConflicts());
        assertEquals("7500000000001", whiteGallon.getBarcode()); // El existente conserva su codigo.
        assertEquals(1, result.inventoriesCreated());
        assertEquals(1, result.stockRowsCreated());
        assertEquals(1, result.countsCreated());
        // Producto largo, GARRAFA, producto 999, stock de 999, lugar COCINA y cantidad 1000.
        assertEquals(6, result.rowsSkipped());
    }

    @Test
    void rejectsFilesThatAreNotBackups() {
        assertThrows(IllegalArgumentException.class, () -> service.importBackup(
                new BackupDTO("otro-formato", 1, null, null, null, null), null));
        assertThrows(IllegalArgumentException.class, () -> service.importBackup(
                new BackupDTO(BackupDTO.FORMAT, BackupDTO.VERSION + 1, null, null, null, null), null));
    }
}
