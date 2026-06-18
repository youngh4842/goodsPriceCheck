const form = document.querySelector("#searchForm");
const keywordInput = document.querySelector("#keyword");
const button = document.querySelector("#searchButton");
const clearButton = document.querySelector("#clearButton");
const crawlerSourceBadge = document.querySelector("#crawlerSourceBadge");
const crawlerSourceButtons = document.querySelectorAll("[data-crawler-source]");
const summary = document.querySelector("#summary");
const message = document.querySelector("#message");
const toast = document.querySelector("#toast");
const resultsBody = document.querySelector("#results");
const resultTable = document.querySelector("#resultTable");
const toggleProductCodeButton = document.querySelector("#toggleProductCode");
const rentalFilterButtons = document.querySelectorAll("[data-rental-filter]");
const priceSortButton = document.querySelector("#priceSortButton");
const priceSortIcon = document.querySelector("#priceSortIcon");
const pagination = document.querySelector("#pagination");
const storageKey = "goodsPrice.lastSearch";
const searchHistoryKey = "goodsPrice.searchHistory";
const trackedProductsKey = "goodsPrice.trackedProducts";
const pageSize = 10;
let currentData = null;
let currentRentalFilter = "ALL";
let currentPriceSort = "ASC";
let currentPage = 1;
let toastTimer = null;
let currentCrawlerSource = "HSMOA";

const saleTypeLabels = {
	PURCHASE: "구매",
	RENTAL: "렌탈",
	USED: "중고",
	UNKNOWN: "미확인"
};

const matchStatusLabels = {
	MATCHED: "일치",
	POSSIBLE_MATCH: "확인 필요",
	NOT_MATCHED: "동일 아님",
	UNKNOWN: "미확인"
};

initializeState();
loadCrawlerSource();

crawlerSourceButtons.forEach((sourceButton) => {
	sourceButton.addEventListener("click", () => changeCrawlerSource(sourceButton.dataset.crawlerSource));
});

toggleProductCodeButton.addEventListener("click", () => {
	const hidden = resultTable.classList.toggle("hide-product-code");
	toggleProductCodeButton.textContent = hidden ? "상품코드 보이기" : "상품코드 숨기기";
	toggleProductCodeButton.setAttribute("aria-pressed", String(hidden));
});

rentalFilterButtons.forEach((filterButton) => {
	filterButton.addEventListener("click", () => {
		currentRentalFilter = filterButton.dataset.rentalFilter;
		currentPage = 1;
		rentalFilterButtons.forEach((button) => button.classList.toggle("active", button === filterButton));
		renderResults(currentData);
		saveState();
	});
});

priceSortButton.addEventListener("click", () => {
	currentPriceSort = currentPriceSort === "ASC" ? "DESC" : "ASC";
	currentPage = 1;
	updatePriceSortButton();
	renderResults(currentData);
	saveState();
});

clearButton.addEventListener("click", () => {
	clearState();
	keywordInput.value = "";
	localStorage.removeItem(storageKey);
	summary.textContent = "검색어를 입력해 주세요.";
});

form.addEventListener("submit", async (event) => {
	event.preventDefault();
	const keyword = keywordInput.value.trim();
	clearState();
	resetRentalFilter();
	resetPriceSort();
	currentPage = 1;

	if (!keyword) {
		showMessage("검색어를 입력해 주세요.");
		return;
	}

	button.disabled = true;
	button.textContent = "검색 중";
	summary.textContent = `${keyword} 조회 중입니다.`;

	try {
		const response = await fetch(`/api/products/price-search?keyword=${encodeURIComponent(keyword)}`);
		const data = await response.json();
		if (!response.ok) {
			showMessage(data.message || "가격 조회 중 오류가 발생했습니다.");
			summary.textContent = "조회 실패";
			return;
		}
		currentData = data;
		renderResults(data);
		saveState();
		saveSearchHistory(data);
	} catch (error) {
		showMessage("서버와 통신하는 중 오류가 발생했습니다.");
		summary.textContent = "조회 실패";
	} finally {
		button.disabled = false;
		button.textContent = "검색";
	}
});

