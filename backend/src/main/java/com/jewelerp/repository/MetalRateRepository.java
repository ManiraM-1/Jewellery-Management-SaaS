package com.jewelerp.repository;

import com.jewelerp.model.MetalRate;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MetalRateRepository extends JpaRepository<MetalRate, Long> {

    @Query("""
            select r from MetalRate r
            where r.metalType = :metal and r.purity = :purity and r.status = 'approved'
              and r.effectiveAt <= :now
            order by r.effectiveAt desc, r.id desc
            """)
    List<MetalRate> findApproved(@Param("metal") String metal, @Param("purity") String purity,
                                 @Param("now") LocalDateTime now);

    List<MetalRate> findTop100ByOrderByEffectiveAtDescIdDesc();

    @Query("""
            select r from MetalRate r
            where r.metalType = 'Gold' and r.purity = '24K' and r.status = 'approved' and r.effectiveAt >= :from
            order by r.effectiveAt
            """)
    List<MetalRate> gold24History(@Param("from") LocalDateTime from);
}
