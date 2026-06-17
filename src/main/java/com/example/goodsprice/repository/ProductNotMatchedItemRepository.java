package com.example.goodsprice.repository;

import com.example.goodsprice.domain.ProductNotMatchedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductNotMatchedItemRepository extends JpaRepository<ProductNotMatchedItem, Long> {

	Optional<ProductNotMatchedItem> findByKeywordAndMallNameAndProductUrl(String keyword, String mallName,
			String productUrl);

	List<ProductNotMatchedItem> findByKeywordAndProductUrlIn(String keyword, Collection<String> productUrls);

	List<ProductNotMatchedItem> findByKeywordAndMallProductCodeIn(String keyword, Collection<String> mallProductCodes);
}
