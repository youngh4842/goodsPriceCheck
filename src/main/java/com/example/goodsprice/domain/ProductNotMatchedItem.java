package com.example.goodsprice.domain;

import com.example.goodsprice.product.MatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT_NOT_MATCHED_ITEM", uniqueConstraints = {
		@UniqueConstraint(name = "uk_not_matched_keyword_url", columnNames = {"keyword", "mall_name", "product_url"})
})
public class ProductNotMatchedItem {

	@Id
	@Column(name = "not_matched_id")
	private Long notMatchedId;

	@Column(name = "keyword", nullable = false)
	private String keyword;

	@Column(name = "mall_name")
	private String mallName;

	@Column(name = "mall_product_code")
	private String mallProductCode;

	@Column(name = "product_name", length = 1000)
	private String productName;

	@Column(name = "product_url", length = 2000)
	private String productUrl;

	private Long price;

	@Column(name = "price_text")
	private String priceText;

	@Column(name = "reason", length = 1000)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(name = "match_status", nullable = false)
	private MatchStatus matchStatus = MatchStatus.NOT_MATCHED;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected ProductNotMatchedItem() {
	}

	public ProductNotMatchedItem(Long notMatchedId, String keyword, String mallName, String mallProductCode,
			String productName, String productUrl, Long price, String priceText, String reason) {
		this.notMatchedId = notMatchedId;
		this.keyword = keyword;
		this.mallName = mallName;
		this.mallProductCode = mallProductCode;
		this.productName = productName;
		this.productUrl = productUrl;
		this.price = price;
		this.priceText = priceText;
		this.reason = reason;
		this.matchStatus = MatchStatus.NOT_MATCHED;
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

	public void markNotMatched(String reason) {
		this.matchStatus = MatchStatus.NOT_MATCHED;
		this.reason = reason;
	}

	public void markPossibleMatch() {
		this.matchStatus = MatchStatus.POSSIBLE_MATCH;
		this.reason = "사용자가 재확인 대상으로 변경했습니다.";
	}

	public Long getNotMatchedId() {
		return notMatchedId;
	}

	public String getKeyword() {
		return keyword;
	}

	public String getMallName() {
		return mallName;
	}

	public String getMallProductCode() {
		return mallProductCode;
	}

	public String getProductUrl() {
		return productUrl;
	}

	public MatchStatus getMatchStatus() {
		return matchStatus;
	}
}
