package com.jewelerp.repository;

import com.jewelerp.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("select coalesce(max(i.id), 0) from Invoice i")
    long maxId();

    List<Invoice> findByCustomerIdOrderByBilledAtDesc(Long customerId);

    List<Invoice> findTop10ByOrderByBilledAtDesc();

    List<Invoice> findByStatusNot(String status);

    @Query("""
            select i from Invoice i
            where (:status = '' or i.status = :status)
              and (:q = '' or lower(i.invoiceNo) like lower(concat('%', :q, '%')))
            order by i.billedAt desc
            """)
    List<Invoice> search(@Param("q") String q, @Param("status") String status);
}
