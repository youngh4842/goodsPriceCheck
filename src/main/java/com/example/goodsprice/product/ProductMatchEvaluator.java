package com.example.goodsprice.product;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ProductMatchEvaluator {

	private static final Pattern MODEL_CODE_QUERY = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)[A-Z0-9]{5,}$");
	private static final Pattern MODEL_TOKEN = Pattern.compile("(?=.*[A-Z])(?=.*\\d)[A-Z0-9]{5,}");
	private static final List<String> EXCLUDED_KEYWORDS = List.of(
			"필터", "부품", "악세서리", "액세서리", "케이스", "커버", "거치대", "호환", "리필", "전용", "교체용"
	);

	public ProductMatchResult evaluate(String keyword, String mallName, String mallProductName, String normalizedProductName,
			String productCode, Long price, String productPeriod) {
		List<String> reasons = new ArrayList<>();
		String normalizedKeywordCode = normalizeModelCode(keyword);
		boolean keywordLooksLikeModelCode = MODEL_CODE_QUERY.matcher(normalizedKeywordCode).matches();
		String searchableName = safe(mallProductName) + " " + safe(normalizedProductName);
		String normalizedSearchableName = normalizeModelCode(searchableName);
		String lowerName = searchableName.toLowerCase(Locale.ROOT);
		SaleType saleType = decideSaleType(lowerName, price, productPeriod);
		List<String> excludedKeywords = EXCLUDED_KEYWORDS.stream()
				.filter(lowerName::contains)
				.toList();

		if (saleType == SaleType.USED) {
			reasons.add("중고 상품 키워드가 포함되어 있습니다.");
		}
		if (!excludedKeywords.isEmpty()) {
			reasons.add("제외 검토 키워드가 포함되어 있습니다: " + String.join(", ", excludedKeywords));
		}

		if (keywordLooksLikeModelCode && normalizedSearchableName.contains(normalizedKeywordCode)) {
			reasons.add("모델코드가 정확히 일치합니다.");
			if (excludedKeywords.isEmpty()) {
				return new ProductMatchResult(saleType, MatchStatus.MATCHED, 100, reasons);
			}
			return new ProductMatchResult(saleType, MatchStatus.NOT_MATCHED, 35, reasons);
		}

		if (keywordLooksLikeModelCode) {
			Set<String> modelCodes = extractModelCodes(searchableName);
			if (!modelCodes.isEmpty()) {
				reasons.add("검색어와 다른 모델코드가 포함되어 있습니다: " + String.join(", ", modelCodes));
				return new ProductMatchResult(saleType, MatchStatus.NOT_MATCHED, 20, reasons);
			}
			reasons.add("모델코드를 상품명에서 확인하지 못했습니다.");
			return new ProductMatchResult(saleType, MatchStatus.UNKNOWN, 0, reasons);
		}

		double tokenMatchRatio = tokenMatchRatio(keyword, searchableName);
		if (tokenMatchRatio >= 0.65 && excludedKeywords.isEmpty()) {
			reasons.add("주요 상품명 토큰이 대부분 일치합니다.");
			return new ProductMatchResult(saleType, MatchStatus.POSSIBLE_MATCH, 70, reasons);
		}
		if (tokenMatchRatio > 0) {
			reasons.add("일부 상품명 토큰만 일치합니다.");
			return new ProductMatchResult(saleType, MatchStatus.POSSIBLE_MATCH, 45, reasons);
		}
		reasons.add("일치 여부를 판단할 정보가 부족합니다.");
		return new ProductMatchResult(saleType, MatchStatus.UNKNOWN, 0, reasons);
	}

	public String normalizeModelCode(String value) {
		return safe(value).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
	}

	private SaleType decideSaleType(String lowerName, Long price, String productPeriod) {
		if (lowerName.contains("중고")) {
			return SaleType.USED;
		}
		if (lowerName.contains("렌탈") || (productPeriod != null && !productPeriod.isBlank())) {
			return SaleType.RENTAL;
		}
		if (price != null) {
			return SaleType.PURCHASE;
		}
		return SaleType.UNKNOWN;
	}

	private Set<String> extractModelCodes(String value) {
		Set<String> codes = new LinkedHashSet<>();
		String normalizedValue = safe(value).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", " ");
		Arrays.stream(normalizedValue.split("\\s+"))
				.filter(token -> MODEL_TOKEN.matcher(token).matches())
				.forEach(codes::add);
		return codes;
	}

	private double tokenMatchRatio(String keyword, String productName) {
		Set<String> keywordTokens = meaningfulTokens(keyword);
		if (keywordTokens.isEmpty()) {
			return 0;
		}
		Set<String> productTokens = meaningfulTokens(productName);
		long matched = keywordTokens.stream().filter(productTokens::contains).count();
		return (double) matched / keywordTokens.size();
	}

	private Set<String> meaningfulTokens(String value) {
		Set<String> tokens = new LinkedHashSet<>();
		for (String token : safe(value).toLowerCase(Locale.ROOT).replaceAll("[^0-9a-z가-힣]+", " ").split("\\s+")) {
			if (token.length() >= 2) {
				tokens.add(token);
			}
		}
		return tokens;
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}
}
