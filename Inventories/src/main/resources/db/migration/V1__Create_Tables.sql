CREATE TABLE users(
    id bigint NOT NULL AUTO_INCREMENT,
    usuario VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    PRIMARY KEY(id)
)ENGINE=InnoDB;

 CREATE TABLE inventories (
  id bigint NOT NULL AUTO_INCREMENT,
  created_by BIGINT NULL,
  created_by_snapshot VARCHAR(250),
  inventory_date date NOT NULL,
  presentation varchar(10) NOT NULL,
  status ENUM('OPENED','LOCKED','PENDING','CLOSED') NOT NULL DEFAULT 'OPENED',  -- OPENED: Inventario creado sin comenzar, LOCKED:En uso, PENDING: Comenzado sin finalizar y sin usuario utilizándolo, CLOSED: Finalizado
  locked_by BIGINT NULL, -- ID del usuario que bloqueo el inventario = locked, NULL = unlocked
  locked_at TIMESTAMP NULL DEFAULT NULL, -- Momento en que fue bloqueado
  PRIMARY KEY (id),
  CONSTRAINT fk_created_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT fk_inventory_user FOREIGN KEY (locked_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE products (
  description varchar(50) NOT NULL,
  id varchar(10) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE stock (
  id bigint NOT NULL AUTO_INCREMENT,
  id_product varchar(10) DEFAULT NULL,
  stock decimal(6,3) NOT NULL,
  id_inventory bigint NOT NULL,
  PRIMARY KEY (id),
  KEY fk_id_product (id_product),
  KEY fk_id_inventory (id_inventory),
  CONSTRAINT fk_id_inventory FOREIGN KEY (id_inventory) REFERENCES inventories (id) ON DELETE CASCADE,
  CONSTRAINT fk_id_product FOREIGN KEY (id_product) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE product_counts (
  id bigint NOT NULL AUTO_INCREMENT,
  id_inventory bigint NOT NULL,
  id_product varchar(10) NOT NULL,
  quantity decimal(6,3) NOT NULL,
  place ENUM('SALES_AREA','WAREHOUSE','STORAGE_AREA','NOTE') NOT NULL, -- Almacena el lugar en que fue encontrado el producto en cuestion (piso, almacen o bodega)
  PRIMARY KEY (id),
  KEY fk_id_inventory_pc (id_inventory),
  KEY fk_id_product_pc (id_product),
  CONSTRAINT fk_id_inventory_pc FOREIGN KEY (id_inventory) REFERENCES inventories (id) ON DELETE CASCADE,
  CONSTRAINT fk_id_product_pc FOREIGN KEY (id_product) REFERENCES products (id)
) ENGINE=InnoDB;