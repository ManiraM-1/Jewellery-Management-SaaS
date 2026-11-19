package com.jewelerp.repository;

import com.jewelerp.model.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    List<LoyaltyTransaction> findByCustomerIdOrderByIdDesc(Long customerId);
}
