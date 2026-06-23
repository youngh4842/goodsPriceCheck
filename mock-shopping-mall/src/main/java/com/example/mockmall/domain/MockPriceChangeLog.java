package com.example.mockmall.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "MOCK_PRICE_CHANGE_LOG")
public class MockPriceChangeLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "change_log_id")
	private Long changeLogId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "mock_product_id", nullable = false)
	private MockProduct mockProduct;

	@Column(name = "previous_price", nullable = false)
	private Long previousPrice;

	@Column(name = "changed_price", nullable = false)
	private Long changedPrice;

	@Column(name = "change_reason")
	private String changeReason;

	@Column(name = "changed_at", nullable = false)
	private LocalDateTime changedAt;

	protected MockPriceChangeLog() {
	}

	public MockPriceChangeLog(MockProduct mockProduct, Long previousPrice, Long changedPrice, String changeReason) {
		this.mockProduct = mockProduct;
		this.previousPrice = previousPrice;
		this.changedPrice = changedPrice;
		this.changeReason = changeReason;
		this.changedAt = LocalDateTime.now();
	}

	public Long getPreviousPrice() {
		return previousPrice;
	}

	public Long getChangedPrice() {
		return changedPrice;
	}

	public String getChangeReason() {
		return changeReason;
	}

	public LocalDateTime getChangedAt() {
		return changedAt;
	}
}
