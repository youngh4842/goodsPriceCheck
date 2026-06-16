package com.example.goodsprice.crawler;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HsmoaSelectors {

	private final List<String> searchInputs = List.of(
			"input[name='q']",
			"input[placeholder*='검색']",
			"form input[type='text']"
	);

	private final List<String> resultAreas = List.of(
			"#__NEXT_DATA__",
			"#__next",
			"[data-sentry-component='SearchResultTabs']"
	);

	public List<String> searchInputs() {
		return searchInputs;
	}

	public List<String> resultAreas() {
		return resultAreas;
	}
}