async function loadCrawlerSource() {
	try {
		const response = await fetch("/api/products/crawler-source");
		const data = await response.json();
		if (!response.ok) {
			throw new Error(data.message || "크롤링 상태를 조회하지 못했습니다.");
		}
		applyCrawlerSource(data.source, data.label);
	} catch (error) {
		applyCrawlerSource("HSMOA", "홈쇼핑");
	}
}

async function changeCrawlerSource(source) {
	if (!source || source === currentCrawlerSource) {
		return;
	}
	const sourceButton = [...crawlerSourceButtons].find((button) => button.dataset.crawlerSource === source);
	crawlerSourceButtons.forEach((button) => button.disabled = true);
	if (sourceButton) {
		sourceButton.textContent = "전환 중";
	}
	try {
		const response = await fetch("/api/products/crawler-source", {
			method: "PUT",
			headers: {"Content-Type": "application/json; charset=utf-8"},
			body: JSON.stringify({source})
		});
		const data = await response.json();
		if (!response.ok) {
			throw new Error(data.message || "크롤링 대상을 변경하지 못했습니다.");
		}
		applyCrawlerSource(data.source, data.label);
		clearState();
		summary.textContent = `${data.label} 크롤링으로 전환했습니다.`;
		showToast(`${data.label} 연결`);
	} catch (error) {
		showMessage(error.message);
	} finally {
		crawlerSourceButtons.forEach((button) => {
			button.disabled = false;
			button.textContent = button.dataset.crawlerSource === "MOCK_MALL" ? "TEST" : "홈쇼핑";
		});
	}
}

function applyCrawlerSource(source, labelText) {
	currentCrawlerSource = source || "HSMOA";
	const label = labelText || (currentCrawlerSource === "MOCK_MALL" ? "TEST" : "홈쇼핑");
	crawlerSourceBadge.textContent = label;
	crawlerSourceBadge.dataset.source = currentCrawlerSource;
	crawlerSourceButtons.forEach((button) => {
		button.classList.toggle("active", button.dataset.crawlerSource === currentCrawlerSource);
		button.setAttribute("aria-pressed", String(button.dataset.crawlerSource === currentCrawlerSource));
	});
}

function clearState() {
	message.hidden = true;
	message.textContent = "";
	resultsBody.replaceChildren();
	pagination.replaceChildren();
	pagination.hidden = true;
	currentData = null;
}

function showMessage(text) {
	message.textContent = text;
	message.hidden = false;
}

function showToast(text) {
	if (toastTimer) {
		clearTimeout(toastTimer);
	}
	toast.textContent = text;
	toast.hidden = false;
	toastTimer = setTimeout(() => {
		toast.hidden = true;
		toast.textContent = "";
	}, 2200);
}

