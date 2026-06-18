package com.example.goodsprice.repository;

import com.example.goodsprice.domain.CrawlRunLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlRunLogRepository extends JpaRepository<CrawlRunLog, Long> {
}
