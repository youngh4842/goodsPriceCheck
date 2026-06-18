package com.example.mockmall.service;

import com.example.mockmall.domain.MockProduct;
import com.example.mockmall.repository.MockProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockProductService {

	private final MockProductRepository productRepository;

	public MockProductService(MockProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public List<MockProduct> listProducts() {
		return productRepository.findAll();
	}

	public MockProduct getProduct(String productCode) {
		return productRepository.findByProductCode(productCode)
				.orElseThrow(() -> new ProductNotFoundException(productCode));
	}

	public MockProduct changePrice(String productCode, long price) {
		if (price < 0) {
			throw new InvalidPriceException("price must be greater than or equal to 0");
		}
		MockProduct product = getProduct(productCode);
		product.changePrice(price);
		return productRepository.save(product);
	}
}
