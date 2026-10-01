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
 * otro dispositivo ya puede tomarlo y releaseStaleLocks lo regresa a OPENED.
 */
@Service
public class InventoryLockService {

    /** Sin renovacion durante este tiempo, el bloqueo se considera abandonado. */
    public static final Duration LOCK_TIMEOUT = Duration.ofMinutes(3);

    private static final Logger log = LoggerFactory.getLogger(InventoryLockService.class);

    public enum Outcome { LOCKED, UNLOCKED, NOT_FOUND, CLOSED, LOCKED_BY_OTHER }

    /**
     * inventory es null si NOT_FOUND. Si LOCKED_BY_OTHER: holder es el usuario que
     * lo tiene y sameUser indica que es la misma cuenta, pero en otro dispositivo.
     */
    public record Result(Outcome outcome, InventoriesEntity inventory, String holder, boolean sameUser) {
        Result(Outcome outcome, InventoriesEntity inventory) {
            this(outcome, inventory, null, false);
        }
    }

    @Autowired
    private InventoriesRepository inventoriesRepository;

    /**
     * Toma el inventario para este dispositivo, o renueva su bloqueo si ya era
     * suyo. El bloqueo es por dispositivo (deviceId = header X-Device-Id que envia
     * cada instalacion de la app): mientras este vigente, nadie mas puede entrar,
     * ni siquiera el mismo usuario desde otro telefono.
     */
    @Transactional
    public Result lock(Long inventoryId, UserEntity user, String deviceId) {
        Optional<InventoriesEntity> found = inventoriesRepository.findByIdForUpdate(inventoryId);
        if (found.isEmpty())
            return new Result(Outcome.NOT_FOUND, null);

        InventoriesEntity inventory = found.get();
        if (inventory.getStatus() == InventoryStatus.CLOSED)
            return new Result(Outcome.CLOSED, inventory);

        if (inventory.getStatus() == InventoryStatus.LOCKED && isHeldByOther(inventory, user, deviceId) && !isStale(inventory))
            return new Result(Outcome.LOCKED_BY_OTHER, inventory, holderName(inventory), isSameUser(inventory, user));

        inventory.setStatus(InventoryStatus.LOCKED);
        inventory.setLockedBy(user);
        inventory.setLockedAt(now());
        inventory.setLockedDevice(deviceId);
        inventoriesRepository.save(inventory);
        return new Result(Outcome.LOCKED, inventory);
    }

    /**
     * Regresa el inventario a OPENED si el bloqueo es de este dispositivo (o ya
     * estaba abandonado). Si no esta bloqueado, o lo tiene otro dispositivo, no
     * cambia nada: salir del conteo nunca debe liberar el bloqueo de otro.
     */
    @Transactional
    public Result unlock(Long inventoryId, UserEntity user, String deviceId) {
        Optional<InventoriesEntity> found = inventoriesRepository.findByIdForUpdate(inventoryId);
        if (found.isEmpty())
            return new Result(Outcome.NOT_FOUND, null);

        InventoriesEntity inventory = found.get();
        if (inventory.getStatus() == InventoryStatus.LOCKED
                && (!isHeldByOther(inventory, user, deviceId) || isStale(inventory))) {
            clearLock(inventory, InventoryStatus.OPENED);
            inventoriesRepository.save(inventory);
        }
        return new Result(Outcome.UNLOCKED, inventory);
    }

    /** Quita los datos del bloqueo y deja el inventario en el estado indicado. */
    public static void clearLock(InventoriesEntity inventory, InventoryStatus status) {
        inventory.setStatus(status);
        inventory.setLockedBy(null);
        inventory.setLockedAt(null);
        inventory.setLockedDevice(null);
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

    /**
     * El dueño del bloqueo es el dispositivo que lo tomo. Una peticion sin
     * X-Device-Id (version vieja de la app) nunca es dueña de un bloqueo con
     * dispositivo. Solo los bloqueos sin dispositivo (tomados por una version
     * vieja) se comparan por usuario.
     */
    private static boolean isHeldByOther(InventoriesEntity inventory, UserEntity user, String deviceId) {
        if (inventory.getLockedDevice() != null)
            return deviceId == null || !inventory.getLockedDevice().equals(deviceId);
        UserEntity holder = inventory.getLockedBy();
        return holder != null && (user == null || !Objects.equals(holder.getId(), user.getId()));
    }

    private static boolean isSameUser(InventoriesEntity inventory, UserEntity user) {
        UserEntity holder = inventory.getLockedBy();
        return holder != null && user != null && Objects.equals(holder.getId(), user.getId());
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
