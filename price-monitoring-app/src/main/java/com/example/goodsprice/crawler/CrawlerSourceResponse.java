package com.example.goodsprice.crawler;

public record CrawlerSourceResponse(
		CrawlerSource source,
		String label
) {
	public static CrawlerSourceResponse from(CrawlerSource source) {
		return new CrawlerSourceResponse(source, source.getLabel());
	}
}
