package com.jewelerp.repository;

import com.jewelerp.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByStatus(String status);

    boolean existsByHuidIgnoreCase(String huid);

    long countByProductIdAndStatus(Long productId, String status);

    @Query("select coalesce(max(i.id), 0) from InventoryItem i")
    long maxId();

    @Query("""
            select i from InventoryItem i
            where (:branchId is null or i.branch.id = :branchId)
              and (:status = '' or i.status = :status)
              and (:metal = '' or i.product.metalType = :metal)
              and (:q = '' or lower(i.itemCode) like lower(concat('%', :q, '%'))
                   or i.barcode like concat('%', :q, '%')
                   or lower(i.huid) like lower(concat('%', :q, '%'))
                   or lower(i.product.name) like lower(concat('%', :q, '%')))
            order by i.id desc
            """)
    List<InventoryItem> search(@Param("branchId") Long branchId, @Param("status") String status,
                               @Param("metal") String metal, @Param("q") String q);
}
