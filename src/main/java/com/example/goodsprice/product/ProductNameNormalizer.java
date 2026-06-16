package com.example.goodsprice.product;

import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ProductNameNormalizer {

	public String removePrice(String productName, Long price) {
		if (productName == null || productName.isBlank() || price == null) {
			return normalizeSpaces(productName);
		}
		String plainPrice = String.valueOf(price);
		String commaPrice = NumberFormat.getNumberInstance(Locale.US).format(price);
		String pricePattern = Pattern.quote(commaPrice) + "|" + Pattern.quote(plainPrice);
		String normalized = productName.replaceAll("(?<!\\d)(" + pricePattern + ")\\s*원?(?!\\d)", " ");
		return normalizeSpaces(normalized);
	}

	public String removeKeywordAndPeriod(String productName, String keyword, String productPeriod) {
		String normalized = removeKeyword(productName, keyword);
		normalized = removePeriod(normalized, productPeriod);
		return normalizeSpaces(normalized);
	}

	public String removeRentalWord(String productName) {
		if (productName == null || productName.isBlank()) {
			return normalizeSpaces(productName);
		}
		return normalizeSpaces(productName.replace("렌탈", " "));
	}

	private String removeKeyword(String productName, String keyword) {
		if (productName == null || productName.isBlank() || keyword == null || keyword.isBlank()) {
			return productName;
		}
		return productName.replaceAll("(?i)" + Pattern.quote(keyword), " ");
	}

	private String removePeriod(String productName, String productPeriod) {
		if (productName == null || productName.isBlank() || productPeriod == null || productPeriod.isBlank()) {
			return productName;
		}
		String periodPattern = productPeriod.replaceAll("(\\d+)(개월|년)", "$1\\\\s*$2");
		return productName.replaceAll("(?<!\\d)" + periodPattern + "(?![가-힣A-Za-z0-9])", " ");
	}

	private String normalizeSpaces(String text) {
		if (text == null) {
			return null;
		}
		return text.replaceAll("\\(\\s*\\)", " ")
				.replaceAll("\\[\\s*\\]", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}
}
