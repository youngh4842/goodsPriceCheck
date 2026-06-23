package com.example.goodsprice.product;

import com.example.goodsprice.crawler.CrawledProduct;
import com.example.goodsprice.crawler.CrawledSearchResult;
import com.example.goodsprice.crawler.ProductCrawler;
import com.example.goodsprice.domain.CrawlItemLog;
import com.example.goodsprice.domain.CrawlRunLog;
import com.example.goodsprice.domain.ProductMallItem;
import com.example.goodsprice.domain.ProductMaster;
import com.example.goodsprice.domain.ProductPriceHistory;
import com.example.goodsprice.repository.CrawlItemLogRepository;
import com.example.goodsprice.repository.CrawlRunLogRepository;
import com.example.goodsprice.repository.DailyIdGenerator;
import com.example.goodsprice.repository.ProductMallItemRepository;
import com.example.goodsprice.repository.ProductMasterRepository;
import com.example.goodsprice.repository.ProductPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TrackedProductService {

	private final ProductMasterRepository productMasterRepository;
	private final ProductMallItemRepository productMallItemRepository;
	private final ProductPriceHistoryRepository productPriceHistoryRepository;
	private final CrawlRunLogRepository crawlRunLogRepository;
	private final CrawlItemLogRepository crawlItemLogRepository;
	private final DailyIdGenerator dailyIdGenerator;
	private final ProductMatchEvaluator productMatchEvaluator;
	private final PriceHistoryService priceHistoryService;
	private final ProductCrawler crawler;

	public TrackedProductService(ProductMasterRepository productMasterRepository,
			ProductMallItemRepository productMallItemRepository,
			ProductPriceHistoryRepository productPriceHistoryRepository, CrawlRunLogRepository crawlRunLogRepository,
			CrawlItemLogRepository crawlItemLogRepository, DailyIdGenerator dailyIdGenerator,
			ProductMatchEvaluator productMatchEvaluator, PriceHistoryService priceHistoryService,
			ProductCrawler crawler) {
		this.productMasterRepository = productMasterRepository;
		this.productMallItemRepository = productMallItemRepository;
		this.productPriceHistoryRepository = productPriceHistoryRepository;
		this.crawlRunLogRepository = crawlRunLogRepository;
		this.crawlItemLogRepository = crawlItemLogRepository;
		this.dailyIdGenerator = dailyIdGenerator;
		this.productMatchEvaluator = productMatchEvaluator;
		this.priceHistoryService = priceHistoryService;
		this.crawler = crawler;
	}

	@Transactional
	public TrackedProductResponse register(TrackedProductRequest request) {
		String normalizedProductCode = normalizeProductCode(request.productCode(), request.keyword());
		ProductMaster productMaster = findOrCreateProductMaster(request, normalizedProductCode);
		CrawlRunLog runLog = new CrawlRunLog(dailyIdGenerator.nextCrawlRunId(LocalDate.now()),
				PriceHistorySourceType.USER_SEARCH, request.selectedItems().size());
		crawlRunLogRepository.save(runLog);

		List<Long> mallItemIds = new ArrayList<>();
		int successCount = 0;
		int failureCount = 0;
		int changedCount = 0;

		for (TrackedMallItemRequest itemRequest : request.selectedItems()) {
			ProductMallItem mallItem = productMallItemRepository
					.findByMallNameAndProductUrl(itemRequest.mallName(), itemRequest.productUrl())
					.map(existing -> refresh(existing, itemRequest))
					.orElseGet(() -> createMallItem(productMaster, itemRequest));
			productMallItemRepository.save(mallItem);
			mallItemIds.add(mallItem.getMallItemId());
			try {
				PriceHistoryResult result = priceHistoryService.recordPrice(mallItem, itemRequest.price(),
						itemRequest.priceText(), PriceHistorySourceType.USER_SEARCH, LocalDateTime.now());
				successCount += 1;
				if (result.historyCreated() && result.changed()) {
					changedCount += 1;
				}
				saveItemLog(runLog, mallItem, CrawlItemStatus.SUCCESS, result.previousPrice(), result.currentPrice(),
						result.historyCreated() && result.changed(), null, LocalDateTime.now());
			}
			catch (RuntimeException ex) {
				failureCount += 1;
				saveItemLog(runLog, mallItem, CrawlItemStatus.PRICE_PARSE_FAILED, null, itemRequest.price(), false,
						ex.getMessage(), LocalDateTime.now());
			}
		}
		runLog.finish(successCount, failureCount, changedCount, null);
		return new TrackedProductResponse(productMaster.getProductId(), mallItemIds, mallItemIds.size(),
				"추적 상품으로 등록했습니다.");
	}

	@Transactional(readOnly = true)
	public List<TrackedProductGroupResponse> findTrackedProducts() {
		Map<String, List<TrackedProductItemResponse>> groupedItems = new LinkedHashMap<>();
		productMallItemRepository.findAll().stream()
				.filter(item -> "Y".equals(item.getActiveYn()))
				.sorted(Comparator
						.comparing((ProductMallItem item) -> blankToDash(item.getProductMaster().getSearchKeyword()))
						.thenComparing(item -> blankToDash(item.getProductMaster().getProductCode()))
						.thenComparing(ProductMallItem::getMallItemId))
				.forEach(item -> {
					ProductMaster productMaster = item.getProductMaster();
					String keyword = blankToDash(productMaster.getSearchKeyword());
					groupedItems.computeIfAbsent(keyword, key -> new ArrayList<>()).add(toTrackedItemResponse(item));
				});
		return groupedItems.entrySet().stream()
				.map(entry -> new TrackedProductGroupResponse(entry.getKey(), entry.getValue()))
				.toList();
	}

	@Transactional(readOnly = true)
	public ProductPriceHistoryResponse findPriceHistory(Long productId, Long mallItemId, LocalDateTime from,
			LocalDateTime to) {
		ProductMaster productMaster = productMasterRepository.findById(productId)
				.orElseThrow(() -> new ProductPriceSearchException("추적 상품을 찾을 수 없습니다."));
		List<ProductMallItem> mallItems = productMallItemRepository.findByProductMasterProductId(productId).stream()
				.filter(item -> mallItemId == null || Objects.equals(item.getMallItemId(), mallItemId))
				.sorted(Comparator.comparing(ProductMallItem::getMallItemId))
				.toList();
		List<PriceHistoryItemResponse> items = mallItems.stream()
				.map(item -> toPriceHistoryItemResponse(item, histories(item.getMallItemId(), from, to)))
				.toList();
		return new ProductPriceHistoryResponse(productMaster.getProductId(), productMaster.getProductCode(),
				productMaster.getProductName(), items);
	}

	@Transactional
	public RefreshTrackedProductResponse refreshTrackedProduct(Long productId) {
		ProductMaster productMaster = productMasterRepository.findById(productId)
				.orElseThrow(() -> new ProductPriceSearchException("추적 상품을 찾을 수 없습니다."));
		List<ProductMallItem> mallItems = productMallItemRepository.findByProductMasterProductIdAndActiveYn(productId, "Y");
		CrawlRunLog runLog = new CrawlRunLog(dailyIdGenerator.nextCrawlRunId(LocalDate.now()),
				PriceHistorySourceType.MANUAL, mallItems.size());
		crawlRunLogRepository.save(runLog);

		int successCount = 0;
		int failureCount = 0;
		int changedCount = 0;
		String runError = null;
		List<CrawledProduct> crawledProducts;
		try {
			String keyword = refreshKeyword(productMaster);
			CrawledSearchResult searchResult = crawler.search(keyword);
			crawledProducts = searchResult.products();
		}
		catch (RuntimeException ex) {
			runError = ex.getMessage();
			crawledProducts = List.of();
		}

		for (ProductMallItem mallItem : mallItems) {
			try {
				CrawledProduct crawledProduct = crawler.fetchProduct(mallItem.getProductUrl()).orElse(null);
				if (crawledProduct == null) {
					crawledProduct = findCrawledProduct(mallItem, crawledProducts);
				}
				if (crawledProduct == null) {
					failureCount += 1;
					saveItemLog(runLog, mallItem, CrawlItemStatus.NOT_FOUND, mallItem.getPrice(), null, false,
							"검색 결과에서 쇼핑몰 상품을 찾을 수 없습니다.", LocalDateTime.now());
					continue;
				}
				if (crawledProduct.price() == null || crawledProduct.price() <= 0) {
					failureCount += 1;
					saveItemLog(runLog, mallItem, CrawlItemStatus.PRICE_PARSE_FAILED, mallItem.getPrice(),
							crawledProduct.price(), false, "정상 가격을 파싱하지 못했습니다.", crawledProduct.crawledAt());
					continue;
				}
				PriceHistoryResult result = priceHistoryService.recordPrice(mallItem, crawledProduct.price(),
						crawledProduct.priceText(), PriceHistorySourceType.MANUAL, crawledProduct.crawledAt());
				successCount += 1;
				if (result.historyCreated() && result.changed()) {
					changedCount += 1;
				}
				saveItemLog(runLog, mallItem, CrawlItemStatus.SUCCESS, result.previousPrice(), result.currentPrice(),
						result.historyCreated() && result.changed(), null, crawledProduct.crawledAt());
			}
			catch (RuntimeException ex) {
				failureCount += 1;
				saveItemLog(runLog, mallItem, CrawlItemStatus.FAILED, mallItem.getPrice(), null, false, ex.getMessage(),
						LocalDateTime.now());
			}
		}
		runLog.finish(successCount, failureCount, changedCount, runError);
		return new RefreshTrackedProductResponse(productId, runLog.getCrawlRunId(), mallItems.size(), successCount,
				failureCount, changedCount, "수동 재조회를 완료했습니다.");
	}

	@Transactional
	public void unregisterMallItem(Long mallItemId) {
		ProductMallItem mallItem = productMallItemRepository.findById(mallItemId)
				.orElseThrow(() -> new ProductPriceSearchException("추적 등록된 쇼핑몰 상품을 찾을 수 없습니다."));
		mallItem.deactivateTracking();
	}

	@Transactional
	public MallItemStatusResponse changeMatchStatus(Long mallItemId, MatchStatusUpdateRequest request) {
		ProductMallItem mallItem = productMallItemRepository.findById(mallItemId)
				.orElseThrow(() -> new ProductPriceSearchException("등록된 쇼핑몰 상품을 찾을 수 없습니다."));
		mallItem.changeMatchStatus(request.matchStatus(), request.manualConfirmed());
		return new MallItemStatusResponse(mallItem.getMallItemId(), mallItem.getMatchStatus(),
				mallItem.getManuallyConfirmedYn());
	}

	private TrackedProductItemResponse toTrackedItemResponse(ProductMallItem item) {
		ProductPriceHistory firstHistory = productPriceHistoryRepository
				.findTopByMallItemMallItemIdOrderByCrawledAtAscPriceHistoryIdAsc(item.getMallItemId())
				.orElse(null);
		ProductPriceHistory lowestHistory = productPriceHistoryRepository
				.findTopByMallItemMallItemIdOrderByPriceAscPriceHistoryIdAsc(item.getMallItemId())
				.orElse(null);
		ProductPriceHistory latestChangedHistory = productPriceHistoryRepository
				.findTopByMallItemMallItemIdAndPreviousPriceNotNullOrderByCrawledAtDescPriceHistoryIdDesc(
						item.getMallItemId())
				.orElse(null);
		return new TrackedProductItemResponse(item.getProductMaster().getProductId(), item.getMallItemId(),
				item.getMallName(), item.getMallProductCode(), item.getNormalizedProductName(), item.getPrice(),
				item.getPriceText(), firstHistory == null ? null : firstHistory.getPrice(),
				latestChangedHistory == null ? null : latestChangedHistory.getPreviousPrice(),
				latestChangedHistory == null ? null : latestChangedHistory.getChangeAmount(),
				latestChangedHistory == null ? null : latestChangedHistory.getChangeRate(),
				lowestHistory == null ? null : lowestHistory.getPrice(),
				latestChangedHistory == null ? null : latestChangedHistory.getCrawledAt(), item.getLastCrawledAt(),
				item.getActiveYn(), item.getProductUrl());
	}

	private PriceHistoryItemResponse toPriceHistoryItemResponse(ProductMallItem item, List<ProductPriceHistory> histories) {
		Long lowestPrice = histories.stream()
				.map(ProductPriceHistory::getPrice)
				.filter(Objects::nonNull)
				.min(Long::compareTo)
				.orElse(null);
		List<PriceHistoryEntryResponse> historyEntries = histories.stream()
				.map(history -> new PriceHistoryEntryResponse(history.getPrice(), history.getPriceText(),
						history.getPreviousPrice(), history.getChangeAmount(), history.getChangeRate(),
						history.getLowestPriceYn(), history.getSourceType(), history.getCrawledAt()))
				.toList();
		return new PriceHistoryItemResponse(item.getMallName(), item.getMallItemId(), item.getMallProductCode(),
				item.getNormalizedProductName(), item.getPrice(), lowestPrice, item.getLastCrawledAt(), historyEntries);
	}

	private List<ProductPriceHistory> histories(Long mallItemId, LocalDateTime from, LocalDateTime to) {
		if (from != null || to != null) {
			LocalDateTime start = from == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : from;
			LocalDateTime end = to == null ? LocalDateTime.now().plusYears(100) : to;
			return productPriceHistoryRepository
					.findByMallItemMallItemIdAndCrawledAtBetweenOrderByCrawledAtAscPriceHistoryIdAsc(mallItemId, start, end);
		}
		return productPriceHistoryRepository.findByMallItemMallItemIdOrderByCrawledAtAscPriceHistoryIdAsc(mallItemId);
	}

	private ProductMaster findOrCreateProductMaster(TrackedProductRequest request, String normalizedProductCode) {
		if (normalizedProductCode != null && !normalizedProductCode.isBlank()) {
			return productMasterRepository.findByProductCode(normalizedProductCode)
					.map(product -> {
						product.updateBaseInfo(request.keyword(), request.productName(), request.brandName());
						return product;
					})
					.orElseGet(() -> productMasterRepository.save(new ProductMaster(
							dailyIdGenerator.nextProductId(LocalDate.now()), normalizedProductCode, request.keyword(),
							request.productName(), request.brandName())));
		}
		return productMasterRepository.save(new ProductMaster(dailyIdGenerator.nextProductId(LocalDate.now()), null,
				request.keyword(), request.productName(), request.brandName()));
	}

	private String normalizeProductCode(String productCode, String keyword) {
		String candidate = productCode != null && !productCode.isBlank() ? productCode : keyword;
		String normalized = productMatchEvaluator.normalizeModelCode(candidate);
		return normalized.isBlank() ? null : normalized;
	}

	private ProductMallItem createMallItem(ProductMaster productMaster, TrackedMallItemRequest request) {
		return new ProductMallItem(dailyIdGenerator.nextMallItemId(LocalDate.now()), productMaster, request.mallName(),
				request.mallProductName(), request.productCode(), request.normalizedProductName(), request.productUrl(),
				request.price(), request.priceText(), saleType(request), matchStatus(request), matchScore(request),
				matchReason(request));
	}

	private ProductMallItem refresh(ProductMallItem mallItem, TrackedMallItemRequest request) {
		mallItem.refresh(request.mallProductName(), request.productCode(), request.normalizedProductName(), request.price(),
				request.priceText(), saleType(request), matchStatus(request), matchScore(request), matchReason(request));
		return mallItem;
	}

	private void saveItemLog(CrawlRunLog runLog, ProductMallItem mallItem, CrawlItemStatus status, Long previousPrice,
			Long currentPrice, boolean priceChanged, String errorMessage, LocalDateTime crawledAt) {
		crawlItemLogRepository.save(new CrawlItemLog(dailyIdGenerator.nextCrawlItemLogId(LocalDate.now()), runLog,
				mallItem, status, previousPrice, currentPrice, priceChanged, errorMessage, crawledAt));
	}

	private String refreshKeyword(ProductMaster productMaster) {
		if (productMaster.getProductCode() != null && !productMaster.getProductCode().isBlank()) {
			return productMaster.getProductCode();
		}
		if (productMaster.getSearchKeyword() != null && !productMaster.getSearchKeyword().isBlank()) {
			return productMaster.getSearchKeyword();
		}
		return productMaster.getProductName();
	}

	private CrawledProduct findCrawledProduct(ProductMallItem mallItem, List<CrawledProduct> products) {
		return products.stream()
				.filter(product -> same(product.productUrl(), mallItem.getProductUrl())
						|| same(product.productCode(), mallItem.getMallProductCode()))
				.findFirst()
				.orElse(null);
	}

	private boolean same(String left, String right) {
		return left != null && right != null && left.equals(right);
	}

	private SaleType saleType(TrackedMallItemRequest request) {
		return request.saleType() == null ? SaleType.UNKNOWN : request.saleType();
	}

	private MatchStatus matchStatus(TrackedMallItemRequest request) {
		return request.matchStatus() == null ? MatchStatus.UNKNOWN : request.matchStatus();
	}

	private int matchScore(TrackedMallItemRequest request) {
		return request.matchScore() == null ? 0 : request.matchScore();
	}

	private String matchReason(TrackedMallItemRequest request) {
		if (request.matchReasons() == null || request.matchReasons().isEmpty()) {
			return null;
		}
		return String.join(" / ", request.matchReasons());
	}

	private String blankToDash(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}
}
