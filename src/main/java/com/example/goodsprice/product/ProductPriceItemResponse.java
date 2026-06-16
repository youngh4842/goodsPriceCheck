package com.example.goodsprice.product;

public record ProductPriceItemResponse(
		String mallName,
		String productCode,
		String productName,
		String rentalYn,
		String productPeriod,
		Long price,
		String priceText,
		String productUrl
) {
}
