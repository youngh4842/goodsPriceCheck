package com.example.mockmall.web;

import com.example.mockmall.domain.MockProduct;

import java.time.LocalDateTime;

public record ProductResponse(
		String productCode,
		String productName,
		long price,
		LocalDateTime updatedAt
) {
	public static ProductResponse from(MockProduct product) {
		return new ProductResponse(product.getProductCode(), product.getProductName(), product.getPrice(),
				product.getUpdatedAt());
	}
}
