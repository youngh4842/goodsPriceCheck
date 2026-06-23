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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT_MALL_ITEM", uniqueConstraints = {
		@UniqueConstraint(name = "uk_product_mall_item_url", columnNames = {"mall_name", "product_url"})
})
public class ProductMallItem {

	@Id
	@Column(name = "mall_item_id")
	private Long mallItemId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductMaster productMaster;

	@Column(name = "mall_name")
	private String mallName;

	@Column(name = "mall_product_name", length = 1000)
	private String mallProductName;

	@Column(name = "mall_product_code")
	private String mallProductCode;

	@Column(name = "normalized_product_name", length = 1000)
	private String normalizedProductName;

	@Column(name = "product_url", length = 2000)
	private String productUrl;

	private Long price;

	@Column(name = "price_text")
	private String priceText;

	@Enumerated(EnumType.STRING)
	@Column(name = "sale_type", nullable = false)
	private SaleType saleType;

	@Enumerated(EnumType.STRING)
	@Column(name = "match_status", nullable = false)
	private MatchStatus matchStatus;

	@Column(name = "match_score", nullable = false)
	private int matchScore;

	@Column(name = "match_reason", length = 2000)
	private String matchReason;

	@Column(name = "manually_confirmed_yn", length = 1, nullable = false)
	private String manuallyConfirmedYn = "N";

	@Column(name = "active_yn", length = 1, nullable = false)
	private String activeYn = "Y";

	@Column(name = "last_crawled_at")
	private LocalDateTime lastCrawledAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected ProductMallItem() {
	}

	public ProductMallItem(Long mallItemId, ProductMaster productMaster, String mallName, String mallProductName,
			String mallProductCode, String normalizedProductName, String productUrl, Long price, String priceText,
			SaleType saleType, MatchStatus matchStatus, int matchScore, String matchReason) {
		this.mallItemId = mallItemId;
		this.productMaster = productMaster;
		this.mallName = mallName;
		this.mallProductName = mallProductName;
		this.mallProductCode = mallProductCode;
		this.normalizedProductName = normalizedProductName;
		this.productUrl = productUrl;
		this.price = price;
		this.priceText = priceText;
		this.saleType = saleType;
		this.matchStatus = matchStatus;
		this.matchScore = matchScore;
		this.matchReason = matchReason;
		this.lastCrawledAt = LocalDateTime.now();
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public void refresh(String mallProductName, String mallProductCode, String normalizedProductName, Long price,
			String priceText, SaleType saleType, MatchStatus matchStatus, int matchScore, String matchReason) {
		this.mallProductName = mallProductName;
		this.mallProductCode = mallProductCode;
		this.normalizedProductName = normalizedProductName;
		this.saleType = saleType;
		this.matchStatus = matchStatus;
		this.matchScore = matchScore;
		this.matchReason = matchReason;
		this.activeYn = "Y";
		updatePrice(price, priceText);
	}

	public void updatePrice(Long price, String priceText) {
		this.price = price;
		this.priceText = priceText;
		this.lastCrawledAt = LocalDateTime.now();
	}

	public void changeMatchStatus(MatchStatus matchStatus, boolean manualConfirmed) {
		this.matchStatus = matchStatus;
		this.manuallyConfirmedYn = manualConfirmed ? "Y" : "N";
		this.matchReason = appendReason(this.matchReason, "사용자가 검증 상태를 " + matchStatus + "(으)로 변경했습니다.");
	}

	public void deactivateTracking() {
		this.activeYn = "N";
	}

	private String appendReason(String before, String reason) {
		if (before == null || before.isBlank()) {
			return reason;
		}
		return before + " / " + reason;
	}

	public Long getMallItemId() {
		return mallItemId;
	}

	public MatchStatus getMatchStatus() {
		return matchStatus;
	}

	public String getManuallyConfirmedYn() {
		return manuallyConfirmedYn;
	}

	public ProductMaster getProductMaster() {
		return productMaster;
	}

	public String getMallName() {
		return mallName;
	}

	public String getNormalizedProductName() {
		return normalizedProductName;
	}

	public String getMallProductCode() {
		return mallProductCode;
	}

	public String getProductUrl() {
		return productUrl;
	}

	public Long getPrice() {
		return price;
	}

	public String getPriceText() {
		return priceText;
	}

	public String getMallProductName() {
		return mallProductName;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public LocalDateTime getLastCrawledAt() {
		return lastCrawledAt;
	}
}
