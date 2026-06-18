package com.example.goodsprice.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TrackedProductRequest(
		String keyword,
		String productCode,
		@NotBlank(message = "상품명을 입력해 주세요.")
		String productName,
		String brandName,
		@Valid
		@NotEmpty(message = "추적 등록할 쇼핑몰 상품을 선택해 주세요.")
		List<TrackedMallItemRequest> selectedItems
) {
}
