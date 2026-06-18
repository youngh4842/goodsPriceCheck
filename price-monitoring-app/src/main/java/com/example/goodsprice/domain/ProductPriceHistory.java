package com.example.goodsprice.domain;

import com.example.goodsprice.product.PriceHistorySourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT_PRICE_HISTORY")
public class ProductPriceHistory {

	@Id
	@Column(name = "price_history_id")
	private Long priceHistoryId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "mall_item_id", nullable = false)
	private ProductMallItem mallItem;

	private Long price;

	@Column(name = "price_text")
	private String priceText;

	@Column(name = "previous_price")
	private Long previousPrice;

	@Column(name = "change_amount")
	private Long changeAmount;

	@Column(name = "change_rate", precision = 10, scale = 2)
	private BigDecimal changeRate;

	@Column(name = "lowest_price_yn", length = 1, nullable = false)
	private String lowestPriceYn;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false)
	private PriceHistorySourceType sourceType;

	@Column(name = "crawled_at", nullable = false)
	private LocalDateTime crawledAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected ProductPriceHistory() {
	}

	public ProductPriceHistory(Long priceHistoryId, ProductMallItem mallItem, Long price, String priceText,
			Long previousPrice, Long changeAmount, BigDecimal changeRate, boolean lowestPrice,
			PriceHistorySourceType sourceType, LocalDateTime crawledAt) {
		this.priceHistoryId = priceHistoryId;
		this.mallItem = mallItem;
		this.price = price;
		this.priceText = priceText;
		this.previousPrice = previousPrice;
		this.changeAmount = changeAmount;
		this.changeRate = changeRate;
		this.lowestPriceYn = lowestPrice ? "Y" : "N";
		this.sourceType = sourceType;
		this.crawledAt = crawledAt;
	}

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getPriceHistoryId() {
		return priceHistoryId;
	}

	public ProductMallItem getMallItem() {
		return mallItem;
	}

	public Long getPrice() {
		return price;
	}

	public String getPriceText() {
		return priceText;
	}

	public Long getPreviousPrice() {
		return previousPrice;
	}

	public Long getChangeAmount() {
		return changeAmount;
	}

	public BigDecimal getChangeRate() {
		return changeRate;
	}

	public String getLowestPriceYn() {
		return lowestPriceYn;
	}

	public PriceHistorySourceType getSourceType() {
		return sourceType;
	}

	public LocalDateTime getCrawledAt() {
		return crawledAt;
	}
}
