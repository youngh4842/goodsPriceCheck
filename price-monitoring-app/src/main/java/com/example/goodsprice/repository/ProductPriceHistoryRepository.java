package com.example.goodsprice.repository;

import com.example.goodsprice.domain.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {

	Optional<ProductPriceHistory> findTopByMallItemMallItemIdOrderByCrawledAtDescPriceHistoryIdDesc(Long mallItemId);

	Optional<ProductPriceHistory> findTopByMallItemMallItemIdOrderByPriceAscPriceHistoryIdAsc(Long mallItemId);

	List<ProductPriceHistory> findByMallItemProductMasterProductIdOrderByMallItemMallItemIdAscCrawledAtAscPriceHistoryIdAsc(
			Long productId);

	List<ProductPriceHistory> findByMallItemProductMasterProductIdAndCrawledAtBetweenOrderByMallItemMallItemIdAscCrawledAtAscPriceHistoryIdAsc(
			Long productId, LocalDateTime from, LocalDateTime to);

	List<ProductPriceHistory> findByMallItemMallItemIdOrderByCrawledAtAscPriceHistoryIdAsc(Long mallItemId);

	List<ProductPriceHistory> findByMallItemMallItemIdAndCrawledAtBetweenOrderByCrawledAtAscPriceHistoryIdAsc(
			Long mallItemId, LocalDateTime from, LocalDateTime to);

	Optional<ProductPriceHistory> findTopByMallItemMallItemIdOrderByCrawledAtAscPriceHistoryIdAsc(Long mallItemId);

	Optional<ProductPriceHistory> findTopByMallItemMallItemIdAndPreviousPriceNotNullOrderByCrawledAtDescPriceHistoryIdDesc(
			Long mallItemId);
}
