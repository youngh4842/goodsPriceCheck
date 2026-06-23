package com.example.mockmall.repository;

import com.example.mockmall.domain.MockProduct;
import com.example.mockmall.domain.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockProductRepository extends JpaRepository<MockProduct, Long> {

	List<MockProduct> findBySaleStatusNotOrderByMallNameAscMockProductIdAsc(SaleStatus saleStatus);

	List<MockProduct> findByNormalizedProductCodeContainingIgnoreCaseOrProductNameContainingIgnoreCaseOrBrandNameContainingIgnoreCaseOrderByMallNameAscMockProductIdAsc(
			String normalizedProductCode, String productName, String brandName);

	List<MockProduct> findByProductNameContainingIgnoreCaseOrBrandNameContainingIgnoreCaseOrderByMallNameAscMockProductIdAsc(
			String productName, String brandName);

	boolean existsByNormalizedProductCode(String normalizedProductCode);
}
