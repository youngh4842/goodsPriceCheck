package com.example.goodsprice.product;

import jakarta.validation.constraints.NotNull;

public record MatchStatusUpdateRequest(
		@NotNull(message = "검증 상태를 입력해 주세요.")
		MatchStatus matchStatus,
		boolean manualConfirmed
) {
}
