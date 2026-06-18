package com.example.goodsprice.product;

import com.example.goodsprice.domain.ProductMallItem;
import com.example.goodsprice.domain.ProductPriceHistory;
import com.example.goodsprice.repository.DailyIdGenerator;
import com.example.goodsprice.repository.ProductPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class PriceHistoryService {

	private final ProductPriceHistoryRepository productPriceHistoryRepository;
	private final DailyIdGenerator dailyIdGenerator;

	public PriceHistoryService(ProductPriceHistoryRepository productPriceHistoryRepository,
			DailyIdGenerator dailyIdGenerator) {
		this.productPriceHistoryRepository = productPriceHistoryRepository;
		this.dailyIdGenerator = dailyIdGenerator;
	}

	@Transactional
	public PriceHistoryResult recordPrice(ProductMallItem mallItem, Long currentPrice, String priceText,
			PriceHistorySourceType sourceType, LocalDateTime crawledAt) {
		if (currentPrice == null || currentPrice <= 0) {
			throw new ProductPriceSearchException("정상 가격이 없어 가격 이력을 저장할 수 없습니다.");
		}
		LocalDateTime effectiveCrawledAt = crawledAt == null ? LocalDateTime.now() : crawledAt;
		ProductPriceHistory previousHistory = productPriceHistoryRepository
				.findTopByMallItemMallItemIdOrderByCrawledAtDescPriceHistoryIdDesc(mallItem.getMallItemId())
				.orElse(null);
		Long previousPrice = previousHistory == null ? null : previousHistory.getPrice();
		if (previousPrice != null && previousPrice.equals(currentPrice)) {
			mallItem.updatePrice(currentPrice, priceText);
			return new PriceHistoryResult(mallItem.getMallItemId(), previousPrice, currentPrice, false, 0L,
					BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), false, false);
		}

		Long changeAmount = previousPrice == null ? null : currentPrice - previousPrice;
		BigDecimal changeRate = calculateChangeRate(previousPrice, currentPrice);
		boolean newLowestPrice = isNewLowestPrice(mallItem.getMallItemId(), currentPrice);
		ProductPriceHistory history = new ProductPriceHistory(
				dailyIdGenerator.nextPriceHistoryId(effectiveCrawledAt.toLocalDate()), mallItem, currentPrice, priceText,
				previousPrice, changeAmount, changeRate, newLowestPrice, sourceType, effectiveCrawledAt);
		productPriceHistoryRepository.save(history);
		mallItem.updatePrice(currentPrice, priceText);
		return new PriceHistoryResult(mallItem.getMallItemId(), previousPrice, currentPrice, previousPrice != null,
				changeAmount, changeRate, newLowestPrice, true);
	}

	private BigDecimal calculateChangeRate(Long previousPrice, Long currentPrice) {
		if (previousPrice == null || previousPrice == 0L) {
			return null;
		}
		return BigDecimal.valueOf(currentPrice - previousPrice)
				.multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(previousPrice), 2, RoundingMode.HALF_UP);
	}

	private boolean isNewLowestPrice(Long mallItemId, Long currentPrice) {
		return productPriceHistoryRepository.findTopByMallItemMallItemIdOrderByPriceAscPriceHistoryIdAsc(mallItemId)
				.map(history -> currentPrice < history.getPrice())
				.orElse(true);
	}
}
