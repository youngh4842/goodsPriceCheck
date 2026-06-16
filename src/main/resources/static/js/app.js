const form = document.querySelector("#searchForm");
const keywordInput = document.querySelector("#keyword");
const button = document.querySelector("#searchButton");
const summary = document.querySelector("#summary");
const message = document.querySelector("#message");
const resultsBody = document.querySelector("#results");
const resultTable = document.querySelector("#resultTable");
const toggleProductCodeButton = document.querySelector("#toggleProductCode");
const rentalFilterButtons = document.querySelectorAll("[data-rental-filter]");
const priceSortButton = document.querySelector("#priceSortButton");
const priceSortIcon = document.querySelector("#priceSortIcon");
let currentData = null;
let currentRentalFilter = "ALL";
let currentPriceSort = "ASC";

toggleProductCodeButton.addEventListener("click", () => {
	const hidden = resultTable.classList.toggle("hide-product-code");
	toggleProductCodeButton.textContent = hidden ? "상품코드 보이기" : "상품코드 숨기기";
	toggleProductCodeButton.setAttribute("aria-pressed", String(hidden));
});

rentalFilterButtons.forEach((filterButton) => {
	filterButton.addEventListener("click", () => {
		currentRentalFilter = filterButton.dataset.rentalFilter;
		rentalFilterButtons.forEach((button) => button.classList.toggle("active", button === filterButton));
		renderResults(currentData);
	});
});

priceSortButton.addEventListener("click", () => {
	currentPriceSort = currentPriceSort === "ASC" ? "DESC" : "ASC";
	updatePriceSortButton();
	renderResults(currentData);
});

form.addEventListener("submit", async (event) => {
	event.preventDefault();
	const keyword = keywordInput.value.trim();
	clearState();
	resetRentalFilter();
	resetPriceSort();

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
	} catch (error) {
		showMessage("서버와 통신하는 중 오류가 발생했습니다.");
		summary.textContent = "조회 실패";
	} finally {
		button.disabled = false;
		button.textContent = "검색";
	}
});

function clearState() {
	message.hidden = true;
	message.textContent = "";
	resultsBody.replaceChildren();
	currentData = null;
}

function showMessage(text) {
	message.textContent = text;
	message.hidden = false;
}

function renderResults(data) {
	resultsBody.replaceChildren();
	if (!data) {
		return;
	}
	const filteredResults = sortResults(filterResults(data.results || []));
	const sourceText = Number.isInteger(data.sourceTotalCount)
		? ` / 홈쇼핑모아 전체 ${data.sourceTotalCount.toLocaleString()}건`
		: "";
	const filterText = currentRentalFilter === "ALL" ? "" : ` / 필터 ${filteredResults.length}건`;
	summary.textContent = `${data.keyword} 수집 결과 ${data.resultCount}건${sourceText}${filterText}`;
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
	for (const item of filteredResults) {
		const tr = document.createElement("tr");
		tr.appendChild(td(item.mallName || "-", "mall"));
		tr.appendChild(td(item.productCode || "-", "product-code"));
		tr.appendChild(td(item.productName || "-", "product-name"));
		tr.appendChild(td(item.rentalYn || "X", "rental"));
		tr.appendChild(td(item.productPeriod || "-", "period"));
		tr.appendChild(td(item.priceText || "가격정보 없음", "price"));
		const linkTd = document.createElement("td");
		linkTd.dataset.column = "link";
		const link = document.createElement("a");
		link.href = item.productUrl;
		link.target = "_blank";
		link.rel = "noopener noreferrer";
		link.textContent = "바로가기";
		linkTd.appendChild(link);
		tr.appendChild(linkTd);
		resultsBody.appendChild(tr);
	}
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
	rentalFilterButtons.forEach((button) => {
		button.classList.toggle("active", button.dataset.rentalFilter === "ALL");
	});
}

function resetPriceSort() {
	currentPriceSort = "ASC";
	updatePriceSortButton();
}

function updatePriceSortButton() {
	const ascending = currentPriceSort === "ASC";
	priceSortIcon.textContent = ascending ? "▲" : "▼";
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
