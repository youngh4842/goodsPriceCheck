package com.example.goodsprice.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPeriodParserTests {

	private final ProductPeriodParser parser = new ProductPeriodParser();

	@Test
	void parsesMonthPeriod() {
		assertThat(parser.parse("삼성 에어드레서 DF18CB8600ER 60개월")).isEqualTo("60개월");
	}

	@Test
	void parsesYearPeriod() {
		assertThat(parser.parse("삼성 에어드레서 DF18CB8600ER 5년")).isEqualTo("5년");
	}

	@Test
	void ignoresModelYearExpression() {
		assertThat(parser.parse("삼성 에어드레서 2026년형 DF18CB8600ER")).isNull();
	}

	@Test
	void returnsNullWhenPeriodIsMissing() {
		assertThat(parser.parse("삼성 에어드레서 DF18CB8600ER")).isNull();
	}
}
