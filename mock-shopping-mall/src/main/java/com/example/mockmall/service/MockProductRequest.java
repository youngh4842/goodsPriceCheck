package com.example.mockmall.service;

import com.example.mockmall.domain.SaleStatus;
import com.example.mockmall.domain.SaleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MockProductRequest(
		@NotBlank String productCode,
		@NotBlank String productName,
		String brandName,
		@NotBlank String mallName,
		@NotNull @Positive Long price,
		SaleType saleType,
		SaleStatus saleStatus,
		String productDescription,
		String productImageUrl,
		String priceVisibleYn
) {
}
