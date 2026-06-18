package com.example.goodsprice.product;

import com.example.goodsprice.domain.ProductNotMatchedItem;
import com.example.goodsprice.repository.DailyIdGenerator;
import com.example.goodsprice.repository.ProductNotMatchedItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class NotMatchedProductService {

	private final ProductNotMatchedItemRepository productNotMatchedItemRepository;
	private final DailyIdGenerator dailyIdGenerator;

	public NotMatchedProductService(ProductNotMatchedItemRepository productNotMatchedItemRepository,
			DailyIdGenerator dailyIdGenerator) {
		this.productNotMatchedItemRepository = productNotMatchedItemRepository;
		this.dailyIdGenerator = dailyIdGenerator;
	}

	@Transactional
	public NotMatchedProductResponse register(NotMatchedProductRequest request) {
		String reason = normalizeReason(request.reason());
		ProductNotMatchedItem item = productNotMatchedItemRepository
				.findByKeywordAndMallNameAndProductUrl(request.keyword(), request.mallName(), request.productUrl())
				.map(existing -> {
					existing.markNotMatched(reason);
					return existing;
				})
				.orElseGet(() -> productNotMatchedItemRepository.save(new ProductNotMatchedItem(
						dailyIdGenerator.nextNotMatchedId(LocalDate.now()), request.keyword(), request.mallName(),
						request.productCode(), request.productName(), request.productUrl(), request.price(),
						request.priceText(), reason)));
		return new NotMatchedProductResponse(item.getNotMatchedId(), MatchStatus.NOT_MATCHED,
				"동일하지 않은 상품으로 저장했습니다.");
	}

	@Transactional
	public NotMatchedProductResponse recheck(NotMatchedProductRequest request) {
		boolean deletedByCode = false;
		if (request.productCode() != null && !request.productCode().isBlank()) {
			List<ProductNotMatchedItem> items = productNotMatchedItemRepository
					.findByKeywordAndMallProductCodeIn(request.keyword(), List.of(request.productCode()));
			deletedByCode = items.stream()
					.filter(item -> sameProductUrl(item.getProductUrl(), request.productUrl()))
					.findFirst()
					.map(item -> {
						item.markPossibleMatch();
						return true;
					})
					.orElse(false);
		}
		if (!deletedByCode) {
			productNotMatchedItemRepository
					.findByKeywordAndMallNameAndProductUrl(request.keyword(), request.mallName(), request.productUrl())
					.ifPresent(ProductNotMatchedItem::markPossibleMatch);
		}
		return new NotMatchedProductResponse(null, MatchStatus.POSSIBLE_MATCH, "재확인 대상으로 변경했습니다.");
	}

	private boolean sameProductUrl(String left, String right) {
		return left != null && right != null && left.equals(right);
	}

	private String normalizeReason(String reason) {
		return reason == null || reason.isBlank() ? "사용자가 동일하지 않은 상품으로 판정했습니다." : reason.trim();
	}
}
