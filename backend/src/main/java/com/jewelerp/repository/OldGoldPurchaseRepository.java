package com.jewelerp.repository;

import com.jewelerp.model.OldGoldPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OldGoldPurchaseRepository extends JpaRepository<OldGoldPurchase, Long> {

    List<OldGoldPurchase> findAllByOrderByIdDesc();

    List<OldGoldPurchase> findByStatusAndInvoiceIdIsNullOrderByIdDesc(String status);

    List<OldGoldPurchase> findByCustomerIdOrderByIdDesc(Long customerId);
}
