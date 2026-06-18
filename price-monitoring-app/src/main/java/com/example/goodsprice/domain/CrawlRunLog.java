package com.example.goodsprice.domain;

import com.example.goodsprice.product.CrawlRunStatus;
import com.example.goodsprice.product.PriceHistorySourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "CRAWL_RUN_LOG")
public class CrawlRunLog {

	@Id
	@Column(name = "crawl_run_id")
	private Long crawlRunId;

	@Enumerated(EnumType.STRING)
	@Column(name = "run_type", nullable = false)
	private PriceHistorySourceType runType;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "ended_at")
	private LocalDateTime endedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private CrawlRunStatus status = CrawlRunStatus.RUNNING;

	@Column(name = "target_count", nullable = false)
	private int targetCount;

	@Column(name = "success_count", nullable = false)
	private int successCount;

	@Column(name = "failure_count", nullable = false)
	private int failureCount;

	@Column(name = "changed_count", nullable = false)
	private int changedCount;

	@Column(name = "error_message", length = 2000)
	private String errorMessage;

	protected CrawlRunLog() {
	}

	public CrawlRunLog(Long crawlRunId, PriceHistorySourceType runType, int targetCount) {
		this.crawlRunId = crawlRunId;
		this.runType = runType;
		this.targetCount = targetCount;
		this.startedAt = LocalDateTime.now();
	}

	@PrePersist
	void prePersist() {
		if (this.startedAt == null) {
			this.startedAt = LocalDateTime.now();
		}
	}

	public void finish(int successCount, int failureCount, int changedCount, String errorMessage) {
		this.successCount = successCount;
		this.failureCount = failureCount;
		this.changedCount = changedCount;
		this.errorMessage = errorMessage;
		this.endedAt = LocalDateTime.now();
		if (failureCount == 0) {
			this.status = CrawlRunStatus.SUCCESS;
		}
		else if (successCount > 0) {
			this.status = CrawlRunStatus.PARTIAL_SUCCESS;
		}
		else {
			this.status = CrawlRunStatus.FAILED;
		}
	}

	public Long getCrawlRunId() {
		return crawlRunId;
	}
}
