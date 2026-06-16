package com.example.goodsprice.crawler;

import java.util.List;

public record CrawledSearchResult(
		List<CrawledProduct> products,
		Integer sourceTotalCount
) {
}
