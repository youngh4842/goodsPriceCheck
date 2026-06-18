package com.example.goodsprice.product;

import com.example.goodsprice.domain.ProductMallItem;
import com.example.goodsprice.domain.ProductMaster;
import com.example.goodsprice.repository.DailyIdGenerator;
import com.example.goodsprice.repository.ProductMallItemRepository;
import com.example.goodsprice.repository.ProductMasterRepository;
import com.example.goodsprice.repository.ProductPriceHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:price_history_test;MODE=MySQL;DATABASE_TO_UPPER=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class PriceHistoryServiceTests {

	@Autowired
	private PriceHistoryService priceHistoryService;

	@Autowired
	private ProductPriceHistoryRepository productPriceHistoryRepository;

	@Autowired
	private ProductMasterRepository productMasterRepository;

	@Autowired
	private ProductMallItemRepository productMallItemRepository;

	@Autowired
	private DailyIdGenerator dailyIdGenerator;

	@Test
	void savesFirstPrice() {
		ProductMallItem item = mallItem("A몰", "A-1");

		PriceHistoryResult result = priceHistoryService.recordPrice(item, 1000L, "1,000원",
				PriceHistorySourceType.USER_SEARCH, LocalDateTime.now());

		assertThat(result.historyCreated()).isTrue();
		assertThat(result.previousPrice()).isNull();
		assertThat(result.currentPrice()).isEqualTo(1000L);
		assertThat(result.changed()).isFalse();
		assertThat(result.newLowestPrice()).isTrue();
		assertThat(productPriceHistoryRepository.findByMallItemMallItemIdOrderByCrawledAtAscPriceHistoryIdAsc(
				item.getMallItemId())).hasSize(1);
	}

	@Test
	void skipsHistoryWhenPriceIsSame() {
		ProductMallItem item = mallItem("A몰", "A-2");
		priceHistoryService.recordPrice(item, 1000L, "1,000원", PriceHistorySourceType.USER_SEARCH,
				LocalDateTime.now());

		PriceHistoryResult result = priceHistoryService.recordPrice(item, 1000L, "1,000원",
				PriceHistorySourceType.MANUAL, LocalDateTime.now().plusHours(1));

		assertThat(result.historyCreated()).isFalse();
		assertThat(result.changed()).isFalse();
		assertThat(productPriceHistoryRepository.findByMallItemMallItemIdOrderByCrawledAtAscPriceHistoryIdAsc(
				item.getMallItemId())).hasSize(1);
	}

	@Test
	void savesPriceIncrease() {
		ProductMallItem item = mallItem("A몰", "A-3");
		priceHistoryService.recordPrice(item, 1000L, "1,000원", PriceHistorySourceType.USER_SEARCH,
				LocalDateTime.now());

		PriceHistoryResult result = priceHistoryService.recordPrice(item, 1250L, "1,250원",
				PriceHistorySourceType.MANUAL, LocalDateTime.now().plusHours(1));

		assertThat(result.historyCreated()).isTrue();
		assertThat(result.changed()).isTrue();
		assertThat(result.previousPrice()).isEqualTo(1000L);
		assertThat(result.changeAmount()).isEqualTo(250L);
		assertThat(result.changeRate()).isEqualByComparingTo(new BigDecimal("25.00"));
	}

	@Test
	void savesPriceDecreaseAndLowestPrice() {
		ProductMallItem item = mallItem("A몰", "A-4");
		priceHistoryService.recordPrice(item, 1000L, "1,000원", PriceHistorySourceType.USER_SEARCH,
				LocalDateTime.now());

		PriceHistoryResult result = priceHistoryService.recordPrice(item, 900L, "900원",
				PriceHistorySourceType.MANUAL, LocalDateTime.now().plusHours(1));

		assertThat(result.historyCreated()).isTrue();
		assertThat(result.changed()).isTrue();
		assertThat(result.changeAmount()).isEqualTo(-100L);
		assertThat(result.changeRate()).isEqualByComparingTo(new BigDecimal("-10.00"));
		assertThat(result.newLowestPrice()).isTrue();
	}

	@Test
	void rejectsNullOrInvalidPrice() {
		ProductMallItem item = mallItem("A몰", "A-5");

		assertThatThrownBy(() -> priceHistoryService.recordPrice(item, null, null, PriceHistorySourceType.MANUAL,
				LocalDateTime.now()))
				.isInstanceOf(ProductPriceSearchException.class);
		assertThatThrownBy(() -> priceHistoryService.recordPrice(item, 0L, "0원", PriceHistorySourceType.MANUAL,
				LocalDateTime.now()))
				.isInstanceOf(ProductPriceSearchException.class);
		assertThat(productPriceHistoryRepository.findByMallItemMallItemIdOrderByCrawledAtAscPriceHistoryIdAsc(
				item.getMallItemId())).isEmpty();
	}

	@Test
	void keepsMallItemHistoriesSeparated() {
		ProductMallItem left = mallItem("A몰", "A-6");
		ProductMallItem right = mallItem("B몰", "B-6");

		priceHistoryService.recordPrice(left, 1000L, "1,000원", PriceHistorySourceType.USER_SEARCH,
				LocalDateTime.now());
		priceHistoryService.recordPrice(right, 2000L, "2,000원", PriceHistorySourceType.USER_SEARCH,
				LocalDateTime.now());
		priceHistoryService.recordPrice(left, 900L, "900원", PriceHistorySourceType.MANUAL,
				LocalDateTime.now().plusHours(1));

		assertThat(productPriceHistoryRepository.findByMallItemMallItemIdOrderByCrawledAtAscPriceHistoryIdAsc(
				left.getMallItemId())).hasSize(2);
		assertThat(productPriceHistoryRepository.findByMallItemMallItemIdOrderByCrawledAtAscPriceHistoryIdAsc(
				right.getMallItemId())).hasSize(1);
	}

	private ProductMallItem mallItem(String mallName, String mallProductCode) {
		ProductMaster productMaster = productMasterRepository.save(new ProductMaster(
				dailyIdGenerator.nextProductId(LocalDate.now()), "MODEL-" + mallProductCode, "MODEL-" + mallProductCode,
				"테스트 상품", "브랜드"));
		return productMallItemRepository.save(new ProductMallItem(dailyIdGenerator.nextMallItemId(LocalDate.now()),
				productMaster, mallName, "테스트 상품 " + mallProductCode, mallProductCode, "테스트 상품",
				"https://example.com/" + mallProductCode, null, null, SaleType.PURCHASE, MatchStatus.MATCHED, 100,
				"테스트"));
	}
}
