package com.example.mockmall.service;

public class InvalidPriceException extends RuntimeException {

	public InvalidPriceException(String message) {
		super(message);
	}
}
