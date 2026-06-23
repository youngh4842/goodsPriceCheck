package com.example.mockmall.web;

import com.example.mockmall.domain.MockProduct;
import com.example.mockmall.domain.SaleStatus;
import com.example.mockmall.domain.SaleType;

public record MockProductResponse(
		Long mockProductId,
		String productCode,
		String normalizedProductCode,
		String productName,
		String brandName,
		String mallName,
		Long price,
		String priceText,
		SaleType saleType,
		SaleStatus saleStatus,
		String productDescription,
		String productImageUrl,
		String priceVisibleYn,
		String productUrl
) {
	static MockProductResponse from(MockProduct product) {
		return new MockProductResponse(product.getMockProductId(), product.getProductCode(),
				product.getNormalizedProductCode(), product.getProductName(), product.getBrandName(), product.getMallName(),
				product.getPrice(), product.getPriceText(), product.getSaleType(), product.getSaleStatus(),
				product.getProductDescription(), product.getProductImageUrl(), product.getPriceVisibleYn(),
				"/products/" + product.getMockProductId());
	}
}
