-- El codigo de barras deja de pertenecer al producto y pasa a cada una de sus
-- presentaciones. Los codigos capturados en products.barcode se descartan.
ALTER TABLE products
  DROP COLUMN barcode;

CREATE TABLE product_presentations (
  id bigint NOT NULL AUTO_INCREMENT,
  id_product varchar(10) NOT NULL,
  presentation ENUM('1/8 LT','1/4 LT','1 LT','4 LTS','19 LTS','PIEZA') NOT NULL,
  barcode VARCHAR(64) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_presentation (id_product, presentation), -- Un producto no repite presentacion
  UNIQUE KEY uk_presentation_barcode (barcode), -- Un codigo de barras identifica una sola presentacion
  CONSTRAINT fk_id_product_pp FOREIGN KEY (id_product) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB;
