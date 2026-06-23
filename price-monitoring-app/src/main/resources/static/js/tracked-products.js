const trackedSummary = document.querySelector("#trackedSummary");
const trackedMessage = document.querySelector("#trackedMessage");
const trackedGroups = document.querySelector("#trackedGroups");
const toast = document.querySelector("#toast");
const trackedProductsKey = "goodsPrice.trackedProducts";
let openedMallItemId = null;
let toastTimer = null;

loadTrackedProducts();

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
		saveCachedGroups(groups);
		renderGroups(groups, false);
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
		section.className = "tracked-group tracked-search-block";

		const head = document.createElement("div");
		head.className = "tracked-search-head";
		const title = document.createElement("h3");
		title.textContent = `검색어: ${group.keyword || "-"} (${(group.items || []).length}건)`;
		head.appendChild(title);
		if (cached) {
			const unavailable = document.createElement("span");
			unavailable.className = "muted-text";
			unavailable.textContent = "서버 조회 후 재조회 가능";
			head.appendChild(unavailable);
		} else {
			const refreshButton = document.createElement("button");
			refreshButton.type = "button";
			refreshButton.className = "mini-button secondary-mini-button group-refresh-button";
			refreshButton.textContent = "재조회";
			refreshButton.addEventListener("click", () => refreshTrackedGroup(group, refreshButton));
			head.appendChild(refreshButton);
		}
		section.appendChild(head);

		const productList = document.createElement("div");
		productList.className = "tracked-product-list";
		for (const item of group.items || []) {
			productList.appendChild(productBlock(item, cached));
		}
		section.appendChild(productList);
		trackedGroups.appendChild(section);
	}
}

function productBlock(item, cached) {
	const block = document.createElement("article");
	block.className = "tracked-product-card";
	block.dataset.productId = item.productId || "";
	block.dataset.mallItemId = item.mallItemId || "";

	const header = document.createElement("div");
	header.className = "tracked-product-card-header";

	const titleArea = document.createElement("div");
	titleArea.className = "tracked-product-title";
	const mall = document.createElement("span");
	mall.className = "tracked-product-mall";
	mall.textContent = item.mallName || "-";
	const name = document.createElement("strong");
	name.textContent = item.productName || "-";
	titleArea.append(mall, name);

	const actions = document.createElement("div");
	actions.className = "row-actions";
	if (cached || !item.productId || !item.mallItemId) {
		const unavailable = document.createElement("span");
		unavailable.className = "muted-text";
		unavailable.textContent = "서버 조회 필요";
		actions.appendChild(unavailable);
	} else {
		const unregisterButton = document.createElement("button");
		unregisterButton.type = "button";
		unregisterButton.className = "mini-button danger-mini-button";
		unregisterButton.textContent = "등록해지";
		unregisterButton.addEventListener("click", () => unregisterTrackedProduct(item.mallItemId, unregisterButton));

		const historyButton = document.createElement("button");
		historyButton.type = "button";
		historyButton.className = "mini-button history-button";
		historyButton.textContent = "이력";
		historyButton.addEventListener("click", () => togglePriceHistory(item, block, historyButton));
		actions.append(unregisterButton, historyButton);
	}
	header.append(titleArea, actions);

	const detail = document.createElement("dl");
	detail.className = "tracked-product-detail";
	appendDetail(detail, "상품코드", item.productCode || "-");
	appendDetail(detail, "현재 가격", item.priceText || formatPrice(item.price));
	appendDetail(detail, "최저 가격", formatPrice(item.lowestPrice));
	appendDetail(detail, "마지막 조회", formatDateTime(item.lastCrawledAt));
	appendLinkDetail(detail, "링크", item.productUrl);

	const historySlot = document.createElement("div");
	historySlot.className = "tracked-history-slot";
	historySlot.hidden = true;

	block.append(header, detail, historySlot);
	return block;
}

function appendDetail(parent, label, value) {
	const item = document.createElement("div");
	const term = document.createElement("dt");
	term.textContent = label;
	const description = document.createElement("dd");
	description.textContent = value;
	item.append(term, description);
	parent.appendChild(item);
}

function appendLinkDetail(parent, label, productUrl) {
	const item = document.createElement("div");
	const term = document.createElement("dt");
	term.textContent = label;
	const description = document.createElement("dd");
	if (!productUrl) {
		description.textContent = "-";
	} else {
		const link = document.createElement("a");
		link.href = productUrl;
		link.target = "_blank";
		link.rel = "noopener noreferrer";
		link.textContent = "바로가기";
		description.appendChild(link);
	}
	item.append(term, description);
	parent.appendChild(item);
}

