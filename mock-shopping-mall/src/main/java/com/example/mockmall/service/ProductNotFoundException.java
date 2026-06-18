package com.example.mockmall.service;

public class ProductNotFoundException extends RuntimeException {

	public ProductNotFoundException(String productCode) {
		super("product not found: " + productCode);
	}
}
