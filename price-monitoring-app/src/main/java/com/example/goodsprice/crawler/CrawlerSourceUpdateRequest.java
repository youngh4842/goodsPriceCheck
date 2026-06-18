package com.example.goodsprice.crawler;

import jakarta.validation.constraints.NotNull;

public record CrawlerSourceUpdateRequest(
		@NotNull CrawlerSource source
) {
}
