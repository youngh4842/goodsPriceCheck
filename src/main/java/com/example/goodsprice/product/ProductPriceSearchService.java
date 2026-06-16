package com.example.goodsprice.product;

import com.example.goodsprice.crawler.CrawledProduct;
import com.example.goodsprice.crawler.CrawledSearchResult;
import com.example.goodsprice.crawler.CrawlerException;
import com.example.goodsprice.crawler.HsmoaPriceCrawler;
import com.example.goodsprice.domain.ProductPriceResult;
import com.example.goodsprice.domain.SearchLog;
import com.example.goodsprice.repository.DailyIdGenerator;
import com.example.goodsprice.repository.SearchLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ProductPriceSearchService {

	private final HsmoaPriceCrawler crawler;
	private final SearchLogRepository searchLogRepository;
	private final DailyIdGenerator dailyIdGenerator;
	private final ProductPeriodParser productPeriodParser;
	private final ProductNameNormalizer productNameNormalizer;

	public ProductPriceSearchService(HsmoaPriceCrawler crawler, SearchLogRepository searchLogRepository,
			DailyIdGenerator dailyIdGenerator, ProductPeriodParser productPeriodParser,
			ProductNameNormalizer productNameNormalizer) {
		this.crawler = crawler;
		this.searchLogRepository = searchLogRepository;
		this.dailyIdGenerator = dailyIdGenerator;
		this.productPeriodParser = productPeriodParser;
		this.productNameNormalizer = productNameNormalizer;
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
						product.productUrl(), product.crawledAt());
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
					productName = productNameNormalizer.removeKeywordAndPeriod(productName, keyword, productPeriod);
					String rentalYn = isRentalProduct(productName, productPeriod) ? "O" : "X";
					productName = productNameNormalizer.removeRentalWord(productName);
					return new PreparedProduct(product.mallName(), product.productCode(), productName, rentalYn, productPeriod,
							product.price(), product.priceText(), product.productUrl(), product.crawledAt());
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
		List<ProductPriceItemResponse> results = products.stream()
				.map(product -> new ProductPriceItemResponse(product.mallName(), product.productCode(),
						product.productName(), product.rentalYn(), product.productPeriod(), product.price(), product.priceText(),
						product.productUrl()))
				.toList();
		String responseMessage = message;
		if (results.isEmpty()) {
			responseMessage = "검색 결과가 없습니다.";
		}
		return new ProductPriceSearchResponse(keyword, searchedAt, !results.isEmpty(), results.size(), sourceTotalCount,
				results, responseMessage);
	}

	private record PreparedProduct(
			String mallName,
			String productCode,
			String productName,
			String rentalYn,
			String productPeriod,
			Long price,
			String priceText,
			String productUrl,
			LocalDateTime crawledAt
	) {
	}
}
