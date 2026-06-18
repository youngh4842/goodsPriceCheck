package com.example.goodsprice.crawler;

import org.springframework.stereotype.Component;

@Component
public class PriceParser {

	public Long parse(String priceText) {
		if (priceText == null || priceText.isBlank()) {
			return null;
		}
		String digits = priceText.replaceAll("[^0-9]", "");
		if (digits.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(digits);
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	public String format(Long price) {
		if (price == null) {
			return "가격정보 없음";
		}
		return String.format("%,d원", price);
	}
}
