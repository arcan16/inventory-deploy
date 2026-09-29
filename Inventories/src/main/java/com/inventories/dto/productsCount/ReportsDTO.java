package com.inventories.dto.productsCount;

import java.sql.Date;

public record ReportsDTO (Long idInventory,
                          Date inventoryDate,
                          String presentation){
}
