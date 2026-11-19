package com.jewelerp.repository;

import com.jewelerp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByPhone(String phone);

    @Query("select coalesce(max(c.id), 0) from Customer c")
    long maxId();

    @Query("""
            select c from Customer c
            where (:tier = '' or c.groupTier = :tier)
              and (:q = '' or lower(c.name) like lower(concat('%', :q, '%')) or c.phone like concat('%', :q, '%'))
            order by c.id desc
            """)
    List<Customer> search(@Param("q") String q, @Param("tier") String tier);
}
