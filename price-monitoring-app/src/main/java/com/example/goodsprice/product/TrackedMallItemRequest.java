package com.example.goodsprice.product;

public record TrackedMallItemRequest(
		String mallName,
		String mallProductName,
		String productCode,
		String normalizedProductName,
		String productUrl,
		Long price,
		String priceText,
		SaleType saleType,
		MatchStatus matchStatus,
		Integer matchScore,
		java.util.List<String> matchReasons
) {
}
