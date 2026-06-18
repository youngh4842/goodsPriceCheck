package com.example.goodsprice.product;

import java.time.LocalDateTime;
import java.util.List;

public record ProductPriceSearchResponse(
		String keyword,
		LocalDateTime searchedAt,
		boolean found,
		int resultCount,
		Integer sourceTotalCount,
		List<ProductPriceItemResponse> results,
		String message
) {
}
