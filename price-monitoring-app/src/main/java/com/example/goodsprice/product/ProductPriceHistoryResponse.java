package com.example.goodsprice.product;

import java.util.List;

public record ProductPriceHistoryResponse(
		Long productId,
		String productCode,
		String productName,
		List<PriceHistoryItemResponse> items
) {
}
