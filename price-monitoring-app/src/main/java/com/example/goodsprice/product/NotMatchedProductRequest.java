package com.example.goodsprice.product;

import jakarta.validation.constraints.NotBlank;

public record NotMatchedProductRequest(
		@NotBlank(message = "검색어를 입력해 주세요.")
		String keyword,
		String mallName,
		String productCode,
		String productName,
		@NotBlank(message = "상품 링크를 확인할 수 없습니다.")
		String productUrl,
		Long price,
		String priceText,
		String reason
) {
}
