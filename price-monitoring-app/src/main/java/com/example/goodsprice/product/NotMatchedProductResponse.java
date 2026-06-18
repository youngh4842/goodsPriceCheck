package com.example.goodsprice.product;

public record NotMatchedProductResponse(
		Long notMatchedId,
		MatchStatus matchStatus,
		String message
) {
}
