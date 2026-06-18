package com.example.goodsprice.crawler;

public class CrawlerException extends RuntimeException {

	private final CrawlerFailureType failureType;

	public CrawlerException(CrawlerFailureType failureType, String detail) {
		super(detail);
		this.failureType = failureType;
	}

	public CrawlerException(CrawlerFailureType failureType, String detail, Throwable cause) {
		super(detail, cause);
		this.failureType = failureType;
	}

	public String getUserMessage() {
		return failureType.getUserMessage();
	}

	public CrawlerFailureType getFailureType() {
		return failureType;
	}
}
