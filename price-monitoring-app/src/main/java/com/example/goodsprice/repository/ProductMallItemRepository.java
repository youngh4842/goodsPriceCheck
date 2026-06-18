package com.example.goodsprice.repository;

import com.example.goodsprice.domain.ProductMallItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductMallItemRepository extends JpaRepository<ProductMallItem, Long> {

	Optional<ProductMallItem> findByMallNameAndProductUrl(String mallName, String productUrl);

	List<ProductMallItem> findByMallProductCodeIn(Collection<String> mallProductCodes);

	List<ProductMallItem> findByProductMasterProductId(Long productId);
}
