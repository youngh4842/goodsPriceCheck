package com.example.goodsprice.crawler;

public enum CrawlerFailureType {
	SITE_ACCESS_FAILED("검색 사이트 접속 중 오류가 발생했습니다."),
	SEARCH_INPUT_NOT_FOUND("검색 화면 구조가 변경되어 조회할 수 없습니다."),
	RESULT_AREA_NOT_FOUND("검색 결과 영역을 찾을 수 없습니다."),
	TIMEOUT("검색 요청 시간이 초과되었습니다."),
	UNKNOWN("가격 조회 중 알 수 없는 오류가 발생했습니다.");

	private final String userMessage;

	CrawlerFailureType(String userMessage) {
		this.userMessage = userMessage;
	}

	public String getUserMessage() {
		return userMessage;
	}
}