function renderResults(data) {
	resultsBody.replaceChildren();
	pagination.replaceChildren();
	pagination.hidden = true;
	if (!data) {
		return;
	}
	const filteredResults = sortResults(filterResults(data.results || []));
	const totalPages = Math.max(1, Math.ceil(filteredResults.length / pageSize));
	currentPage = Math.min(Math.max(currentPage, 1), totalPages);
	const pageStart = (currentPage - 1) * pageSize;
	const pageResults = filteredResults.slice(pageStart, pageStart + pageSize);
	const sourceText = Number.isInteger(data.sourceTotalCount)
		? ` / 홈쇼핑모아 전체 ${data.sourceTotalCount.toLocaleString()}건`
		: "";
	const filterText = currentRentalFilter === "ALL" ? "" : ` / 필터 ${filteredResults.length}건`;
	const pageText = filteredResults.length > 0 ? ` / ${currentPage}/${totalPages}페이지` : "";
	summary.textContent = `${data.keyword} 수집 결과 ${data.resultCount}건${sourceText}${filterText}${pageText}`;
	if (!data.found) {
		showMessage(data.message || "검색 결과가 없습니다.");
		return;
	}
	if (filteredResults.length === 0) {
		showMessage("필터 조건에 맞는 결과가 없습니다.");
		return;
	}

	message.hidden = true;
	message.textContent = "";
	for (let index = 0; index < pageResults.length; index += 1) {
		const item = pageResults[index];
		const rowNo = pageStart + index + 1;
		const tr = document.createElement("tr");
		tr.dataset.productCode = item.productCode || "";
		tr.dataset.productUrl = item.productUrl || "";
		tr.appendChild(td(rowNo, "no"));
		tr.appendChild(td(item.mallName || "-", "mall"));
		tr.appendChild(td(item.productCode || "-", "product-code"));
		tr.appendChild(td(item.productName || "-", "product-name"));
		tr.appendChild(td(label(saleTypeLabels, item.saleType), "sale"));
		tr.appendChild(td(item.rentalYn || "X", "rental"));
		tr.appendChild(td(item.productPeriod || "-", "period"));
		tr.appendChild(td(item.priceText || "가격정보 없음", "price"));
		tr.appendChild(statusCell(item.matchStatus));
		tr.appendChild(td(Number.isFinite(item.matchScore) ? `${item.matchScore}` : "-", "score"));
		tr.appendChild(td((item.matchReasons || []).join(" / ") || "-", "reason"));
		tr.appendChild(linkCell(item.productUrl));
		tr.appendChild(actionCell(item, tr));
		resultsBody.appendChild(tr);
	}
	renderPagination(totalPages);
}

function renderPagination(totalPages) {
	if (totalPages <= 1) {
		return;
	}
	pagination.hidden = false;
	pagination.appendChild(pageButton("이전", currentPage - 1, currentPage === 1));
	for (let page = 1; page <= totalPages; page += 1) {
		const pageControl = pageButton(String(page), page, false);
		pageControl.classList.toggle("active", page === currentPage);
		pageControl.setAttribute("aria-current", page === currentPage ? "page" : "false");
		pagination.appendChild(pageControl);
	}
	pagination.appendChild(pageButton("다음", currentPage + 1, currentPage === totalPages));
}

function pageButton(text, targetPage, disabled) {
	const pageControl = document.createElement("button");
	pageControl.type = "button";
	pageControl.className = "page-button";
	pageControl.textContent = text;
	pageControl.disabled = disabled;
	pageControl.addEventListener("click", () => {
		currentPage = targetPage;
		renderResults(currentData);
		saveState();
	});
	return pageControl;
}

