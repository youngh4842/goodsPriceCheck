package com.example.goodsprice.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMatchEvaluatorTests {

	private final ProductMatchEvaluator evaluator = new ProductMatchEvaluator();

	@Test
	void marksExactModelCodeAsMatched() {
		ProductMatchResult result = evaluator.evaluate("DF18-CB8600ER", "mall",
				"삼성 비스포크 에어드레서 DF18CB8600ER", "삼성 비스포크 에어드레서", null, 1_000_000L, null);

		assertThat(result.matchStatus()).isEqualTo(MatchStatus.MATCHED);
		assertThat(result.matchScore()).isEqualTo(100);
		assertThat(result.saleType()).isEqualTo(SaleType.PURCHASE);
	}

	@Test
	void separatesAccessoryByExcludedKeyword() {
		ProductMatchResult result = evaluator.evaluate("DF18CB8600ER", "mall",
				"삼성 에어드레서 DF18CB8600ER 전용 필터", "삼성 에어드레서 전용 필터", null, 20_000L, null);

		assertThat(result.matchStatus()).isEqualTo(MatchStatus.NOT_MATCHED);
		assertThat(result.matchReasons()).anyMatch(reason -> reason.contains("제외 검토 키워드"));
	}

	@Test
	void marksRentalByRentalWord() {
		ProductMatchResult result = evaluator.evaluate("DF18CB8600ER", "mall",
				"렌탈 삼성 에어드레서 DF18CB8600ER", "삼성 에어드레서", null, 44_900L, null);

		assertThat(result.saleType()).isEqualTo(SaleType.RENTAL);
		assertThat(result.matchStatus()).isEqualTo(MatchStatus.MATCHED);
	}

	@Test
	void normalizesModelCodeForComparison() {
		assertThat(evaluator.normalizeModelCode("df18 cb-8600_er")).isEqualTo("DF18CB8600ER");
	}
}
