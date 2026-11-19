package com.jewelerp.repository;

import com.jewelerp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    @Query("select p from Product p where :q = '' or lower(p.name) like lower(concat('%', :q, '%')) or lower(p.sku) like lower(concat('%', :q, '%')) order by p.id desc")
    List<Product> search(@Param("q") String q);
}
