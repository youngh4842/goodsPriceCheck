package com.example.mockmall.service;

import com.example.mockmall.domain.MockPriceChangeLog;
import com.example.mockmall.domain.MockProduct;
import com.example.mockmall.domain.SaleStatus;
import com.example.mockmall.domain.SaleType;
import com.example.mockmall.repository.MockPriceChangeLogRepository;
import com.example.mockmall.repository.MockProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class MockProductService implements CommandLineRunner {

	private final MockProductRepository productRepository;
	private final MockPriceChangeLogRepository changeLogRepository;

	public MockProductService(MockProductRepository productRepository, MockPriceChangeLogRepository changeLogRepository) {
		this.productRepository = productRepository;
		this.changeLogRepository = changeLogRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (productRepository.existsByNormalizedProductCode(normalizeProductCode("DF18CB8600ER"))) {
			return;
		}
		create(seed("DF18CB8600ER", "삼성 비스포크 에어드레서 DF18CB8600ER", "삼성", "Mock Mall A", 1_250_000L,
				SaleType.PURCHASE, SaleStatus.ACTIVE, "Mock Mall A의 정상 구매 상품입니다."));
		create(seed("DF18-CB8600ER", "삼성 비스포크 에어드레서 DF18CB8600ER 특가", "삼성", "Mock Mall B", 1_220_000L,
				SaleType.PURCHASE, SaleStatus.ACTIVE, "같은 상품코드를 하이픈 포함 형태로 등록한 테스트 상품입니다."));
		create(seed("df18 cb8600er", "삼성 비스포크 에어드레서 DF18CB8600ER 패키지", "삼성", "Mock Mall C", 1_290_000L,
				SaleType.PURCHASE, SaleStatus.ACTIVE, "같은 상품코드를 공백/소문자 형태로 등록한 테스트 상품입니다."));
		create(seed("DF18CB8600ER-FILTER", "DF18CB8600ER 전용 필터", "삼성", "Mock Parts", 39_000L,
				SaleType.PURCHASE, SaleStatus.ACTIVE, "동일 상품 판정 제외 테스트용 액세서리입니다."));
		create(seed("DF18CB8600ER-COVER", "DF18CB8600ER 호환 커버", "삼성", "Mock Parts", 29_000L,
				SaleType.PURCHASE, SaleStatus.ACTIVE, "호환 상품 판정 테스트용 액세서리입니다."));
		create(seed("DF18CB8600ER-RENTAL", "삼성 비스포크 에어드레서 DF18CB8600ER 렌탈 상품", "삼성", "Mock Rental", 49_900L,
				SaleType.RENTAL, SaleStatus.ACTIVE, "렌탈 상품 판정 테스트용 상품입니다."));
		create(seed("DF18CB8600EQ", "삼성 비스포크 에어드레서 DF18CB8600EQ 유사 모델", "삼성", "Mock Mall D", 1_180_000L,
				SaleType.PURCHASE, SaleStatus.ACTIVE, "모델 코드가 일부 다른 유사 상품입니다."));
	}

	@Transactional(readOnly = true)
	public List<MockProduct> list(String keyword, boolean includeHidden) {
		if (keyword == null || keyword.isBlank()) {
			return includeHidden ? productRepository.findAll() : productRepository.findBySaleStatusNotOrderByMallNameAscMockProductIdAsc(SaleStatus.HIDDEN);
		}
		String normalized = normalizeProductCode(keyword);
		List<MockProduct> products;
		if (normalized.isBlank()) {
			products = productRepository
					.findByProductNameContainingIgnoreCaseOrBrandNameContainingIgnoreCaseOrderByMallNameAscMockProductIdAsc(
							keyword, keyword);
		}
		else {
			products = productRepository
					.findByNormalizedProductCodeContainingIgnoreCaseOrProductNameContainingIgnoreCaseOrBrandNameContainingIgnoreCaseOrderByMallNameAscMockProductIdAsc(
							normalized, keyword, keyword);
		}
		if (includeHidden) {
			return products;
		}
		return products.stream().filter(product -> product.getSaleStatus() != SaleStatus.HIDDEN).toList();
	}

	@Transactional(readOnly = true)
	public MockProduct get(Long productId) {
		return productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Mock 상품을 찾을 수 없습니다."));
	}

	@Transactional
	public MockProduct create(MockProductRequest request) {
		MockProduct product = new MockProduct(request.productCode().trim(), normalizeProductCode(request.productCode()),
				request.productName().trim(), trim(request.brandName()), request.mallName().trim(), request.price(),
				request.saleType() == null ? SaleType.PURCHASE : request.saleType(),
				request.saleStatus() == null ? SaleStatus.ACTIVE : request.saleStatus(), trim(request.productDescription()),
				trim(request.productImageUrl()));
		product.changePriceVisibleYn(request.priceVisibleYn());
		return productRepository.save(product);
	}

	@Transactional
	public MockProduct update(Long productId, MockProductRequest request) {
		MockProduct product = get(productId);
		product.update(request.productCode().trim(), normalizeProductCode(request.productCode()), request.productName().trim(),
				trim(request.brandName()), request.mallName().trim(), request.price(),
				request.saleType() == null ? SaleType.PURCHASE : request.saleType(),
				request.saleStatus() == null ? SaleStatus.ACTIVE : request.saleStatus(), trim(request.productDescription()),
				trim(request.productImageUrl()), request.priceVisibleYn());
		return product;
	}

	@Transactional
	public MockProduct changePrice(Long productId, Long changedPrice, String reason) {
		if (changedPrice == null || changedPrice <= 0) {
			throw new IllegalArgumentException("가격은 0원보다 커야 합니다.");
		}
		MockProduct product = get(productId);
		Long previousPrice = product.getPrice();
		product.changePrice(changedPrice);
		changeLogRepository.save(new MockPriceChangeLog(product, previousPrice, changedPrice, trim(reason)));
		return product;
	}

	@Transactional
	public MockProduct adjustPrice(Long productId, String mode) {
		MockProduct product = get(productId);
		long current = product.getPrice();
		long next = switch (mode) {
			case "UP_10000" -> current + 10_000L;
			case "DOWN_10000" -> current - 10_000L;
			case "DOWN_50000" -> current - 50_000L;
			case "UP_5P" -> Math.round(current * 1.05);
			case "DOWN_5P" -> Math.round(current * 0.95);
			case "DOWN_10P" -> Math.round(current * 0.90);
			default -> throw new IllegalArgumentException("지원하지 않는 빠른 변경 값입니다.");
		};
		return changePrice(productId, next, "빠른 가격 변경: " + mode);
	}

	@Transactional
	public MockProduct changeSaleStatus(Long productId, SaleStatus saleStatus) {
		MockProduct product = get(productId);
		product.changeSaleStatus(saleStatus);
		return product;
	}

	@Transactional
	public MockProduct changePriceVisible(Long productId, String priceVisibleYn) {
		MockProduct product = get(productId);
		product.changePriceVisibleYn(priceVisibleYn);
		return product;
	}

	@Transactional
	public void hide(Long productId) {
		get(productId).changeSaleStatus(SaleStatus.HIDDEN);
	}

	@Transactional(readOnly = true)
	public List<MockPriceChangeLog> priceHistory(Long productId) {
		return changeLogRepository.findByMockProductMockProductIdOrderByChangedAtDescChangeLogIdDesc(productId);
	}

	public String normalizeProductCode(String value) {
		if (value == null) {
			return "";
		}
		return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
	}

	private MockProductRequest seed(String productCode, String productName, String brandName, String mallName, Long price,
			SaleType saleType, SaleStatus saleStatus, String description) {
		return new MockProductRequest(productCode, productName, brandName, mallName, price, saleType, saleStatus,
				description, "https://placehold.co/480x320?text=" + normalizeProductCode(productCode), "Y");
	}

	private String trim(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
