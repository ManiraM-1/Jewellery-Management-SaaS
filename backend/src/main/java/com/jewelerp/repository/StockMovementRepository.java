package com.jewelerp.repository;

import com.jewelerp.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByInventoryItemIdOrderByIdDesc(Long inventoryItemId);
}
