package com.example.goodsprice.product;

public record MallItemStatusResponse(
		Long mallItemId,
		MatchStatus matchStatus,
		String manualConfirmedYn
) {
}
