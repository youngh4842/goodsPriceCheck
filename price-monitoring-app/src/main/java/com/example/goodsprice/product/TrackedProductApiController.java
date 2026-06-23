package com.example.goodsprice.product;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class TrackedProductApiController {

	private final TrackedProductService trackedProductService;
	private final NotMatchedProductService notMatchedProductService;

	public TrackedProductApiController(TrackedProductService trackedProductService,
			NotMatchedProductService notMatchedProductService) {
		this.trackedProductService = trackedProductService;
		this.notMatchedProductService = notMatchedProductService;
	}

	@PostMapping("/api/tracked-products")
	@ResponseStatus(HttpStatus.CREATED)
	public TrackedProductResponse register(@Valid @RequestBody TrackedProductRequest request) {
		return trackedProductService.register(request);
	}

	@GetMapping("/api/tracked-products")
	public List<TrackedProductGroupResponse> findTrackedProducts() {
		return trackedProductService.findTrackedProducts();
	}

	@GetMapping("/api/tracked-products/{productId}/price-history")
	public ProductPriceHistoryResponse findPriceHistory(@PathVariable Long productId,
			@RequestParam(required = false) Long mallItemId,
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to) {
		return trackedProductService.findPriceHistory(productId, mallItemId, parseDateTime(from, true),
				parseDateTime(to, false));
	}

	@PostMapping("/api/tracked-products/{productId}/refresh")
	public RefreshTrackedProductResponse refreshTrackedProduct(@PathVariable Long productId) {
		return trackedProductService.refreshTrackedProduct(productId);
	}

	@DeleteMapping("/api/tracked-products/mall-items/{mallItemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unregisterMallItem(@PathVariable Long mallItemId) {
		trackedProductService.unregisterMallItem(mallItemId);
	}

	@PatchMapping("/api/mall-items/{mallItemId}/match-status")
	public MallItemStatusResponse changeMatchStatus(@PathVariable Long mallItemId,
			@Valid @RequestBody MatchStatusUpdateRequest request) {
		return trackedProductService.changeMatchStatus(mallItemId, request);
	}

	@PostMapping("/api/not-matched-products")
	@ResponseStatus(HttpStatus.CREATED)
	public NotMatchedProductResponse registerNotMatched(@Valid @RequestBody NotMatchedProductRequest request) {
		return notMatchedProductService.register(request);
	}

	@PostMapping("/api/not-matched-products/recheck")
	public NotMatchedProductResponse recheckNotMatched(@Valid @RequestBody NotMatchedProductRequest request) {
		return notMatchedProductService.recheck(request);
	}

	private LocalDateTime parseDateTime(String value, boolean startOfDay) {
		if (value == null || value.isBlank()) {
			return null;
		}
		if (value.length() == 10) {
			LocalDate date = LocalDate.parse(value);
			return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59);
		}
		return LocalDateTime.parse(value);
	}
}
