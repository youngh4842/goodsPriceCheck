package com.example.goodsprice.product;

public record RefreshTrackedProductResponse(
		Long productId,
		Long crawlRunId,
		int targetCount,
		int successCount,
		int failureCount,
		int changedCount,
		String message
) {
}
