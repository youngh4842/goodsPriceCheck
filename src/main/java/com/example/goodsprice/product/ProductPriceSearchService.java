package com.example.goodsprice.product;

import com.example.goodsprice.crawler.CrawledProduct;
import com.example.goodsprice.crawler.CrawledSearchResult;
import com.example.goodsprice.crawler.CrawlerException;
import com.example.goodsprice.crawler.HsmoaPriceCrawler;
import com.example.goodsprice.domain.ProductMallItem;
import com.example.goodsprice.domain.ProductNotMatchedItem;
import com.example.goodsprice.domain.ProductPriceResult;
import com.example.goodsprice.domain.SearchLog;
import com.example.goodsprice.repository.DailyIdGenerator;
import com.example.goodsprice.repository.ProductMallItemRepository;
import com.example.goodsprice.repository.ProductNotMatchedItemRepository;
import com.example.goodsprice.repository.SearchLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductPriceSearchService {

	private final HsmoaPriceCrawler crawler;
	private final SearchLogRepository searchLogRepository;
	private final DailyIdGenerator dailyIdGenerator;
	private final ProductPeriodParser productPeriodParser;
	private final ProductNameNormalizer productNameNormalizer;
	private final ProductMatchEvaluator productMatchEvaluator;
	private final ProductMallItemRepository productMallItemRepository;
	private final ProductNotMatchedItemRepository productNotMatchedItemRepository;

	public ProductPriceSearchService(HsmoaPriceCrawler crawler, SearchLogRepository searchLogRepository,
			DailyIdGenerator dailyIdGenerator, ProductPeriodParser productPeriodParser,
			ProductNameNormalizer productNameNormalizer, ProductMatchEvaluator productMatchEvaluator,
			ProductMallItemRepository productMallItemRepository,
			ProductNotMatchedItemRepository productNotMatchedItemRepository) {
		this.crawler = crawler;
		this.searchLogRepository = searchLogRepository;
		this.dailyIdGenerator = dailyIdGenerator;
		this.productPeriodParser = productPeriodParser;
		this.productNameNormalizer = productNameNormalizer;
		this.productMatchEvaluator = productMatchEvaluator;
		this.productMallItemRepository = productMallItemRepository;
		this.productNotMatchedItemRepository = productNotMatchedItemRepository;
	}

	@Transactional
	public ProductPriceSearchResponse search(String rawKeyword) {
		String keyword = normalizeKeyword(rawKeyword);
		LocalDateTime searchedAt = LocalDateTime.now();
		SearchLog searchLog = new SearchLog(keyword, searchedAt);
		searchLog.assignSearchId(dailyIdGenerator.nextSearchId(searchedAt.toLocalDate()));

		try {
			CrawledSearchResult crawledSearchResult = crawler.search(keyword);
			List<PreparedProduct> preparedProducts = prepareProducts(crawledSearchResult.products(), keyword);
			for (PreparedProduct product : preparedProducts) {
				ProductPriceResult priceResult = new ProductPriceResult(product.mallName(), product.productCode(),
						product.productName(), product.productPeriod(), product.price(), product.priceText(),
						product.productUrl(), product.saleType(), product.matchStatus(), product.matchScore(),
						String.join(" / ", product.matchReasons()), product.crawledAt());
				priceResult.assignResultId(dailyIdGenerator.nextResultId(searchedAt.toLocalDate()));
				searchLog.addResult(priceResult);
			}
			searchLog.markSuccess(preparedProducts.size());
			searchLogRepository.save(searchLog);
			return toResponse(keyword, searchedAt, preparedProducts, crawledSearchResult.sourceTotalCount(), null);
		}
		catch (CrawlerException ex) {
			searchLog.markFailure(ex.getUserMessage() + " [" + ex.getFailureType() + "]");
			searchLogRepository.save(searchLog);
			throw new ProductPriceSearchException(ex.getUserMessage(), ex);
		}
		catch (RuntimeException ex) {
			searchLog.markFailure("가격 조회 중 오류가 발생했습니다. " + ex.getMessage());
			searchLogRepository.save(searchLog);
			throw ex;
		}
	}

	private String normalizeKeyword(String rawKeyword) {
		if (rawKeyword == null || rawKeyword.trim().isEmpty()) {
			throw new InvalidKeywordException("검색어를 입력해 주세요.");
		}
		return rawKeyword.trim();
	}

	private List<PreparedProduct> prepareProducts(List<CrawledProduct> products, String keyword) {
		return products.stream()
				.map(product -> {
					String productName = productNameNormalizer.removePrice(product.productName(), product.price());
					String productPeriod = productPeriodParser.parse(productName);
					productName = productNameNormalizer.removeKeywordAndPeriod(productName, null, productPeriod);
					String rentalYn = isRentalProduct(productName, productPeriod) ? "O" : "X";
					productName = productNameNormalizer.removeRentalWord(productName);
					ProductMatchResult matchResult = productMatchEvaluator.evaluate(keyword, product.mallName(),
							product.productName(), productName, product.productCode(), product.price(), productPeriod);
					return new PreparedProduct(product.mallName(), product.productCode(), product.productName(), productName,
							rentalYn, productPeriod, product.price(), product.priceText(), product.productUrl(),
							matchResult.saleType(), matchResult.matchStatus(), matchResult.matchScore(),
							matchResult.matchReasons(), product.crawledAt());
				})
				.sorted(Comparator.comparing(PreparedProduct::price, Comparator.nullsLast(Long::compareTo)))
				.toList();
	}

	private boolean isRentalProduct(String productName, String productPeriod) {
		if (productPeriod != null && !productPeriod.isBlank()) {
			return true;
		}
		return productName != null && productName.contains("렌탈");
	}

	private ProductPriceSearchResponse toResponse(String keyword, LocalDateTime searchedAt, List<PreparedProduct> products,
			Integer sourceTotalCount, String message) {
		Map<String, Long> trackedMallItemIds = findTrackedMallItemIds(products);
		ManualProductMatches manualMatches = findManualMatches(keyword, products);
		List<ProductPriceItemResponse> results = products.stream()
				.map(product -> {
					MatchStatus manualStatus = manualMatches.matchStatus(product.productCode(), product.productUrl());
					boolean rejected = manualStatus == MatchStatus.NOT_MATCHED;
					MatchStatus matchStatus = manualStatus == null ? product.matchStatus() : manualStatus;
					List<String> matchReasons = appendManualReason(product.matchReasons(), manualStatus);
					boolean tracked = !rejected && trackedMallItemIds.containsKey(product.productCode());
					return new ProductPriceItemResponse(product.mallName(), product.productCode(),
							product.mallProductName(), product.productName(), product.rentalYn(), product.productPeriod(),
							product.price(), product.priceText(), product.productUrl(), product.saleType(), matchStatus,
							product.matchScore(), matchReasons, tracked, tracked ? trackedMallItemIds.get(product.productCode()) : null,
							rejected);
				})
				.toList();
		String responseMessage = message;
		if (results.isEmpty()) {
			responseMessage = "검색 결과가 없습니다.";
		}
		return new ProductPriceSearchResponse(keyword, searchedAt, !results.isEmpty(), results.size(), sourceTotalCount,
				results, responseMessage);
	}

	private Map<String, Long> findTrackedMallItemIds(List<PreparedProduct> products) {
		List<String> productCodes = products.stream()
				.map(PreparedProduct::productCode)
				.filter(code -> code != null && !code.isBlank())
				.distinct()
				.toList();
		if (productCodes.isEmpty()) {
			return Map.of();
		}
		Map<String, Long> trackedMallItemIds = new LinkedHashMap<>();
		for (ProductMallItem item : productMallItemRepository.findByMallProductCodeIn(productCodes)) {
			if (item.getMallProductCode() != null && !item.getMallProductCode().isBlank()) {
				trackedMallItemIds.putIfAbsent(item.getMallProductCode(), item.getMallItemId());
			}
		}
		return trackedMallItemIds;
	}

	private ManualProductMatches findManualMatches(String keyword, List<PreparedProduct> products) {
		List<String> productUrls = products.stream()
				.map(PreparedProduct::productUrl)
				.filter(url -> url != null && !url.isBlank())
				.distinct()
				.toList();
		List<String> productCodes = products.stream()
				.map(PreparedProduct::productCode)
				.filter(code -> code != null && !code.isBlank())
				.distinct()
				.toList();
		if (productUrls.isEmpty() && productCodes.isEmpty()) {
			return new ManualProductMatches(Map.of(), Map.of());
		}
		Map<String, ProductNotMatchedItem> manualByUrl = new LinkedHashMap<>();
		Map<String, ProductNotMatchedItem> manualByCode = new LinkedHashMap<>();
		if (!productUrls.isEmpty()) {
			for (ProductNotMatchedItem item : productNotMatchedItemRepository.findByKeywordAndProductUrlIn(keyword, productUrls)) {
				if (item.getProductUrl() != null && !item.getProductUrl().isBlank()) {
					manualByUrl.putIfAbsent(item.getProductUrl(), item);
				}
			}
		}
		if (!productCodes.isEmpty()) {
			for (ProductNotMatchedItem item : productNotMatchedItemRepository
					.findByKeywordAndMallProductCodeIn(keyword, productCodes)) {
				if (item.getMallProductCode() != null && !item.getMallProductCode().isBlank()) {
					manualByCode.putIfAbsent(item.getMallProductCode(), item);
				}
			}
		}
		return new ManualProductMatches(manualByCode, manualByUrl);
	}

	private record ManualProductMatches(
			Map<String, ProductNotMatchedItem> byProductCode,
			Map<String, ProductNotMatchedItem> byProductUrl
	) {
		private MatchStatus matchStatus(String productCode, String productUrl) {
			if (productCode != null && byProductCode.containsKey(productCode)) {
				return byProductCode.get(productCode).getMatchStatus();
			}
			if (productUrl != null && byProductUrl.containsKey(productUrl)) {
				return byProductUrl.get(productUrl).getMatchStatus();
			}
			return null;
		}
	}

	private List<String> appendManualReason(List<String> reasons, MatchStatus manualStatus) {
		if (manualStatus == null) {
			return reasons;
		}
		List<String> mergedReasons = new ArrayList<>(reasons == null ? List.of() : reasons);
		if (manualStatus == MatchStatus.NOT_MATCHED) {
			mergedReasons.add("사용자가 동일하지 않은 상품으로 판정했습니다.");
		}
		else if (manualStatus == MatchStatus.POSSIBLE_MATCH) {
			mergedReasons.add("사용자가 재확인 대상으로 변경했습니다.");
		}
		return mergedReasons;
	}

	private record PreparedProduct(
			String mallName,
			String productCode,
			String mallProductName,
			String productName,
			String rentalYn,
			String productPeriod,
			Long price,
			String priceText,
			String productUrl,
			SaleType saleType,
			MatchStatus matchStatus,
			int matchScore,
			List<String> matchReasons,
			LocalDateTime crawledAt
	) {
	}
}
