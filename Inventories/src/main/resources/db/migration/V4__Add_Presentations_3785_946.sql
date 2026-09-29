-- Agrega las presentaciones de galon (3.785 LTS) y 2.5 galones (9.46 LTS).
ALTER TABLE product_presentations
  MODIFY COLUMN presentation ENUM('1/8 LT','1/4 LT','1 LT','3.785 LTS','4 LTS','9.46 LTS','19 LTS','PIEZA') NOT NULL;
