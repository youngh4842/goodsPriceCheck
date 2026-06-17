const trackedSummary = document.querySelector("#trackedSummary");
const trackedMessage = document.querySelector("#trackedMessage");
const trackedGroups = document.querySelector("#trackedGroups");
const trackedProductsKey = "goodsPrice.trackedProducts";

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
				<col class="link-col">
			</colgroup>
			<thead>
				<tr>
					<th>상품코드</th>
					<th>상품명</th>
					<th>가격</th>
					<th>링크</th>
				</tr>
			</thead>
		`;
		const tbody = document.createElement("tbody");
		for (const item of group.items) {
			const tr = document.createElement("tr");
			tr.appendChild(td(item.productCode || "-", "product-code"));
			tr.appendChild(td(item.productName || "-", "product-name"));
			tr.appendChild(td(item.priceText || formatPrice(item.price), "price"));
			tr.appendChild(linkCell(item.productUrl));
			tbody.appendChild(tr);
		}
		table.appendChild(tbody);
		tableWrap.appendChild(table);
		section.appendChild(tableWrap);
		trackedGroups.appendChild(section);
	}
}

function showTrackedMessage(text) {
	trackedMessage.textContent = text;
	trackedMessage.hidden = false;
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
