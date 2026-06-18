package com.example.mockmall.web;

import com.example.mockmall.service.MockProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

	private final MockProductService productService;

	public ProductApiController(MockProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public List<ProductResponse> listProducts() {
		return productService.listProducts().stream()
				.map(ProductResponse::from)
				.toList();
	}

	@GetMapping("/{productCode}")
	public ProductResponse getProduct(@PathVariable String productCode) {
		return ProductResponse.from(productService.getProduct(productCode));
	}

	@PutMapping("/{productCode}/price")
	public ProductResponse changePrice(@PathVariable String productCode, @Valid @RequestBody PriceChangeRequest request) {
		return ProductResponse.from(productService.changePrice(productCode, request.price()));
	}
}
