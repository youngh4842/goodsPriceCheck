package com.example.mockmall.domain;

import java.time.LocalDateTime;

public class MockProduct {

	private final String productCode;
	private final String productName;
	private long price;
	private LocalDateTime updatedAt;

	public MockProduct(String productCode, String productName, long price, LocalDateTime updatedAt) {
		this.productCode = productCode;
		this.productName = productName;
		this.price = price;
		this.updatedAt = updatedAt;
	}

	public void changePrice(long price) {
		this.price = price;
		this.updatedAt = LocalDateTime.now();
	}

	public String getProductCode() {
		return productCode;
	}

	public String getProductName() {
		return productName;
	}

	public long getPrice() {
		return price;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
