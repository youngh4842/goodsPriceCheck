package com.example.goodsprice.repository;

import com.example.goodsprice.domain.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
}
