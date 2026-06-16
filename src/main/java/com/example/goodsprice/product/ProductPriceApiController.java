package com.example.goodsprice.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductPriceApiController {

	private final ProductPriceSearchService searchService;

	public ProductPriceApiController(ProductPriceSearchService searchService) {
		this.searchService = searchService;
	}

	@GetMapping("/price-search")
	public ResponseEntity<ProductPriceSearchResponse> search(@RequestParam String keyword) {
		return ResponseEntity.ok(searchService.search(keyword));
	}
}
