package com.inventories.controllers;

import com.inventories.dto.stock.StockListDTO;
import com.inventories.models.StockEntity;
import com.inventories.repositories.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private StockRepository stockRepository;

    @GetMapping("/{idInventory}")
    public ResponseEntity<?> getStockFromInventory(@PathVariable Long idInventory){
        List<StockListDTO> stockList = stockRepository.getByIdInventory(idInventory);

        return ResponseEntity.ok(stockList);
    }
}
