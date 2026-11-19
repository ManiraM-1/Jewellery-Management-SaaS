package com.jewelerp.repository;

import com.jewelerp.model.Counter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CounterRepository extends JpaRepository<Counter, Long> {

    List<Counter> findByActiveTrue();
}
