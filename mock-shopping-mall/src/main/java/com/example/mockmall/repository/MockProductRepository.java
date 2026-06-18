package com.example.mockmall.repository;

import com.example.mockmall.domain.MockProduct;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MockProductRepository {

	private final Map<String, MockProduct> products = new ConcurrentHashMap<>();

	@PostConstruct
	void initialize() {
		LocalDateTime now = LocalDateTime.now();
		save(new MockProduct("DF18CB8600ER", "Samsung Bespoke AirDresser DF18CB8600ER", 1_250_000L, now));
		save(new MockProduct("DF18-CB8600ER", "Samsung Bespoke AirDresser DF18-CB8600ER Package", 1_220_000L, now));
		save(new MockProduct("DF18CB8600ER-FILTER", "DF18CB8600ER Replacement Filter", 39_000L, now));
		save(new MockProduct("DF18CB8600ER-COVER", "DF18CB8600ER Dust Cover", 29_000L, now));
		save(new MockProduct("DF18CB8600EQ", "Samsung Bespoke AirDresser DF18CB8600EQ", 1_180_000L, now));
	}

	public List<MockProduct> findAll() {
		return new ArrayList<>(products.values()).stream()
				.sorted(Comparator.comparing(MockProduct::getProductCode))
				.toList();
	}

	public Optional<MockProduct> findByProductCode(String productCode) {
		return Optional.ofNullable(products.get(normalize(productCode)));
	}

	public MockProduct save(MockProduct product) {
		products.put(normalize(product.getProductCode()), product);
		return product;
	}

	private String normalize(String productCode) {
		return productCode == null ? "" : productCode.trim().toUpperCase();
	}
}
