package com.example.goodsprice.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class HsmoaPriceCrawler implements ProductCrawler {

	private static final Logger log = LoggerFactory.getLogger(HsmoaPriceCrawler.class);
	private static final int EP_FETCH_LIMIT = 100;
	private static final String HSMOA_SEARCH_API = "https://api.hsmoa.net/v2/search/ep";
	private static final List<String> EP_SITE_FILTERS = List.of(
			"cjmall", "gsshop", "hmall", "ssgshop", "bshop", "nsmall", "hnsmall");

	private final HsmoaSelectors selectors;
	private final PriceParser priceParser;
	private final ObjectMapper objectMapper;
	private final String baseUrl;
	private final boolean headless;
	private final int timeoutMillis;
	private final boolean directSearchFallback;
	private final int maxResults;

	public HsmoaPriceCrawler(HsmoaSelectors selectors, PriceParser priceParser, ObjectMapper objectMapper,
			@Value("${crawler.hsmoa.base-url:https://hsmoa.com}") String baseUrl,
			@Value("${crawler.headless:true}") boolean headless,
			@Value("${crawler.timeout-ms:10000}") int timeoutMillis,
			@Value("${crawler.allow-direct-search-fallback:true}") boolean directSearchFallback,
			@Value("${crawler.max-results:0}") int maxResults) {
		this.selectors = selectors;
		this.priceParser = priceParser;
		this.objectMapper = objectMapper;
		this.baseUrl = baseUrl;
		this.headless = headless;
		this.timeoutMillis = timeoutMillis;
		this.directSearchFallback = directSearchFallback;
		this.maxResults = maxResults;
	}

	@Override
	public CrawledSearchResult search(String keyword) {
		String searchUrl = buildSearchUrl(keyword);
		try {
			CrawledSearchResult result = searchByEpApi(keyword);
			log.info("hsmoa ep api search completed. keyword={}, sourceTotal={}, count={}", keyword,
					result.sourceTotalCount(), result.products().size());
			return result;
		}
		catch (CrawlerException ex) {
			log.warn("Hsmoa ep api search failed. keyword={}, reason={}", keyword, ex.getMessage());
		}

		try {
			CrawledSearchResult result = searchByHttpClient(searchUrl, keyword);
			log.info("hsmoa http search completed. keyword={}, count={}", keyword, result.products().size());
			return result;
		}
		catch (CrawlerException ex) {
			log.warn("Fast hsmoa http search failed. keyword={}, reason={}", keyword, ex.getMessage());
		}

		try {
			CrawledSearchResult result = searchByPlaywrightRequest(searchUrl, keyword);
			log.info("hsmoa request search completed. keyword={}, count={}", keyword, result.products().size());
			return result;
		}
		catch (CrawlerException ex) {
			log.warn("Fast hsmoa request search failed. keyword={}, reason={}", keyword, ex.getMessage());
		}

		try (Playwright playwright = Playwright.create();
				Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless))) {
			Page page = browser.newPage();
			page.setDefaultTimeout(timeoutMillis);
			page.setDefaultNavigationTimeout(timeoutMillis);
			openSearchPage(page, keyword);
			waitForResultArea(page);
			String nextData = page.locator("#__NEXT_DATA__").textContent();
			CrawledSearchResult result = extractProducts(nextData, keyword);
			log.info("hsmoa browser search completed. keyword={}, count={}", keyword, result.products().size());
			return result;
		}
		catch (TimeoutError ex) {
			throw new CrawlerException(CrawlerFailureType.TIMEOUT, "Playwright timeout while searching hsmoa", ex);
		}
		catch (CrawlerException ex) {
			throw ex;
		}
		catch (PlaywrightException ex) {
			throw new CrawlerException(CrawlerFailureType.SITE_ACCESS_FAILED, "Failed to access hsmoa: " + ex.getMessage(), ex);
		}
		catch (Exception ex) {
			throw new CrawlerException(CrawlerFailureType.UNKNOWN, "Unexpected crawling error: " + ex.getMessage(), ex);
		}
	}

	private CrawledSearchResult searchByEpApi(String keyword) {
		try {
			Map<String, CrawledProduct> unique = new LinkedHashMap<>();
			JsonNode root = fetchEpApi(keyword, "");
			collectProductNodes(root.path("results"), unique, keyword);
			collectAggregatedSearchPage(keyword, unique);

			for (String site : EP_SITE_FILTERS) {
				if (reachedMaxResults(unique)) {
					break;
				}
				try {
					JsonNode siteRoot = fetchEpApi(keyword, site);
					collectProductNodes(siteRoot.path("results"), unique, keyword);
				}
				catch (CrawlerException ex) {
					log.debug("EP site filter failed. keyword={}, site={}, reason={}", keyword, site, ex.getMessage());
				}
			}

			Integer sourceTotalCount = unique.size();
			return new CrawledSearchResult(limitResults(unique), sourceTotalCount);
		}
		catch (Exception ex) {
			throw new CrawlerException(CrawlerFailureType.RESULT_AREA_NOT_FOUND, "Failed to parse EP API result", ex);
		}
	}

	private void collectAggregatedSearchPage(String keyword, Map<String, CrawledProduct> unique) {
		try {
			JsonNode root = objectMapper.readTree(extractNextData(fetchText(buildSearchUrl(keyword))));
			JsonNode aggregatedData = root.path("props").path("pageProps").path("aggregatedData");
			if (aggregatedData.isMissingNode() || aggregatedData.isNull()) {
				return;
			}
			collectSearchPageSections(aggregatedData, unique, keyword);
		}
		catch (Exception ex) {
			log.debug("Aggregated search page merge failed. keyword={}, reason={}", keyword, ex.getMessage());
		}
	}

	private JsonNode fetchEpApi(String keyword, String site) {
		String apiUrl = UriComponentsBuilder.fromHttpUrl(HSMOA_SEARCH_API)
				.queryParam("query", keyword)
				.queryParam("offset", 0)
				.queryParam("limit", EP_FETCH_LIMIT)
				.queryParam("order", "rel")
				.queryParam("site", site)
				.queryParam("platform", "web")
				.build()
				.encode()
				.toUriString();
		String body = fetchText(apiUrl);
		try {
			return objectMapper.readTree(body);
		}
		catch (Exception ex) {
			throw new CrawlerException(CrawlerFailureType.RESULT_AREA_NOT_FOUND, "Failed to parse EP API result", ex);
		}
	}

	private CrawledSearchResult searchByHttpClient(String searchUrl, String keyword) {
		return extractProducts(extractNextData(fetchText(searchUrl)), keyword);
	}

	private String fetchText(String url) {
		try {
			HttpClient client = HttpClient.newBuilder()
					.connectTimeout(Duration.ofMillis(timeoutMillis))
					.followRedirects(HttpClient.Redirect.NORMAL)
					.build();
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofMillis(timeoutMillis))
					.header("User-Agent", "Mozilla/5.0")
					.header("Referer", baseUrl)
					.header("Origin", baseUrl)
					.GET()
					.build();
			HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new CrawlerException(CrawlerFailureType.SITE_ACCESS_FAILED,
						"Search page returned HTTP " + response.statusCode());
			}
			return new String(response.body(), StandardCharsets.UTF_8);
		}
		catch (CrawlerException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new CrawlerException(CrawlerFailureType.SITE_ACCESS_FAILED,
					"Failed to fetch hsmoa search page: " + ex.getMessage(), ex);
		}
	}

	private CrawledSearchResult searchByPlaywrightRequest(String searchUrl, String keyword) {
		try (Playwright playwright = Playwright.create()) {
			APIRequestContext request = playwright.request().newContext();
			APIResponse response = request.get(searchUrl);
			if (!response.ok()) {
				throw new CrawlerException(CrawlerFailureType.SITE_ACCESS_FAILED,
						"Search page returned HTTP " + response.status());
			}
			return extractProducts(extractNextData(response.text()), keyword);
		}
		catch (CrawlerException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new CrawlerException(CrawlerFailureType.SITE_ACCESS_FAILED,
					"Failed to fetch hsmoa search page: " + ex.getMessage(), ex);
		}
	}

	private void openSearchPage(Page page, String keyword) {
		try {
			page.navigate(baseUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
			Optional<Locator> input = findFirstVisible(page, selectors.searchInputs());
			if (input.isPresent()) {
				input.get().fill(keyword);
				input.get().press("Enter");
				page.waitForURL("**/search**", new Page.WaitForURLOptions().setTimeout(3000));
				return;
			}
		}
		catch (TimeoutError ex) {
			throw ex;
		}
		catch (Exception ex) {
			log.warn("Home search interaction failed. keyword={}, reason={}", keyword, ex.getMessage());
		}

		if (!directSearchFallback) {
			throw new CrawlerException(CrawlerFailureType.SEARCH_INPUT_NOT_FOUND, "No visible search input on hsmoa home");
		}

		page.navigate(buildSearchUrl(keyword), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
	}

	private String buildSearchUrl(String keyword) {
		return UriComponentsBuilder.fromHttpUrl(baseUrl)
				.path("/search")
				.queryParam("query", keyword)
				.build()
				.encode()
				.toUriString();
	}

	private Optional<Locator> findFirstVisible(Page page, List<String> candidates) {
		for (String selector : candidates) {
			Locator locator = page.locator(selector).first();
			try {
				if (locator.count() > 0 && locator.isVisible()) {
					return Optional.of(locator);
				}
			}
			catch (PlaywrightException ex) {
				log.debug("Selector candidate failed. selector={}, reason={}", selector, ex.getMessage());
			}
		}
		return Optional.empty();
	}

	private void waitForResultArea(Page page) {
		for (String selector : selectors.resultAreas()) {
			try {
				page.locator(selector).first().waitFor();
				return;
			}
			catch (PlaywrightException ex) {
				log.debug("Result selector did not match. selector={}, reason={}", selector, ex.getMessage());
			}
		}
		throw new CrawlerException(CrawlerFailureType.RESULT_AREA_NOT_FOUND, "No configured result area selector matched");
	}

	private CrawledSearchResult extractProducts(String nextData, String keyword) {
		if (nextData == null || nextData.isBlank()) {
			throw new CrawlerException(CrawlerFailureType.RESULT_AREA_NOT_FOUND, "Empty __NEXT_DATA__ content");
		}
		try {
			JsonNode root = objectMapper.readTree(nextData);
			Map<String, CrawledProduct> unique = new LinkedHashMap<>();
			JsonNode aggregatedData = root.path("props").path("pageProps").path("aggregatedData");
			if (aggregatedData.isMissingNode()) {
				throw new CrawlerException(CrawlerFailureType.RESULT_AREA_NOT_FOUND, "aggregatedData was not found");
			}
			collectSearchPageSections(aggregatedData, unique, keyword);
			return new CrawledSearchResult(limitResults(unique), null);
		}
		catch (CrawlerException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new CrawlerException(CrawlerFailureType.RESULT_AREA_NOT_FOUND, "Failed to parse search result JSON", ex);
		}
	}

	private void collectSearchPageSections(JsonNode aggregatedData, Map<String, CrawledProduct> unique, String keyword) {
		collectProductNodes(aggregatedData.path("future"), unique, keyword);
		collectProductNodes(aggregatedData.path("best"), unique, keyword);
		collectProductNodes(aggregatedData.path("past"), unique, keyword);
		aggregatedData.fields().forEachRemaining(entry -> {
			if (entry.getKey().matches("ep\\d+")) {
				collectProductNodes(entry.getValue(), unique, keyword);
			}
		});
	}

	private String extractNextData(String html) {
		if (html == null || html.isBlank()) {
			throw new CrawlerException(CrawlerFailureType.RESULT_AREA_NOT_FOUND, "Empty search page HTML");
		}
		String marker = "<script id=\"__NEXT_DATA__\" type=\"application/json\">";
		int start = html.indexOf(marker);
		if (start < 0) {
			throw new CrawlerException(CrawlerFailureType.RESULT_AREA_NOT_FOUND, "__NEXT_DATA__ script was not found");
		}
		start += marker.length();
		int end = html.indexOf("</script>", start);
		if (end < 0) {
			throw new CrawlerException(CrawlerFailureType.RESULT_AREA_NOT_FOUND, "__NEXT_DATA__ script was not closed");
		}
		return html.substring(start, end);
	}

	private void collectProductNodes(JsonNode node, Map<String, CrawledProduct> unique, String keyword) {
		if (node == null || reachedMaxResults(unique)) {
			return;
		}
		if (node.isObject()) {
			CrawledProduct product = toProduct(node, keyword);
			if (product != null) {
				String key = product.productUrl() != null ? product.productUrl()
						: product.mallName() + "|" + product.productCode() + "|" + product.productName();
				unique.putIfAbsent(key, product);
			}
			node.fields().forEachRemaining(entry -> collectProductNodes(entry.getValue(), unique, keyword));
			return;
		}
		if (node.isArray()) {
			for (JsonNode child : node) {
				collectProductNodes(child, unique, keyword);
			}
		}
	}

	private boolean reachedMaxResults(Map<String, CrawledProduct> unique) {
		return maxResults > 0 && unique.size() >= maxResults;
	}

	private List<CrawledProduct> limitResults(Map<String, CrawledProduct> unique) {
		if (maxResults <= 0) {
			return new ArrayList<>(unique.values());
		}
		return new ArrayList<>(unique.values()).stream().limit(maxResults).toList();
	}

	private CrawledProduct toProduct(JsonNode node, String keyword) {
		String name = text(node, "name");
		String url = text(node, "url");
		if (name == null || url == null) {
			return null;
		}

		String site = text(node, "site");
		Long price = bestPrice(node);
		if (site == null && price == null) {
			return null;
		}
		if (!isRelevant(node, keyword)) {
			return null;
		}
		String priceText = priceParser.format(price);
		String productCode = extractProductCode(node, url);
		return new CrawledProduct(toMallName(site), productCode, name, price, priceText, url, LocalDateTime.now());
	}

	private String extractProductCode(JsonNode node, String url) {
		String pid = text(node, "pid");
		if (pid != null) {
			return pid;
		}
		String pdid = text(node, "pdid");
		if (pdid != null && pdid.contains("_")) {
			return pdid.substring(pdid.indexOf('_') + 1);
		}
		return extractProductCodeFromUrl(url);
	}

	private String extractProductCodeFromUrl(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		String[] queryKeys = {"slitmCd", "goods_id", "goodsId", "item", "itemId", "pid"};
		for (String key : queryKeys) {
			String value = queryParam(url, key);
			if (value != null) {
				return value;
			}
		}
		String[] pathMarkers = {"/item/", "/goods/", "/goods/", "/detail/", "/products/"};
		for (String marker : pathMarkers) {
			String value = pathSegmentAfter(url, marker);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private String queryParam(String url, String key) {
		int queryStart = url.indexOf('?');
		if (queryStart < 0 || queryStart == url.length() - 1) {
			return null;
		}
		String query = url.substring(queryStart + 1);
		for (String pair : query.split("&")) {
			int separator = pair.indexOf('=');
			if (separator <= 0) {
				continue;
			}
			String name = decode(pair.substring(0, separator));
			if (key.equals(name)) {
				String value = decode(pair.substring(separator + 1));
				return value.isBlank() ? null : value;
			}
		}
		return null;
	}

	private String pathSegmentAfter(String url, String marker) {
		int markerIndex = url.indexOf(marker);
		if (markerIndex < 0) {
			return null;
		}
		int start = markerIndex + marker.length();
		int end = url.indexOf('/', start);
		int query = url.indexOf('?', start);
		if (end < 0 || query >= 0 && query < end) {
			end = query;
		}
		if (end < 0) {
			end = url.length();
		}
		String value = decode(url.substring(start, end));
		return value.isBlank() ? null : value;
	}

	private String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private boolean isRelevant(JsonNode node, String keyword) {
		String normalizedKeyword = normalizeForMatch(keyword);
		if (normalizedKeyword.isBlank()) {
			return true;
		}
		String combined = String.join(" ",
				defaultText(text(node, "name")),
				defaultText(text(node, "name_query")),
				defaultText(text(node, "pid")),
				defaultText(text(node, "pdid")),
				defaultText(text(node, "brand")));
		String normalizedCombined = normalizeForMatch(combined);
		for (String token : normalizedKeyword.split("\\s+")) {
			if (!token.isBlank() && !normalizedCombined.contains(token)) {
				return false;
			}
		}
		return true;
	}

	private String normalizeForMatch(String text) {
		if (text == null) {
			return "";
		}
		return text.toLowerCase().replaceAll("[^0-9a-z가-힣]+", " ").trim();
	}

	private String defaultText(String text) {
		return text == null ? "" : text;
	}

	private Long bestPrice(JsonNode node) {
		JsonNode priceInfo = node.path("price_info");
		Long maxDiscountPrice = number(priceInfo, "max_discount_price");
		if (maxDiscountPrice != null) {
			return maxDiscountPrice;
		}
		Long salePrice = number(node, "sale_price");
		if (salePrice != null) {
			return salePrice;
		}
		return number(node, "price");
	}

	private String text(JsonNode node, String fieldName) {
		JsonNode value = node.get(fieldName);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asText(null);
		return text == null || text.isBlank() ? null : text;
	}

	private Long number(JsonNode node, String fieldName) {
		JsonNode value = node.get(fieldName);
		if (value == null || value.isNull() || !value.canConvertToLong()) {
			return null;
		}
		return value.asLong();
	}

	private String toMallName(String site) {
		if (site == null || site.isBlank()) {
			return "홈쇼핑모아";
		}
		if ("hnsmall".equals(site)) {
			return "홈앤쇼핑";
		}
		if ("kshop".equals(site)) {
			return "KT알파쇼핑";
		}
		if ("shopnt".equals(site)) {
			return "쇼핑엔티";
		}
		if ("wshop".equals(site)) {
			return "W쇼핑";
		}
		return switch (site) {
			case "gsshop" -> "GS샵";
			case "cjmall" -> "CJ온스타일";
			case "hmall" -> "현대홈쇼핑";
			case "lotteimall" -> "롯데홈쇼핑";
			case "ssgshop" -> "신세계라이브쇼핑";
			case "skstoa" -> "SK스토아";
			case "bshop" -> "SK스토아";
			case "nsmall" -> "NS홈쇼핑";
			case "hnsmall" -> "홈앤쇼핑";
			default -> site;
		};
	}
}