function statusCell(matchStatus) {
	const cell = td(label(matchStatusLabels, matchStatus), "match");
	cell.dataset.status = matchStatus || "UNKNOWN";
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

function actionCell(item, row) {
	const cell = document.createElement("td");
	cell.dataset.column = "action";
	const actionWrap = document.createElement("div");
	actionWrap.className = "action-buttons";
	if (item.tracked && item.mallItemId) {
		row.dataset.mallItemId = item.mallItemId;
	}

	const registerButton = document.createElement("button");
	registerButton.type = "button";
	registerButton.className = "mini-button";
	setRegisterButtonState(registerButton, Boolean(item.tracked), Boolean(item.rejected));
	registerButton.addEventListener("click", () => registerTrackedProduct(item, row, registerButton));
	actionWrap.appendChild(registerButton);

	const rejectButton = document.createElement("button");
	rejectButton.type = "button";
	rejectButton.className = "mini-button muted";
	setRejectButtonState(rejectButton, Boolean(item.rejected));
	rejectButton.addEventListener("click", () => handleRejectButton(item, row, rejectButton, registerButton));
	actionWrap.appendChild(rejectButton);
	cell.appendChild(actionWrap);
	return cell;
}

function setRegisterButtonState(registerButton, registered, rejected = false) {
	registerButton.classList.toggle("registered", registered);
	registerButton.classList.toggle("rejected", rejected);
	if (rejected) {
		registerButton.textContent = "등록 불가";
		registerButton.disabled = true;
		return;
	}
	registerButton.textContent = registered ? "등록됨" : "추적 등록";
	registerButton.disabled = registered;
}

function setRejectButtonState(rejectButton, rejected) {
	rejectButton.textContent = rejected ? "재확인" : "동일 아님";
	rejectButton.disabled = false;
	rejectButton.classList.toggle("recheck", rejected);
	rejectButton.dataset.mode = rejected ? "recheck" : "reject";
}

function handleRejectButton(item, row, rejectButton, registerButton) {
	if (rejectButton.dataset.mode === "recheck") {
		recheckNotMatched(item, row, rejectButton, registerButton);
		return;
	}
	markAsNotMatched(item, row, rejectButton, registerButton);
}

async function registerTrackedProduct(item, row, registerButton) {
	if (item.rejected) {
		showMessage("동일하지 않은 상품으로 판정되어 추적 등록할 수 없습니다.");
		return;
	}
	registerButton.disabled = true;
	registerButton.textContent = "등록 중";
	try {
		const response = await fetch("/api/tracked-products", {
			method: "POST",
			headers: {"Content-Type": "application/json; charset=utf-8"},
			body: JSON.stringify({
				keyword: currentData?.keyword || keywordInput.value.trim(),
				productCode: currentData?.keyword || item.productCode,
				productName: item.productName,
				brandName: guessBrandName(item.productName),
				selectedItems: [{
					mallName: item.mallName,
					mallProductName: item.mallProductName || item.productName,
					productCode: item.productCode,
					normalizedProductName: item.productName,
					productUrl: item.productUrl,
					price: item.price,
					priceText: item.priceText,
					saleType: item.saleType,
					matchStatus: item.matchStatus,
					matchScore: item.matchScore,
					matchReasons: item.matchReasons || []
				}]
			})
		});
		const data = await response.json();
		if (!response.ok) {
			throw new Error(data.message || "추적 등록에 실패했습니다.");
		}
		const mallItemId = data.mallItemIds?.[0] || "";
		row.dataset.mallItemId = mallItemId;
		markTracked(item.productCode, mallItemId);
		saveTrackedProductToCache(currentData?.keyword || keywordInput.value.trim(), item, mallItemId);
		setRegisterButtonState(registerButton, true, false);
		showToast("추적상품으로 등록했습니다.");
		saveState();
	} catch (error) {
		showMessage(error.message);
		setRegisterButtonState(registerButton, false, false);
	}
}

function markTracked(productCode, mallItemId) {
	if (!currentData?.results || !productCode) {
		return;
	}
	currentData.results = currentData.results.map((item) => {
		if (item.productCode !== productCode) {
			return item;
		}
		return {...item, tracked: true, mallItemId};
	});
}

async function markAsNotMatched(item, row, rejectButton, registerButton) {
	if (!item.productUrl) {
		showMessage("상품 링크를 확인할 수 없어 동일 아님으로 저장할 수 없습니다.");
		return;
	}
	rejectButton.disabled = true;
	rejectButton.textContent = "저장 중";
	try {
		const response = await fetch("/api/not-matched-products", {
			method: "POST",
			headers: {"Content-Type": "application/json; charset=utf-8"},
			body: JSON.stringify(toNotMatchedPayload(item))
		});
		const data = await response.json();
		if (!response.ok) {
			throw new Error(data.message || "동일 아님 저장에 실패했습니다.");
		}
		markRejected(item);
		updateRejectedRow(row, registerButton, rejectButton);
		showToast(data.message || "동일하지 않은 상품으로 저장했습니다.");
		saveState();
	} catch (error) {
		showMessage(error.message);
		rejectButton.disabled = false;
		rejectButton.textContent = "동일 아님";
	}
}

async function recheckNotMatched(item, row, rejectButton, registerButton) {
	rejectButton.disabled = true;
	rejectButton.textContent = "변경 중";
	try {
		const response = await fetch("/api/not-matched-products/recheck", {
			method: "POST",
			headers: {"Content-Type": "application/json; charset=utf-8"},
			body: JSON.stringify(toNotMatchedPayload(item))
		});
		const data = await response.json();
		if (!response.ok) {
			throw new Error(data.message || "재확인 변경에 실패했습니다.");
		}
		markRechecked(item);
		updateRecheckedRow(row, registerButton, rejectButton);
		showToast(data.message || "재확인 대상으로 변경했습니다.");
		saveState();
	} catch (error) {
		showMessage(error.message);
		setRejectButtonState(rejectButton, true);
	}
}

function toNotMatchedPayload(item) {
	return {
		keyword: currentData?.keyword || keywordInput.value.trim(),
		mallName: item.mallName,
		productCode: item.productCode,
		productName: item.productName,
		productUrl: item.productUrl,
		price: item.price,
		priceText: item.priceText,
		reason: "사용자가 동일하지 않은 상품으로 판정했습니다."
	};
}

function markRejected(targetItem) {
	if (!currentData?.results) {
		return;
	}
	currentData.results = currentData.results.map((item) => {
		if (!isSameResult(item, targetItem)) {
			return item;
		}
		const reasons = item.matchReasons || [];
		return {
			...item,
			rejected: true,
			tracked: false,
			mallItemId: null,
			matchStatus: "NOT_MATCHED",
			matchReasons: [...reasons, "사용자가 동일하지 않은 상품으로 판정했습니다."]
		};
	});
}

function markRechecked(targetItem) {
	if (!currentData?.results) {
		return;
	}
	currentData.results = currentData.results.map((item) => {
		if (!isSameResult(item, targetItem)) {
			return item;
		}
		return {
			...item,
			rejected: false,
			matchStatus: "POSSIBLE_MATCH",
			matchReasons: ["사용자가 재확인 대상으로 변경했습니다."]
		};
	});
}

function updateRejectedRow(row, registerButton, rejectButton) {
	const status = row.querySelector("[data-column='match']");
	const reason = row.querySelector("[data-column='reason']");
	status.textContent = label(matchStatusLabels, "NOT_MATCHED");
	status.dataset.status = "NOT_MATCHED";
	if (reason) {
		reason.textContent = "사용자가 동일하지 않은 상품으로 판정했습니다.";
	}
	setRegisterButtonState(registerButton, false, true);
	setRejectButtonState(rejectButton, true);
}

function updateRecheckedRow(row, registerButton, rejectButton) {
	const status = row.querySelector("[data-column='match']");
	const reason = row.querySelector("[data-column='reason']");
	status.textContent = label(matchStatusLabels, "POSSIBLE_MATCH");
	status.dataset.status = "POSSIBLE_MATCH";
	if (reason) {
		reason.textContent = "사용자가 재확인 대상으로 변경했습니다.";
	}
	setRegisterButtonState(registerButton, false, false);
	setRejectButtonState(rejectButton, false);
}

function isSameResult(left, right) {
	if (left.productUrl && right.productUrl) {
		return left.productUrl === right.productUrl;
	}
	return left.mallName === right.mallName && left.productCode === right.productCode;
}

function filterResults(results) {
	if (currentRentalFilter === "ALL") {
		return results;
	}
	return results.filter((item) => item.rentalYn === currentRentalFilter);
}

function sortResults(results) {
	const direction = currentPriceSort === "ASC" ? 1 : -1;
	return [...results].sort((left, right) => comparePrice(left.price, right.price) * direction);
}

function comparePrice(leftPrice, rightPrice) {
	if (Number.isFinite(leftPrice) && Number.isFinite(rightPrice)) {
		return leftPrice - rightPrice;
	}
	if (Number.isFinite(leftPrice)) {
		return -1;
	}
	if (Number.isFinite(rightPrice)) {
		return 1;
	}
	return 0;
}

function resetRentalFilter() {
	currentRentalFilter = "ALL";
	rentalFilterButtons.forEach((filterButton) => {
		filterButton.classList.toggle("active", filterButton.dataset.rentalFilter === "ALL");
	});
}

function resetPriceSort() {
	currentPriceSort = "ASC";
	updatePriceSortButton();
}

function updatePriceSortButton() {
	const ascending = currentPriceSort === "ASC";
	priceSortIcon.textContent = ascending ? "↑" : "↓";
	priceSortButton.setAttribute("aria-label", ascending ? "가격 오름차순 정렬" : "가격 내림차순 정렬");
	priceSortButton.title = ascending ? "가격 오름차순" : "가격 내림차순";
}

function td(text, column) {
	const cell = document.createElement("td");
	if (column) {
		cell.dataset.column = column;
	}
	cell.textContent = text;
	return cell;
}

function label(labels, value) {
	return labels[value] || labels.UNKNOWN || "-";
}

function guessBrandName(productName) {
	if (!productName) {
		return null;
	}
	return productName.split(/\s+/)[0] || null;
}

function saveState() {
	if (!currentData) {
		return;
	}
	localStorage.setItem(storageKey, JSON.stringify({
		keyword: keywordInput.value,
		currentData,
		currentRentalFilter,
		currentPriceSort,
		currentPage
	}));
}

function initializeState() {
	const rawState = localStorage.getItem(storageKey);
	if (!rawState) {
		return;
	}
	try {
		const state = JSON.parse(rawState);
		currentRentalFilter = state.currentRentalFilter || "ALL";
		currentPriceSort = state.currentPriceSort || "ASC";
		currentPage = 1;
		keywordInput.value = "";
		rentalFilterButtons.forEach((filterButton) => {
			filterButton.classList.toggle("active", filterButton.dataset.rentalFilter === currentRentalFilter);
		});
		updatePriceSortButton();
	} catch (error) {
		localStorage.removeItem(storageKey);
	}
}

function saveSearchHistory(data) {
	if (!data?.keyword) {
		return;
	}
	const history = readJson(searchHistoryKey, []);
	const nextHistory = [
		{
			keyword: data.keyword,
			searchedAt: data.searchedAt || new Date().toISOString(),
			resultCount: data.resultCount || 0,
			sourceTotalCount: data.sourceTotalCount || null,
			results: data.results || []
		},
		...history.filter((entry) => entry.keyword !== data.keyword)
	].slice(0, 20);
	localStorage.setItem(searchHistoryKey, JSON.stringify(nextHistory));
}

function saveTrackedProductToCache(keyword, item, mallItemId) {
	if (!keyword || !item) {
		return;
	}
	const groups = readJson(trackedProductsKey, []);
	let group = groups.find((candidate) => candidate.keyword === keyword);
	if (!group) {
		group = {keyword, items: []};
		groups.unshift(group);
	}
	const cachedItem = {
		productId: null,
		mallItemId,
		productCode: item.productCode,
		productName: item.productName,
		price: item.price,
		priceText: item.priceText,
		productUrl: item.productUrl
	};
	const itemIndex = group.items.findIndex((candidate) => candidate.productCode === item.productCode
		|| candidate.productUrl === item.productUrl);
	if (itemIndex >= 0) {
		group.items[itemIndex] = cachedItem;
	} else {
		group.items.unshift(cachedItem);
	}
	localStorage.setItem(trackedProductsKey, JSON.stringify(groups));
}

function readJson(key, fallback) {
	try {
		const rawValue = localStorage.getItem(key);
		return rawValue ? JSON.parse(rawValue) : fallback;
	} catch (error) {
		return fallback;
	}
}
