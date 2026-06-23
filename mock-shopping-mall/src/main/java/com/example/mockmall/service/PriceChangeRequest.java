package com.example.mockmall.service;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PriceChangeRequest(
		@NotNull @Positive Long price,
		String changeReason
) {
}
