package com.inventories.infr.services;

import com.inventories.models.InventoriesEntity;
import com.inventories.models.UserEntity;
import com.inventories.models.enums.InventoryStatus;
import com.inventories.repositories.InventoriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Marca un inventario como en uso (LOCKED) mientras alguien esta dentro de su
 * conteo, y lo regresa a OPENED al salir.
 *
 * La app renueva el bloqueo cada minuto (heartbeat = volver a llamar a lock).
 * Si deja de renovarlo -la app se cerro de golpe, se quedo sin bateria o perdio
 * la red justo al salir- el bloqueo se considera abandonado tras LOCK_TIMEOUT:
 * otro usuario ya puede tomarlo y releaseStaleLocks lo regresa a OPENED.
 */
@Service
public class InventoryLockService {

    /** Sin renovacion durante este tiempo, el bloqueo se considera abandonado. */
    public static final Duration LOCK_TIMEOUT = Duration.ofMinutes(3);

    private static final Logger log = LoggerFactory.getLogger(InventoryLockService.class);

    public enum Outcome { LOCKED, UNLOCKED, NOT_FOUND, CLOSED, LOCKED_BY_OTHER }

    /** inventory es null si NOT_FOUND; holder es el usuario que lo tiene si LOCKED_BY_OTHER. */
    public record Result(Outcome outcome, InventoriesEntity inventory, String holder) {
    }

    @Autowired
    private InventoriesRepository inventoriesRepository;

    /** Toma el inventario para el usuario, o renueva su bloqueo si ya era suyo. */
    @Transactional
    public Result lock(Long inventoryId, UserEntity user) {
        Optional<InventoriesEntity> found = inventoriesRepository.findByIdForUpdate(inventoryId);
        if (found.isEmpty())
            return new Result(Outcome.NOT_FOUND, null, null);

        InventoriesEntity inventory = found.get();
        if (inventory.getStatus() == InventoryStatus.CLOSED)
            return new Result(Outcome.CLOSED, inventory, null);

        if (inventory.getStatus() == InventoryStatus.LOCKED && isHeldByOther(inventory, user) && !isStale(inventory))
            return new Result(Outcome.LOCKED_BY_OTHER, inventory, holderName(inventory));

        inventory.setStatus(InventoryStatus.LOCKED);
        inventory.setLockedBy(user);
        inventory.setLockedAt(now());
        inventoriesRepository.save(inventory);
        return new Result(Outcome.LOCKED, inventory, null);
    }

    /**
     * Regresa el inventario a OPENED si el bloqueo es del usuario (o ya estaba
     * abandonado). Si no esta bloqueado, o lo tiene otro usuario, no cambia nada:
     * salir del conteo nunca debe liberar el bloqueo de otra persona.
     */
    @Transactional
    public Result unlock(Long inventoryId, UserEntity user) {
        Optional<InventoriesEntity> found = inventoriesRepository.findByIdForUpdate(inventoryId);
        if (found.isEmpty())
            return new Result(Outcome.NOT_FOUND, null, null);

        InventoriesEntity inventory = found.get();
        if (inventory.getStatus() == InventoryStatus.LOCKED && (!isHeldByOther(inventory, user) || isStale(inventory))) {
            clearLock(inventory, InventoryStatus.OPENED);
            inventoriesRepository.save(inventory);
        }
        return new Result(Outcome.UNLOCKED, inventory, null);
    }

    /** Quita los datos del bloqueo y deja el inventario en el estado indicado. */
    public static void clearLock(InventoriesEntity inventory, InventoryStatus status) {
        inventory.setStatus(status);
        inventory.setLockedBy(null);
        inventory.setLockedAt(null);
    }

    /** Cada minuto regresa a OPENED los bloqueos que nadie renovo en LOCK_TIMEOUT. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    @Transactional
    public void releaseStaleLocks() {
        int released = inventoriesRepository.releaseStaleLocks(InventoryStatus.OPENED, InventoryStatus.LOCKED,
                new Timestamp(System.currentTimeMillis() - LOCK_TIMEOUT.toMillis()));
        if (released > 0)
            log.info("Bloqueos de inventario abandonados liberados: {}", released);
    }

    private static boolean isHeldByOther(InventoriesEntity inventory, UserEntity user) {
        UserEntity holder = inventory.getLockedBy();
        return holder != null && (user == null || !Objects.equals(holder.getId(), user.getId()));
    }

    private static boolean isStale(InventoriesEntity inventory) {
        Timestamp lockedAt = inventory.getLockedAt();
        return lockedAt == null || lockedAt.toInstant().isBefore(now().toInstant().minus(LOCK_TIMEOUT));
    }

    private static String holderName(InventoriesEntity inventory) {
        return inventory.getLockedBy() != null ? inventory.getLockedBy().getUsuario() : null;
    }

    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
