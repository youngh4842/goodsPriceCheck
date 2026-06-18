package com.example.goodsprice.product;

public class ProductPriceSearchException extends RuntimeException {

	public ProductPriceSearchException(String message) {
		super(message);
	}

	public ProductPriceSearchException(String message, Throwable cause) {
		super(message, cause);
	}
}
