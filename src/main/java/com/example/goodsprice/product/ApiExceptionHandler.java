package com.example.goodsprice.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler({InvalidKeywordException.class, MissingServletRequestParameterException.class})
	public ResponseEntity<ProductPriceSearchResponse> handleInvalidKeyword(Exception ex) {
		return ResponseEntity.badRequest().body(errorResponse("검색어를 입력해 주세요."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProductPriceSearchResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage() == null ? "요청값을 확인해 주세요." : error.getDefaultMessage())
				.orElse("요청값을 확인해 주세요.");
		return ResponseEntity.badRequest().body(errorResponse(message));
	}

	@ExceptionHandler(ProductPriceSearchException.class)
	public ResponseEntity<ProductPriceSearchResponse> handleSearchFailure(ProductPriceSearchException ex) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse(ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProductPriceSearchResponse> handleUnexpected(Exception ex) {
		return ResponseEntity.internalServerError().body(errorResponse("가격 조회 중 오류가 발생했습니다."));
	}

	private ProductPriceSearchResponse errorResponse(String message) {
		return new ProductPriceSearchResponse(null, LocalDateTime.now(), false, 0, null, List.of(), message);
	}
}
