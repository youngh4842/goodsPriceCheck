package com.example.goodsprice.crawler;

import java.util.Optional;

public interface ProductCrawler {

	CrawledSearchResult search(String keyword);

	default Optional<CrawledProduct> fetchProduct(String productUrl) {
		return Optional.empty();
	}
}
