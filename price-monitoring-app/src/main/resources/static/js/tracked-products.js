const trackedSummary = document.querySelector("#trackedSummary");
const trackedMessage = document.querySelector("#trackedMessage");
const trackedGroups = document.querySelector("#trackedGroups");
const historyPanel = document.querySelector("#historyPanel");
const historyTitle = document.querySelector("#historyTitle");
const refreshHistoryButton = document.querySelector("#refreshHistoryButton");
const historyMessage = document.querySelector("#historyMessage");
const historyContent = document.querySelector("#historyContent");
const trackedProductsKey = "goodsPrice.trackedProducts";
let selectedProductId = null;

loadTrackedProducts();

refreshHistoryButton.addEventListener("click", async () => {
	if (!selectedProductId) {
		return;
	}
	refreshHistoryButton.disabled = true;
	refreshHistoryButton.textContent = "새로고침 중";
	try {
		const response = await fetch(`/api/tracked-products/${selectedProductId}/refresh`, {method: "POST"});
		const data = await response.json();
		if (!response.ok) {
			throw new Error(data.message || "수동 새로고침에 실패했습니다.");
		}
		await loadPriceHistory(selectedProductId);
		await loadTrackedProducts();
	} catch (error) {
		showHistoryMessage(error.message);
	} finally {
		refreshHistoryButton.disabled = false;
		refreshHistoryButton.textContent = "수동 새로고침";
	}
});

async function loadTrackedProducts() {
	const cachedGroups = readCachedGroups();
	if (cachedGroups.length > 0) {
		renderGroups(cachedGroups, true);
	}

	try {
		const response = await fetch("/api/tracked-products");
		const groups = await response.json();
		if (!response.ok) {
			throw new Error(groups.message || "추적 상품 목록을 조회하지 못했습니다.");
		}
		if (countItems(groups) > 0) {
			saveCachedGroups(groups);
			renderGroups(groups, false);
			return;
		}
		if (cachedGroups.length === 0) {
			renderGroups([], false);
		}
	} catch (error) {
		if (cachedGroups.length === 0) {
			trackedSummary.textContent = "조회 실패";
			showTrackedMessage(error.message);
		}
	}
}

function renderGroups(groups, cached) {
	trackedGroups.replaceChildren();
	const itemCount = countItems(groups);
	trackedSummary.textContent = `검색어 ${groups.length}개 / 상품 ${itemCount}건${cached ? " (브라우저 저장)" : ""}`;
	if (itemCount === 0) {
		showTrackedMessage("등록된 추적 상품이 없습니다.");
		return;
	}
	trackedMessage.hidden = true;
	for (const group of groups) {
		const section = document.createElement("section");
		section.className = "tracked-group";
		const title = document.createElement("h3");
		title.textContent = `${group.keyword} (${group.items.length}건)`;
		section.appendChild(title);

		const tableWrap = document.createElement("div");
		tableWrap.className = "table-wrap";
		const table = document.createElement("table");
		table.className = "tracked-table";
		table.innerHTML = `
			<colgroup>
				<col class="product-code-col">
				<col class="product-name-col">
				<col class="price-col">
				<col class="price-col">
				<col class="price-col">
				<col class="link-col">
				<col class="action-col">
			</colgroup>
			<thead>
				<tr>
					<th>상품코드</th>
					<th>상품명</th>
					<th>현재 가격</th>
					<th>최저 가격</th>
					<th>마지막 조회</th>
					<th>링크</th>
					<th>작업</th>
				</tr>
			</thead>
		`;
		const tbody = document.createElement("tbody");
		for (const item of group.items) {
			const tr = document.createElement("tr");
			tr.appendChild(td(item.productCode || "-", "product-code"));
			tr.appendChild(td(item.productName || "-", "product-name"));
			tr.appendChild(td(item.priceText || formatPrice(item.price), "price"));
			tr.appendChild(td(formatPrice(item.lowestPrice), "price"));
			tr.appendChild(td(formatDateTime(item.lastCrawledAt), "period"));
			tr.appendChild(linkCell(item.productUrl));
			tr.appendChild(actionCell(item, cached));
			tbody.appendChild(tr);
		}
		table.appendChild(tbody);
		tableWrap.appendChild(table);
		section.appendChild(tableWrap);
		trackedGroups.appendChild(section);
	}
}

function actionCell(item, cached) {
	const cell = document.createElement("td");
	cell.dataset.column = "action";
	if (cached || !item.productId) {
		cell.textContent = "-";
		return cell;
	}
	const actions = document.createElement("div");
	actions.className = "row-actions";
	const historyButton = document.createElement("button");
	historyButton.type = "button";
	historyButton.className = "mini-button";
	historyButton.textContent = "이력";
	historyButton.addEventListener("click", () => loadPriceHistory(item.productId));
	const refreshButton = document.createElement("button");
	refreshButton.type = "button";
	refreshButton.className = "mini-button secondary-mini-button";
	refreshButton.textContent = "재조회";
	refreshButton.addEventListener("click", () => refreshTrackedProduct(item.productId, refreshButton));
	actions.append(historyButton, refreshButton);
	cell.appendChild(actions);
	return cell;
}

