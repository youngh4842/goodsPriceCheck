package com.example.goodsprice.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class DailyIdGenerator {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

	@PersistenceContext
	private EntityManager entityManager;

	private final Map<String, Long> lastIssuedIds = new HashMap<>();

	public synchronized Long nextSearchId(LocalDate date) {
		return nextId("tb_search_log", "search_id", date);
	}

	public synchronized Long nextResultId(LocalDate date) {
		return nextId("tb_product_price_result", "result_id", date);
	}

	public synchronized Long nextProductId(LocalDate date) {
		return nextId("product_master", "product_id", date);
	}

	public synchronized Long nextMallItemId(LocalDate date) {
		return nextId("product_mall_item", "mall_item_id", date);
	}

	public synchronized Long nextNotMatchedId(LocalDate date) {
		return nextId("product_not_matched_item", "not_matched_id", date);
	}

	public synchronized Long nextPriceHistoryId(LocalDate date) {
		return nextId("product_price_history", "price_history_id", date);
	}

	public synchronized Long nextCrawlRunId(LocalDate date) {
		return nextId("crawl_run_log", "crawl_run_id", date);
	}

	public synchronized Long nextCrawlItemLogId(LocalDate date) {
		return nextId("crawl_item_log", "crawl_item_log_id", date);
	}

	private Long nextId(String tableName, String idColumn, LocalDate date) {
		long prefix = Long.parseLong(date.format(DATE_FORMAT));
		long start = prefix * 10_000L;
		long end = start + 9_999L;
		String key = tableName + ":" + prefix;
		Number maxId = (Number) entityManager
				.createNativeQuery("select max(" + idColumn + ") from " + tableName
						+ " where " + idColumn + " between ? and ?")
				.setParameter(1, start)
				.setParameter(2, end)
				.getSingleResult();
		long lastIssuedId = lastIssuedIds.getOrDefault(key, start);
		long currentMaxId = Math.max(maxId == null ? start : maxId.longValue(), lastIssuedId);
		long sequence = currentMaxId - start + 1L;
		if (sequence > 9_999L) {
			throw new IllegalStateException("일일 ID 채번 한도를 초과했습니다: " + tableName);
		}
		long nextId = start + sequence;
		lastIssuedIds.put(key, nextId);
		return nextId;
	}
}
