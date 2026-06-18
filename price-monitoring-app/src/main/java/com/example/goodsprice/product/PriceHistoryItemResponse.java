package com.example.goodsprice.product;

import java.time.LocalDateTime;
import java.util.List;

public record PriceHistoryItemResponse(
		String mallName,
		Long mallItemId,
		String productCode,
		String productName,
		Long currentPrice,
		Long lowestPrice,
		LocalDateTime lastCrawledAt,
		List<PriceHistoryEntryResponse> history
) {
}
