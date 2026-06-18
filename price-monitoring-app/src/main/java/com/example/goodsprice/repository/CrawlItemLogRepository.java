package com.example.goodsprice.repository;

import com.example.goodsprice.domain.CrawlItemLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlItemLogRepository extends JpaRepository<CrawlItemLog, Long> {
}
