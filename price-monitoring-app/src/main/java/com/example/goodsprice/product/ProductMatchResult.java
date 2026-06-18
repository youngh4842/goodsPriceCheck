package com.example.goodsprice.product;

import java.util.List;

public record ProductMatchResult(
		SaleType saleType,
		MatchStatus matchStatus,
		int matchScore,
		List<String> matchReasons
) {
}