async function refreshTrackedProduct(productId, button) {
	const previousText = button.textContent;
	button.disabled = true;
	button.textContent = "재조회 중";
	showTrackedMessage("Mock 쇼핑몰 상세 HTML을 다시 크롤링하는 중입니다.");
	try {
		const response = await fetch(`/api/tracked-products/${productId}/refresh`, {method: "POST"});
		const data = await response.json();
		if (!response.ok) {
			throw new Error(data.message || "수동 재조회에 실패했습니다.");
		}
		showTrackedMessage(`재조회 완료: 성공 ${data.successCount}건 / 실패 ${data.failureCount}건 / 변경 ${data.changedCount}건`);
		if (selectedProductId === productId) {
			await loadPriceHistory(productId);
		}
		await loadTrackedProducts();
	} catch (error) {
		showTrackedMessage(error.message);
	} finally {
		button.disabled = false;
		button.textContent = previousText;
	}
}

async function loadPriceHistory(productId) {
	selectedProductId = productId;
	historyPanel.hidden = false;
	historyMessage.hidden = true;
	historyContent.replaceChildren();
	try {
		const response = await fetch(`/api/tracked-products/${productId}/price-history`);
		const data = await response.json();
		if (!response.ok) {
			throw new Error(data.message || "가격 이력을 조회하지 못했습니다.");
		}
		renderPriceHistory(data);
	} catch (error) {
		showHistoryMessage(error.message);
	}
}

function renderPriceHistory(data) {
	historyTitle.textContent = `가격 이력 - ${data.productName || data.productCode || data.productId}`;
	for (const item of data.items || []) {
		const section = document.createElement("section");
		section.className = "tracked-group";
		const title = document.createElement("h3");
		title.textContent = `${item.mallName} / 현재 ${formatPrice(item.currentPrice)} / 최저 ${formatPrice(item.lowestPrice)}`;
		section.appendChild(title);

		const tableWrap = document.createElement("div");
		tableWrap.className = "table-wrap";
		const table = document.createElement("table");
		table.className = "tracked-table history-table";
		table.innerHTML = `
			<thead>
				<tr>
					<th>가격</th>
					<th>이전 가격</th>
					<th>증감</th>
					<th>증감률</th>
					<th>최저가</th>
					<th>수집 구분</th>
					<th>조회 시각</th>
				</tr>
			</thead>
		`;
		const tbody = document.createElement("tbody");
		for (const history of item.history || []) {
			const tr = document.createElement("tr");
			tr.appendChild(td(formatPrice(history.price), "price"));
			tr.appendChild(td(formatPrice(history.previousPrice), "price"));
			tr.appendChild(td(formatSignedPrice(history.changeAmount), "price"));
			tr.appendChild(td(formatRate(history.changeRate), "score"));
			tr.appendChild(td(history.lowestPriceYn === "Y" ? "Y" : "N", "rental"));
			tr.appendChild(td(history.sourceType || "-", "sale"));
			tr.appendChild(td(formatDateTime(history.crawledAt), "period"));
			tbody.appendChild(tr);
		}
		table.appendChild(tbody);
		tableWrap.appendChild(table);
		section.appendChild(tableWrap);
		historyContent.appendChild(section);
	}
}

function showTrackedMessage(text) {
	trackedMessage.textContent = text;
	trackedMessage.hidden = false;
}

function showHistoryMessage(text) {
	historyMessage.textContent = text;
	historyMessage.hidden = false;
}

function td(text, column) {
	const cell = document.createElement("td");
	cell.dataset.column = column;
	cell.textContent = text;
	return cell;
}

function linkCell(productUrl) {
	const cell = document.createElement("td");
	cell.dataset.column = "link";
	if (!productUrl) {
		cell.textContent = "-";
		return cell;
	}
	const link = document.createElement("a");
	link.href = productUrl;
	link.target = "_blank";
	link.rel = "noopener noreferrer";
	link.textContent = "바로가기";
	cell.appendChild(link);
	return cell;
}

function formatPrice(price) {
	return Number.isFinite(price) ? `${price.toLocaleString()}원` : "-";
}

function formatSignedPrice(price) {
	if (!Number.isFinite(price)) {
		return "-";
	}
	return `${price > 0 ? "+" : ""}${price.toLocaleString()}원`;
}

function formatRate(rate) {
	return Number.isFinite(Number(rate)) ? `${Number(rate).toFixed(2)}%` : "-";
}

function formatDateTime(value) {
	return value ? String(value).replace("T", " ").slice(0, 16) : "-";
}

function countItems(groups) {
	return groups.reduce((sum, group) => sum + (group.items || []).length, 0);
}

function readCachedGroups() {
	try {
		const rawValue = localStorage.getItem(trackedProductsKey);
		return rawValue ? JSON.parse(rawValue) : [];
	} catch (error) {
		return [];
	}
}

function saveCachedGroups(groups) {
	localStorage.setItem(trackedProductsKey, JSON.stringify(groups));
}
