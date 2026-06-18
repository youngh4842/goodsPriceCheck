package com.example.goodsprice.crawler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CrawlerSourceService {

	private volatile CrawlerSource currentSource;

	public CrawlerSourceService(@Value("${crawler.source:HSMOA}") String source) {
		this.currentSource = parse(source);
	}

	public CrawlerSource currentSource() {
		return currentSource;
	}

	public CrawlerSource changeSource(CrawlerSource source) {
		this.currentSource = source == null ? CrawlerSource.HSMOA : source;
		return this.currentSource;
	}

	private CrawlerSource parse(String source) {
		try {
			return CrawlerSource.valueOf(source);
		}
		catch (RuntimeException ex) {
			return CrawlerSource.HSMOA;
		}
	}
}