async function unregisterTrackedProduct(mallItemId, button) {
	if (!confirm("해당 상품의 추적 등록을 해지할까요?")) {
		return;
	}
	const previousText = button.textContent;
	button.disabled = true;
	button.textContent = "해지 중";
	try {
		const response = await fetch(`/api/tracked-products/mall-items/${mallItemId}`, {method: "DELETE"});
		if (!response.ok) {
			const data = await safeJson(response);
			throw new Error(data.message || "추적 등록 해지에 실패했습니다.");
		}
		removeCachedMallItem(mallItemId);
		showTrackedMessage("추적 등록을 해지했습니다.");
		await loadTrackedProducts();
	} catch (error) {
		showTrackedMessage(error.message);
		button.disabled = false;
		button.textContent = previousText;
	}
}

async function togglePriceHistory(item, block, button) {
	const historySlot = block.querySelector(".tracked-history-slot");
	if (!historySlot || !item.productId || !item.mallItemId) {
		return;
	}
	if (!historySlot.hidden && openedMallItemId === item.mallItemId) {
		closeHistorySlot(historySlot);
		openedMallItemId = null;
		button.textContent = "이력";
		return;
	}

	closeAllHistorySlots();
	openedMallItemId = item.mallItemId;
	button.disabled = true;
	button.textContent = "조회 중";
	historySlot.hidden = false;
	historySlot.replaceChildren(loadingMessage("가격 이력을 조회 중입니다."));

	try {
		const response = await fetch(`/api/tracked-products/${item.productId}/price-history?mallItemId=${item.mallItemId}`);
		const data = await response.json();
		if (!response.ok) {
			throw new Error(data.message || "가격 이력을 조회하지 못했습니다.");
		}
		renderPriceHistory(historySlot, data, item);
		button.textContent = "닫기";
	} catch (error) {
		historySlot.replaceChildren(errorMessage(error.message));
		button.textContent = "이력";
	} finally {
		button.disabled = false;
	}
}

function closeAllHistorySlots() {
	for (const slot of document.querySelectorAll(".tracked-history-slot")) {
		closeHistorySlot(slot);
	}
	for (const historyButton of document.querySelectorAll(".history-button")) {
		historyButton.textContent = "이력";
	}
}

function closeHistorySlot(historySlot) {
	historySlot.hidden = true;
	historySlot.replaceChildren();
}

async function refreshTrackedGroup(group, button) {
	const productIds = uniqueProductIds(group.items || []);
	if (productIds.length === 0) {
		showTrackedMessage("재조회할 추적 상품이 없습니다.");
		return;
	}
	const beforeItems = snapshotItems(group.items || []);
	const previousText = button.textContent;
	button.disabled = true;
	button.textContent = "재조회 중";
	showTrackedMessage(`검색어 "${group.keyword || "-"}" 기준 ${productIds.length}개 상품을 재조회합니다.`);
	let successCount = 0;
	let failureCount = 0;
	let changedCount = 0;

	try {
		for (const productId of productIds) {
			const response = await fetch(`/api/tracked-products/${productId}/refresh`, {method: "POST"});
			const data = await response.json();
			if (!response.ok) {
				failureCount += 1;
				continue;
			}
			successCount += Number(data.successCount || 0);
			failureCount += Number(data.failureCount || 0);
			changedCount += Number(data.changedCount || 0);
		}
		showTrackedMessage(`검색어 재조회 완료: 성공 ${successCount}건 / 실패 ${failureCount}건 / 변경 ${changedCount}건`);
		const refreshedGroups = await fetchTrackedGroups();
		saveCachedGroups(refreshedGroups);
		renderGroups(refreshedGroups, false);
		alertChangedProducts(changedItems(beforeItems, refreshedGroups));
	} catch (error) {
		showTrackedMessage(error.message || "검색어 기준 재조회에 실패했습니다.");
	} finally {
		button.disabled = false;
		button.textContent = previousText;
	}
}

function uniqueProductIds(items) {
	return [...new Set(items
			.map((item) => item.productId)
			.filter((productId) => productId !== null && productId !== undefined && String(productId).trim() !== ""))];
}

async function fetchTrackedGroups() {
	const response = await fetch("/api/tracked-products");
	const groups = await response.json();
	if (!response.ok) {
		throw new Error(groups.message || "추적 상품 목록을 다시 조회하지 못했습니다.");
	}
	return groups;
}

