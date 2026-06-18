package com.example.goodsprice.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceHistoryEntryResponse(
		Long price,
		String priceText,
		Long previousPrice,
		Long changeAmount,
		BigDecimal changeRate,
		String lowestPriceYn,
		PriceHistorySourceType sourceType,
		LocalDateTime crawledAt
) {
}
