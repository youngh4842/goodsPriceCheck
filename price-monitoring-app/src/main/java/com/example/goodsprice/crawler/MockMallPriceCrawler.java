package com.example.goodsprice.crawler;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MockMallPriceCrawler implements ProductCrawler {

	private final PriceParser priceParser;
	private final String baseUrl;
	private final boolean headless;
	private final int timeoutMillis;
	private final int maxResults;

	public MockMallPriceCrawler(PriceParser priceParser,
			@Value("${crawler.mock-mall.base-url:http://localhost:8090}") String baseUrl,
			@Value("${crawler.headless:true}") boolean headless,
			@Value("${crawler.timeout-ms:10000}") int timeoutMillis,
			@Value("${crawler.max-results:0}") int maxResults) {
		this.priceParser = priceParser;
		this.baseUrl = trimTrailingSlash(baseUrl);
		this.headless = headless;
		this.timeoutMillis = timeoutMillis;
		this.maxResults = maxResults;
	}

	@Override
	public CrawledSearchResult search(String keyword) {
		try (Playwright playwright = Playwright.create();
				Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless))) {
			Page page = browser.newPage();
			page.setDefaultTimeout(timeoutMillis);
			page.setDefaultNavigationTimeout(timeoutMillis);
			page.navigate(buildSearchUrl(keyword), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
			Locator items = page.locator("[data-testid='search-result-item']");
			int count = items.count();
			List<CrawledProduct> products = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				if (maxResults > 0 && products.size() >= maxResults) {
					break;
				}
				products.add(toProduct(items.nth(i)));
			}
			return new CrawledSearchResult(products, count);
		}
		catch (TimeoutError ex) {
			throw new CrawlerException(CrawlerFailureType.TIMEOUT, "Mock 쇼핑몰 검색 시간이 초과되었습니다.", ex);
		}
		catch (PlaywrightException ex) {
			throw new CrawlerException(CrawlerFailureType.SITE_ACCESS_FAILED,
					"Mock 쇼핑몰에 접속할 수 없습니다: " + ex.getMessage(), ex);
		}
	}

	@Override
	public Optional<CrawledProduct> fetchProduct(String productUrl) {
		if (productUrl == null || productUrl.isBlank()) {
			return Optional.empty();
		}
		try (Playwright playwright = Playwright.create();
				Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless))) {
			Page page = browser.newPage();
			page.setDefaultTimeout(timeoutMillis);
			page.setDefaultNavigationTimeout(timeoutMillis);
			page.navigate(resolveUrl(productUrl), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
			return Optional.of(toDetailProduct(page, resolveUrl(productUrl)));
		}
		catch (TimeoutError ex) {
			throw new CrawlerException(CrawlerFailureType.TIMEOUT, "Mock 쇼핑몰 상세 조회 시간이 초과되었습니다.", ex);
		}
		catch (PlaywrightException ex) {
			throw new CrawlerException(CrawlerFailureType.SITE_ACCESS_FAILED,
					"Mock 쇼핑몰 상세 페이지에 접속할 수 없습니다: " + ex.getMessage(), ex);
		}
	}

	private CrawledProduct toProduct(Locator item) {
		String mallName = text(item, "[data-testid='mall-name']");
		String productCode = text(item, "[data-testid='product-code']");
		String productName = text(item, "[data-testid='product-name']");
		String priceText = text(item, "[data-testid='product-price']");
		String url = item.locator("[data-testid='product-link']").first().getAttribute("href");
		return new CrawledProduct(mallName, productCode, productName, priceParser.parse(priceText), priceText, resolveUrl(url),
				LocalDateTime.now());
	}

	private CrawledProduct toDetailProduct(Page page, String productUrl) {
		String mallName = text(page, "[data-testid='mall-name']");
		String productCode = text(page, "[data-testid='product-code']");
		String productName = text(page, "[data-testid='product-name']");
		String priceText = text(page, "[data-testid='product-price']");
		return new CrawledProduct(mallName, productCode, productName, priceParser.parse(priceText), priceText, productUrl,
				LocalDateTime.now());
	}

	private String text(Locator root, String selector) {
		Locator locator = root.locator(selector).first();
		return locator.count() == 0 ? null : blankToNull(locator.textContent());
	}

	private String text(Page page, String selector) {
		Locator locator = page.locator(selector).first();
		return locator.count() == 0 ? null : blankToNull(locator.textContent());
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String buildSearchUrl(String keyword) {
		return UriComponentsBuilder.fromHttpUrl(baseUrl)
				.path("/search")
				.queryParam("keyword", keyword)
				.build()
				.encode()
				.toUriString();
	}

	private String resolveUrl(String url) {
		if (url == null || url.isBlank()) {
			return baseUrl;
		}
		URI uri = URI.create(url);
		if (uri.isAbsolute()) {
			return url;
		}
		return baseUrl + (url.startsWith("/") ? url : "/" + url);
	}

	private String trimTrailingSlash(String value) {
		if (value == null || value.isBlank()) {
			return "http://localhost:8090";
		}
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
