package com.example.goodsprice.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductNameNormalizerTests {

	private final ProductNameNormalizer normalizer = new ProductNameNormalizer();
	private final ProductPeriodParser periodParser = new ProductPeriodParser();

	@Test
	void removesPlainPriceFromProductName() {
		String productName = normalizer.removePrice("삼성 올인원 의류 관리 에어드레서 DF18CB8600ER 60개월 44900", 44_900L);

		assertThat(productName).isEqualTo("삼성 올인원 의류 관리 에어드레서 DF18CB8600ER 60개월");
		assertThat(periodParser.parse(productName)).isEqualTo("60개월");
	}

	@Test
	void removesCommaPriceWithWonFromProductName() {
		String productName = normalizer.removePrice("렌탈 DF18CB8600ER 5년 44,900원", 44_900L);

		assertThat(productName).isEqualTo("렌탈 DF18CB8600ER 5년");
		assertThat(periodParser.parse(productName)).isEqualTo("5년");
	}

	@Test
	void removesKeywordAndPeriodFromProductName() {
		String productName = normalizer.removeKeywordAndPeriod(
				"삼성 에어드레서 비스포크 DF18CB8600ER 60개월", "DF18CB8600ER", "60개월");

		assertThat(productName).isEqualTo("삼성 에어드레서 비스포크");
	}

	@Test
	void removesKeywordAndSpacedPeriodFromProductName() {
		String productName = normalizer.removeKeywordAndPeriod(
				"삼성 에어드레서 비스포크 DF18CB8600ER 60 개월", "DF18CB8600ER", "60개월");

		assertThat(productName).isEqualTo("삼성 에어드레서 비스포크");
	}

	@Test
	void removesEmptyParenthesesAfterKeywordRemoval() {
		String productName = normalizer.removeKeywordAndPeriod(
				"렌탈 [삼성] BESPOKE 에어드레서 새틴베이지 (DF18CB8600ER)", "DF18CB8600ER", null);

		assertThat(productName).isEqualTo("렌탈 [삼성] BESPOKE 에어드레서 새틴베이지");
	}

	@Test
	void removesRentalWordFromProductName() {
		String productName = normalizer.removeRentalWord("렌탈 비스포크 에어드레서");

		assertThat(productName).isEqualTo("비스포크 에어드레서");
	}
}
