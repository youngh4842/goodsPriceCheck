package com.example.goodsprice.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPeriodParserTests {

	private final ProductPeriodParser parser = new ProductPeriodParser();

	@Test
	void extractsRentalPeriod() {
		assertThat(parser.parse("삼성 에어드레서 DF18CB8600ER 60개월")).isEqualTo("60개월");
		assertThat(parser.parse("렌탈 삼성 에어드레서 DF18CB8600ER 5년")).isEqualTo("5년");
	}

	@Test
	void returnsNullWhenPeriodDoesNotExist() {
		assertThat(parser.parse("삼성 에어드레서 DF18CB8600ER")).isNull();
		assertThat(parser.parse("2026년형 삼성 에어드레서 DF18CB8600ER")).isNull();
	}
}
