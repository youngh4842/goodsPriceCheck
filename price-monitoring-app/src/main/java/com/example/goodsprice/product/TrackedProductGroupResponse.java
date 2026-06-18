package com.example.goodsprice.product;

import java.util.List;

public record TrackedProductGroupResponse(
		String keyword,
		List<TrackedProductItemResponse> items
) {
}
