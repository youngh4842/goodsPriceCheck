package com.example.goodsprice.product;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProductPeriodParser {

	private static final Pattern PERIOD_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,3})\\s*(개월|년)(?![가-힣A-Za-z0-9])");

	public String parse(String productName) {
		if (productName == null || productName.isBlank()) {
			return null;
		}
		Matcher matcher = PERIOD_PATTERN.matcher(productName);
		while (matcher.find()) {
			int number = Integer.parseInt(matcher.group(1));
			String unit = matcher.group(2);
			if ("개월".equals(unit) && number >= 1 && number <= 120) {
				return number + unit;
			}
			if ("년".equals(unit) && number >= 1 && number <= 20) {
				return number + unit;
			}
		}
		return null;
	}
}
