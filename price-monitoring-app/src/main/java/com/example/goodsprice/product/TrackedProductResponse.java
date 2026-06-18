package com.example.goodsprice.product;

import java.util.List;

public record TrackedProductResponse(
		Long productId,
		List<Long> mallItemIds,
		int registeredCount,
		String message
) {
}
