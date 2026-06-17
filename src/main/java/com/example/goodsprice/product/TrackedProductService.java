package com.example.goodsprice.product;

import com.example.goodsprice.domain.ProductMallItem;
import com.example.goodsprice.domain.ProductMaster;
import com.example.goodsprice.repository.DailyIdGenerator;
import com.example.goodsprice.repository.ProductMallItemRepository;
import com.example.goodsprice.repository.ProductMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrackedProductService {

	private final ProductMasterRepository productMasterRepository;
	private final ProductMallItemRepository productMallItemRepository;
	private final DailyIdGenerator dailyIdGenerator;
	private final ProductMatchEvaluator productMatchEvaluator;

	public TrackedProductService(ProductMasterRepository productMasterRepository,
			ProductMallItemRepository productMallItemRepository, DailyIdGenerator dailyIdGenerator,
			ProductMatchEvaluator productMatchEvaluator) {
		this.productMasterRepository = productMasterRepository;
		this.productMallItemRepository = productMallItemRepository;
		this.dailyIdGenerator = dailyIdGenerator;
		this.productMatchEvaluator = productMatchEvaluator;
	}

	@Transactional
	public TrackedProductResponse register(TrackedProductRequest request) {
		String normalizedProductCode = normalizeProductCode(request.productCode(), request.keyword());
		ProductMaster productMaster = findOrCreateProductMaster(request, normalizedProductCode);
		List<Long> mallItemIds = new ArrayList<>();

		for (TrackedMallItemRequest itemRequest : request.selectedItems()) {
			ProductMallItem mallItem = productMallItemRepository
					.findByMallNameAndProductUrl(itemRequest.mallName(), itemRequest.productUrl())
					.map(existing -> refresh(existing, itemRequest))
					.orElseGet(() -> createMallItem(productMaster, itemRequest));
			productMallItemRepository.save(mallItem);
			mallItemIds.add(mallItem.getMallItemId());
		}

		return new TrackedProductResponse(productMaster.getProductId(), mallItemIds, mallItemIds.size(),
				"추적 상품으로 등록했습니다.");
	}

	@Transactional(readOnly = true)
	public List<TrackedProductGroupResponse> findTrackedProducts() {
		Map<String, List<TrackedProductItemResponse>> groupedItems = new LinkedHashMap<>();
		productMallItemRepository.findAll().stream()
				.sorted(Comparator
						.comparing((ProductMallItem item) -> blankToDash(item.getProductMaster().getSearchKeyword()))
						.thenComparing(item -> blankToDash(item.getProductMaster().getProductCode()))
						.thenComparing(ProductMallItem::getMallItemId))
				.forEach(item -> {
					ProductMaster productMaster = item.getProductMaster();
					String keyword = blankToDash(productMaster.getSearchKeyword());
					groupedItems.computeIfAbsent(keyword, key -> new ArrayList<>())
							.add(new TrackedProductItemResponse(productMaster.getProductId(), item.getMallItemId(),
									item.getMallProductCode(), item.getNormalizedProductName(), item.getPrice(),
									item.getPriceText(), item.getProductUrl()));
				});
		return groupedItems.entrySet().stream()
				.map(entry -> new TrackedProductGroupResponse(entry.getKey(), entry.getValue()))
				.toList();
	}

	@Transactional
	public MallItemStatusResponse changeMatchStatus(Long mallItemId, MatchStatusUpdateRequest request) {
		ProductMallItem mallItem = productMallItemRepository.findById(mallItemId)
				.orElseThrow(() -> new ProductPriceSearchException("등록된 쇼핑몰 상품을 찾을 수 없습니다."));
		mallItem.changeMatchStatus(request.matchStatus(), request.manualConfirmed());
		return new MallItemStatusResponse(mallItem.getMallItemId(), mallItem.getMatchStatus(),
				mallItem.getManuallyConfirmedYn());
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
				request.mallProductName(), request.productCode(), request.normalizedProductName(), request.productUrl(), request.price(),
				request.priceText(), saleType(request), matchStatus(request), matchScore(request), matchReason(request));
	}

	private ProductMallItem refresh(ProductMallItem mallItem, TrackedMallItemRequest request) {
		mallItem.refresh(request.mallProductName(), request.productCode(), request.normalizedProductName(), request.price(), request.priceText(),
				saleType(request), matchStatus(request), matchScore(request), matchReason(request));
		return mallItem;
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
