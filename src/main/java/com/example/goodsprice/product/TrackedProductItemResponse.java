package com.example.goodsprice.product;

public record TrackedProductItemResponse(
		Long productId,
		Long mallItemId,
		String productCode,
		String productName,
		Long price,
		String priceText,
		String productUrl
) {
}
