package com.example.goodsprice.product;

import java.math.BigDecimal;

public record PriceHistoryResult(
		Long mallItemId,
		Long previousPrice,
		Long currentPrice,
		boolean changed,
		Long changeAmount,
		BigDecimal changeRate,
		boolean newLowestPrice,
		boolean historyCreated
) {
}
