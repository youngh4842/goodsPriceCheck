package com.example.goodsprice.domain;

import com.example.goodsprice.product.MatchStatus;
import com.example.goodsprice.product.SaleType;
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
@Table(name = "TB_PRODUCT_PRICE_RESULT")
public class ProductPriceResult {

	@Id
	@Column(name = "result_id")
	private Long resultId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "search_id", nullable = false)
	private SearchLog searchLog;

	@Column(name = "mall_name")
	private String mallName;

	@Column(name = "product_code")
	private String productCode;

	@Column(name = "product_name", length = 1000)
	private String productName;

	@Column(name = "product_period")
	private String productPeriod;

	private Long price;

	@Column(name = "price_text")
	private String priceText;

	@Column(name = "product_url", length = 2000)
	private String productUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "sale_type")
	private SaleType saleType;

	@Enumerated(EnumType.STRING)
	@Column(name = "match_status")
	private MatchStatus matchStatus;

	@Column(name = "match_score")
	private Integer matchScore;

	@Column(name = "match_reason", length = 2000)
	private String matchReason;

	@Column(name = "crawled_at", nullable = false)
	private LocalDateTime crawledAt;

	protected ProductPriceResult() {
	}

	public ProductPriceResult(String mallName, String productCode, String productName, String productPeriod, Long price,
			String priceText, String productUrl, SaleType saleType, MatchStatus matchStatus, Integer matchScore,
			String matchReason, LocalDateTime crawledAt) {
		this.mallName = mallName;
		this.productCode = productCode;
		this.productName = productName;
		this.productPeriod = productPeriod;
		this.price = price;
		this.priceText = priceText;
		this.productUrl = productUrl;
		this.saleType = saleType;
		this.matchStatus = matchStatus;
		this.matchScore = matchScore;
		this.matchReason = matchReason;
		this.crawledAt = crawledAt;
	}

	public void assignResultId(Long resultId) {
		this.resultId = resultId;
	}

	void assignSearchLog(SearchLog searchLog) {
		this.searchLog = searchLog;
	}
}
