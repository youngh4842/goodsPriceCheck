package com.example.goodsprice.product;

import java.time.LocalDateTime;

public record TrackedProductItemResponse(
		Long productId,
		Long mallItemId,
		String mallName,
		String productCode,
		String productName,
		Long price,
		String priceText,
		Long firstPrice,
		Long previousPrice,
		Long changeAmount,
		java.math.BigDecimal changeRate,
		Long lowestPrice,
		LocalDateTime lastPriceChangedAt,
		LocalDateTime lastCrawledAt,
		String activeYn,
		String productUrl
) {
}
