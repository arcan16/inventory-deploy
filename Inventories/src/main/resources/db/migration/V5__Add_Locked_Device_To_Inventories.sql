-- Dispositivo (instalacion de la app) que tiene bloqueado el inventario. El
-- bloqueo es por dispositivo y no solo por usuario: con la misma cuenta en dos
-- telefonos, solo el que entro primero puede usar el inventario.
ALTER TABLE inventories
  ADD COLUMN locked_device VARCHAR(64) NULL AFTER locked_at;
