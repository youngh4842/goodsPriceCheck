package com.example.mockmall.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "MOCK_PRODUCT")
public class MockProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "mock_product_id")
	private Long mockProductId;

	@Column(name = "product_code", nullable = false)
	private String productCode;

	@Column(name = "normalized_product_code", nullable = false)
	private String normalizedProductCode;

	@Column(name = "product_name", nullable = false, length = 500)
	private String productName;

	@Column(name = "brand_name")
	private String brandName;

	@Column(name = "mall_name", nullable = false)
	private String mallName;

	@Column(name = "price", nullable = false)
	private Long price;

	@Column(name = "price_text")
	private String priceText;

	@Enumerated(EnumType.STRING)
	@Column(name = "sale_type", nullable = false)
	private SaleType saleType;

	@Enumerated(EnumType.STRING)
	@Column(name = "sale_status", nullable = false)
	private SaleStatus saleStatus;

	@Column(name = "product_description", length = 2000)
	private String productDescription;

	@Column(name = "product_image_url", length = 1000)
	private String productImageUrl;

	@Column(name = "price_visible_yn", length = 1, nullable = false)
	private String priceVisibleYn = "Y";

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected MockProduct() {
	}

	public MockProduct(String productCode, String normalizedProductCode, String productName, String brandName,
			String mallName, Long price, SaleType saleType, SaleStatus saleStatus, String productDescription,
			String productImageUrl) {
		this.productCode = productCode;
		this.normalizedProductCode = normalizedProductCode;
		this.productName = productName;
		this.brandName = brandName;
		this.mallName = mallName;
		this.price = price;
		this.priceText = formatPrice(price);
		this.saleType = saleType;
		this.saleStatus = saleStatus;
		this.productDescription = productDescription;
		this.productImageUrl = productImageUrl;
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

	public void update(String productCode, String normalizedProductCode, String productName, String brandName,
			String mallName, Long price, SaleType saleType, SaleStatus saleStatus, String productDescription,
			String productImageUrl, String priceVisibleYn) {
		this.productCode = productCode;
		this.normalizedProductCode = normalizedProductCode;
		this.productName = productName;
		this.brandName = brandName;
		this.mallName = mallName;
		this.price = price;
		this.priceText = formatPrice(price);
		this.saleType = saleType;
		this.saleStatus = saleStatus;
		this.productDescription = productDescription;
		this.productImageUrl = productImageUrl;
		this.priceVisibleYn = priceVisibleYn == null || priceVisibleYn.isBlank() ? "Y" : priceVisibleYn;
	}

	public void changePrice(Long price) {
		this.price = price;
		this.priceText = formatPrice(price);
	}

	public void changeSaleStatus(SaleStatus saleStatus) {
		this.saleStatus = saleStatus;
	}

	public void changePriceVisibleYn(String priceVisibleYn) {
		this.priceVisibleYn = "N".equalsIgnoreCase(priceVisibleYn) ? "N" : "Y";
	}

	private String formatPrice(Long price) {
		return price == null ? "" : String.format("%,d원", price);
	}

	public Long getMockProductId() {
		return mockProductId;
	}

	public String getProductCode() {
		return productCode;
	}

	public String getNormalizedProductCode() {
		return normalizedProductCode;
	}

	public String getProductName() {
		return productName;
	}

	public String getBrandName() {
		return brandName;
	}

	public String getMallName() {
		return mallName;
	}

	public Long getPrice() {
		return price;
	}

	public String getPriceText() {
		return priceText;
	}

	public SaleType getSaleType() {
		return saleType;
	}

	public SaleStatus getSaleStatus() {
		return saleStatus;
	}

	public String getProductDescription() {
		return productDescription;
	}

	public String getProductImageUrl() {
		return productImageUrl;
	}

	public String getPriceVisibleYn() {
		return priceVisibleYn;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
