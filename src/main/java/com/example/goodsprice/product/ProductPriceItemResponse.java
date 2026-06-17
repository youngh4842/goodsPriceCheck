package com.example.goodsprice.product;

public record ProductPriceItemResponse(
		String mallName,
		String productCode,
		String mallProductName,
		String productName,
		String rentalYn,
		String productPeriod,
		Long price,
		String priceText,
		String productUrl,
		SaleType saleType,
		MatchStatus matchStatus,
		int matchScore,
		java.util.List<String> matchReasons,
		boolean tracked,
		Long mallItemId,
		boolean rejected
) {
}