function snapshotItems(items) {
	const snapshots = new Map();
	for (const item of items) {
		if (item.mallItemId === null || item.mallItemId === undefined) {
			continue;
		}
		snapshots.set(Number(item.mallItemId), {
			mallName: item.mallName || "-",
			productCode: item.productCode || "-",
			productName: item.productName || "-",
			price: Number.isFinite(item.price) ? item.price : null
		});
	}
	return snapshots;
}

function changedItems(beforeItems, groups) {
	const changed = [];
	for (const group of groups || []) {
		for (const item of group.items || []) {
			const before = beforeItems.get(Number(item.mallItemId));
			if (!before) {
				continue;
			}
			const currentPrice = Number.isFinite(item.price) ? item.price : null;
			if (before.price !== currentPrice) {
				changed.push({
					mallName: item.mallName || before.mallName,
					productCode: item.productCode || before.productCode,
					productName: item.productName || before.productName,
					beforePrice: before.price,
					currentPrice
				});
			}
		}
	}
	return changed;
}

function alertChangedProducts(items) {
	if (items.length === 0) {
		return;
	}
	const lines = items.map((item) =>
			`${item.mallName} / ${item.productCode} / ${item.productName}: ${formatPrice(item.beforePrice)} -> ${formatPrice(item.currentPrice)}`);
	alert(`가격이 변경된 추적 상품이 있습니다.\n\n${lines.join("\n")}`);
}

function renderPriceHistory(container, data, selectedItem) {
	container.replaceChildren();

	const historyHeader = document.createElement("div");
	historyHeader.className = "tracked-history-head";
	const title = document.createElement("h4");
	title.textContent = `가격 이력 - ${selectedItem.productCode || data.productCode || selectedItem.productName || data.productId}`;
	const closeButton = document.createElement("button");
	closeButton.type = "button";
	closeButton.className = "secondary-button";
	closeButton.textContent = "닫기";
	closeButton.addEventListener("click", () => {
		closeHistorySlot(container);
		openedMallItemId = null;
		const block = container.closest(".tracked-product-card");
		const historyButton = block?.querySelector(".history-button");
		if (historyButton) {
			historyButton.textContent = "이력";
		}
	});
	historyHeader.append(title, closeButton);
	container.appendChild(historyHeader);

	const items = (data.items || []).filter((historyItem) => Number(historyItem.mallItemId) === Number(selectedItem.mallItemId));
	if (items.length === 0) {
		container.appendChild(loadingMessage("해당 상품코드의 가격 이력이 없습니다."));
		return;
	}

	for (const item of items) {
		const section = document.createElement("section");
		section.className = "tracked-history-block";
		const itemTitle = document.createElement("h5");
		itemTitle.textContent = `${item.mallName || "-"} / 상품코드 ${item.productCode || "-"} / 현재 ${formatPrice(item.currentPrice)} / 최저 ${formatPrice(item.lowestPrice)}`;
		section.appendChild(itemTitle);

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
		const histories = item.history || [];
		if (histories.length === 0) {
			const tr = document.createElement("tr");
			const empty = document.createElement("td");
			empty.colSpan = 7;
			empty.className = "empty-history";
			empty.textContent = "가격 이력이 없습니다.";
			tr.appendChild(empty);
			tbody.appendChild(tr);
		} else {
			for (const history of histories) {
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
		}
		table.appendChild(tbody);
		tableWrap.appendChild(table);
		section.appendChild(tableWrap);
		container.appendChild(section);
	}
}

function loadingMessage(text) {
	const message = document.createElement("div");
	message.className = "inline-message";
	message.textContent = text;
	return message;
}

function errorMessage(text) {
	const message = document.createElement("div");
	message.className = "message";
	message.textContent = text;
	return message;
}

function showTrackedMessage(text) {
	trackedMessage.textContent = text;
	trackedMessage.hidden = false;
}

function showToast(text) {
	if (!toast) {
		return;
	}
	if (toastTimer) {
		clearTimeout(toastTimer);
	}
	toast.textContent = text;
	toast.hidden = false;
	toastTimer = setTimeout(() => {
		toast.hidden = true;
		toast.textContent = "";
	}, 3000);
}

function td(text, column) {
	const cell = document.createElement("td");
	cell.dataset.column = column;
	cell.textContent = text;
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

function removeCachedMallItem(mallItemId) {
	const groups = readCachedGroups()
			.map((group) => ({
				...group,
				items: (group.items || []).filter((item) => Number(item.mallItemId) !== Number(mallItemId))
			}))
			.filter((group) => (group.items || []).length > 0);
	saveCachedGroups(groups);
}

async function safeJson(response) {
	try {
		return await response.json();
	} catch (error) {
		return {};
	}
}
