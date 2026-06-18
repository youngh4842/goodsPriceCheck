package com.example.mockmall.web;

import com.example.mockmall.service.InvalidPriceException;
import com.example.mockmall.service.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidPriceException.class)
	public ResponseEntity<ErrorResponse> handleInvalidPrice(InvalidPriceException exception) {
		return ResponseEntity.badRequest().body(ErrorResponse.of(exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		return ResponseEntity.badRequest().body(ErrorResponse.of("price is required"));
	}

	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(exception.getMessage()));
	}

	public record ErrorResponse(String message, LocalDateTime timestamp) {
		static ErrorResponse of(String message) {
			return new ErrorResponse(message, LocalDateTime.now());
		}
	}
}
