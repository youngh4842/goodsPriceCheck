package com.example.goodsprice.crawler;

public enum CrawlerSource {
	HSMOA("홈쇼핑"),
	MOCK_MALL("TEST");

	private final String label;

	CrawlerSource(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
