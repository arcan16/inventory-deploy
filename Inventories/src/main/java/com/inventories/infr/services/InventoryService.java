package com.inventories.infr.services;

import com.inventories.dto.inventories.InventoriesDTO;
import com.inventories.models.InventoriesEntity;
import com.inventories.repositories.InventoriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryService {

    @Autowired
    private InventoriesRepository inventoriesRepository;

    public List<InventoriesDTO> getByType(String type) {
        return inventoriesRepository.getAllByType(type);
    }
}
