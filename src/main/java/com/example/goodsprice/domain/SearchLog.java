package com.example.goodsprice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "TB_SEARCH_LOG")
public class SearchLog {

	@Id
	@Column(name = "search_id")
	private Long searchId;

	@Column(nullable = false)
	private String keyword;

	@Column(name = "result_count", nullable = false)
	private int resultCount;

	@Column(name = "success_yn", length = 1, nullable = false)
	private String successYn;

	@Column(name = "error_message", length = 1000)
	private String errorMessage;

	@Column(name = "searched_at", nullable = false)
	private LocalDateTime searchedAt;

	@OneToMany(mappedBy = "searchLog", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<ProductPriceResult> results = new ArrayList<>();

	protected SearchLog() {
	}

	public SearchLog(String keyword, LocalDateTime searchedAt) {
		this.keyword = keyword;
		this.searchedAt = searchedAt;
		this.resultCount = 0;
		this.successYn = "N";
	}

	public void assignSearchId(Long searchId) {
		this.searchId = searchId;
	}

	public void markSuccess(int resultCount) {
		this.resultCount = resultCount;
		this.successYn = "Y";
		this.errorMessage = null;
	}

	public void markFailure(String errorMessage) {
		this.resultCount = 0;
		this.successYn = "N";
		this.errorMessage = errorMessage;
	}

	public void addResult(ProductPriceResult result) {
		results.add(result);
		result.assignSearchLog(this);
	}

	public Long getSearchId() {
		return searchId;
	}

	public String getKeyword() {
		return keyword;
	}

	public int getResultCount() {
		return resultCount;
	}

	public String getSuccessYn() {
		return successYn;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public LocalDateTime getSearchedAt() {
		return searchedAt;
	}
}
