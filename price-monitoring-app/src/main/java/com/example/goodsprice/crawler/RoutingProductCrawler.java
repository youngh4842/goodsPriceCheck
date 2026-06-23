package com.example.goodsprice.crawler;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Primary
@Service
public class RoutingProductCrawler implements ProductCrawler {

	private final CrawlerSourceService crawlerSourceService;
	private final HsmoaPriceCrawler hsmoaPriceCrawler;
	private final MockMallPriceCrawler mockMallPriceCrawler;

	public RoutingProductCrawler(CrawlerSourceService crawlerSourceService, HsmoaPriceCrawler hsmoaPriceCrawler,
			MockMallPriceCrawler mockMallPriceCrawler) {
		this.crawlerSourceService = crawlerSourceService;
		this.hsmoaPriceCrawler = hsmoaPriceCrawler;
		this.mockMallPriceCrawler = mockMallPriceCrawler;
	}

	@Override
	public CrawledSearchResult search(String keyword) {
		return activeCrawler().search(keyword);
	}

	@Override
	public Optional<CrawledProduct> fetchProduct(String productUrl) {
		if (isMockMallUrl(productUrl)) {
			return mockMallPriceCrawler.fetchProduct(productUrl);
		}
		return activeCrawler().fetchProduct(productUrl);
	}

	private ProductCrawler activeCrawler() {
		return crawlerSourceService.currentSource() == CrawlerSource.MOCK_MALL ? mockMallPriceCrawler : hsmoaPriceCrawler;
	}

	private boolean isMockMallUrl(String productUrl) {
		if (productUrl == null || productUrl.isBlank()) {
			return false;
		}
		return productUrl.contains("localhost:8090") || productUrl.contains("127.0.0.1:8090");
	}
}
