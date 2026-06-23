package com.example.mockmall.repository;

import com.example.mockmall.domain.MockPriceChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockPriceChangeLogRepository extends JpaRepository<MockPriceChangeLog, Long> {

	List<MockPriceChangeLog> findByMockProductMockProductIdOrderByChangedAtDescChangeLogIdDesc(Long mockProductId);
}
