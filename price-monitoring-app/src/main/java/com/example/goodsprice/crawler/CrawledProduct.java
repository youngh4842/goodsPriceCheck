package com.example.goodsprice.crawler;

import java.time.LocalDateTime;

public record CrawledProduct(
		String mallName,
		String productCode,
		String productName,
		Long price,
		String priceText,
		String productUrl,
		LocalDateTime crawledAt
) {
}
