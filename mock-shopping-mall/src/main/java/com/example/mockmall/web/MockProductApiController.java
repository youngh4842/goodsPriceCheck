package com.example.mockmall.web;

import com.example.mockmall.domain.MockPriceChangeLog;
import com.example.mockmall.domain.MockProduct;
import com.example.mockmall.service.MockProductRequest;
import com.example.mockmall.service.MockProductService;
import com.example.mockmall.service.PriceChangeRequest;
import com.example.mockmall.service.SaleStatusChangeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mock-products")
public class MockProductApiController {

	private final MockProductService productService;

	public MockProductApiController(MockProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public List<MockProductResponse> list(@RequestParam(required = false) String keyword) {
		return productService.list(keyword, true).stream().map(MockProductResponse::from).toList();
	}

	@GetMapping("/{productId}")
	public MockProductResponse get(@PathVariable Long productId) {
		return MockProductResponse.from(productService.get(productId));
	}

	@PostMapping
	public MockProductResponse create(@Valid @RequestBody MockProductRequest request) {
		return MockProductResponse.from(productService.create(request));
	}

	@PutMapping("/{productId}")
	public MockProductResponse update(@PathVariable Long productId, @Valid @RequestBody MockProductRequest request) {
		return MockProductResponse.from(productService.update(productId, request));
	}

	@PatchMapping("/{productId}/price")
	public MockProductResponse changePrice(@PathVariable Long productId, @Valid @RequestBody PriceChangeRequest request) {
		return MockProductResponse.from(productService.changePrice(productId, request.price(), request.changeReason()));
	}

	@PatchMapping("/{productId}/sale-status")
	public MockProductResponse changeSaleStatus(@PathVariable Long productId,
			@Valid @RequestBody SaleStatusChangeRequest request) {
		return MockProductResponse.from(productService.changeSaleStatus(productId, request.saleStatus()));
	}

	@GetMapping("/{productId}/price-history")
	public List<MockPriceChangeLogResponse> priceHistory(@PathVariable Long productId) {
		return productService.priceHistory(productId).stream().map(MockPriceChangeLogResponse::from).toList();
	}

	record MockPriceChangeLogResponse(Long previousPrice, Long changedPrice, String changeReason, String changedAt) {
		static MockPriceChangeLogResponse from(MockPriceChangeLog log) {
			return new MockPriceChangeLogResponse(log.getPreviousPrice(), log.getChangedPrice(), log.getChangeReason(),
					log.getChangedAt().toString());
		}
	}
}
