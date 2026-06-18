package com.example.goodsprice.domain;

import com.example.goodsprice.product.CrawlItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "CRAWL_ITEM_LOG")
public class CrawlItemLog {

	@Id
	@Column(name = "crawl_item_log_id")
	private Long crawlItemLogId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "crawl_run_id", nullable = false)
	private CrawlRunLog crawlRunLog;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mall_item_id")
	private ProductMallItem mallItem;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private CrawlItemStatus status;

	@Column(name = "previous_price")
	private Long previousPrice;

	@Column(name = "current_price")
	private Long currentPrice;

	@Column(name = "price_changed_yn", length = 1, nullable = false)
	private String priceChangedYn;

	@Column(name = "error_message", length = 2000)
	private String errorMessage;

	@Column(name = "crawled_at", nullable = false)
	private LocalDateTime crawledAt;

	protected CrawlItemLog() {
	}

	public CrawlItemLog(Long crawlItemLogId, CrawlRunLog crawlRunLog, ProductMallItem mallItem,
			CrawlItemStatus status, Long previousPrice, Long currentPrice, boolean priceChanged, String errorMessage,
			LocalDateTime crawledAt) {
		this.crawlItemLogId = crawlItemLogId;
		this.crawlRunLog = crawlRunLog;
		this.mallItem = mallItem;
		this.status = status;
		this.previousPrice = previousPrice;
		this.currentPrice = currentPrice;
		this.priceChangedYn = priceChanged ? "Y" : "N";
		this.errorMessage = errorMessage;
		this.crawledAt = crawledAt == null ? LocalDateTime.now() : crawledAt;
	}
}
