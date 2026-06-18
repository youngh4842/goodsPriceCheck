package com.example.goodsprice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT_MASTER", uniqueConstraints = {
		@UniqueConstraint(name = "uk_product_master_code", columnNames = "product_code")
})
public class ProductMaster {

	@Id
	@Column(name = "product_id")
	private Long productId;

	@Column(name = "product_code")
	private String productCode;

	@Column(name = "search_keyword")
	private String searchKeyword;

	@Column(name = "product_name", nullable = false, length = 1000)
	private String productName;

	@Column(name = "brand_name")
	private String brandName;

	@Column(name = "active_yn", length = 1, nullable = false)
	private String activeYn = "Y";

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected ProductMaster() {
	}

	public ProductMaster(Long productId, String productCode, String searchKeyword, String productName, String brandName) {
		this.productId = productId;
		this.productCode = productCode;
		this.searchKeyword = searchKeyword;
		this.productName = productName;
		this.brandName = brandName;
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

	public Long getProductId() {
		return productId;
	}

	public String getProductCode() {
		return productCode;
	}

	public String getSearchKeyword() {
		return searchKeyword;
	}

	public String getProductName() {
		return productName;
	}

	public void updateBaseInfo(String searchKeyword, String productName, String brandName) {
		if (searchKeyword != null && !searchKeyword.isBlank()) {
			this.searchKeyword = searchKeyword;
		}
		if (productName != null && !productName.isBlank()) {
			this.productName = productName;
		}
		if (brandName != null && !brandName.isBlank()) {
			this.brandName = brandName;
		}
		this.activeYn = "Y";
	}
}
