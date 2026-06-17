# 상품 기반 가격 조회 MVP

Spring Boot 기반 상품 가격 조회 MVP입니다. 사용자가 상품명 또는 상품코드를 입력하면 홈쇼핑모아 공개 검색 결과에서 가격 정보를 수집하고, 검색 결과별 상품 일치 여부를 규칙 기반으로 검증합니다.

2단계에서는 유사상품 검증, 추적 상품 등록, 추적 상품 리스트, 수동 `동일 아님`/`재확인` 처리, 브라우저 로컬 저장소 기반 화면 보조 저장 기능을 추가했습니다.

## 기술 스택

- Java 17
- Spring Boot 3.5
- Gradle Wrapper
- Thymeleaf
- Spring Data JPA
- H2 Database
- Playwright Java

## 실행 방법

```powershell
.\gradlew.bat playwrightInstall
.\gradlew.bat bootRun
```

- 상품 가격 조회 화면: `http://localhost:8080`
- 추적 상품 리스트 화면: `http://localhost:8080/tracked-products`
- H2 콘솔: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:goods_price`
- User Name: `sa`
- Password: 빈 값

## 주요 화면

### 상품 가격 조회

- 상품명 또는 상품코드로 홈쇼핑모아 검색 결과를 수집합니다.
- 쇼핑몰명, 쇼핑몰 상품코드, 상품명, 판매유형, 렌탈 여부, 기간, 가격, 검증상태, 점수, 검증 사유, 링크, 작업 버튼을 표시합니다.
- 결과는 가격 기준으로 정렬되며, 가격 컬럼에서 오름차순/내림차순을 변경할 수 있습니다.
- 렌탈 필터는 `전체 상품`, `렌탈 가능 상품`, `구매 가능 상품`을 제공합니다.
- `추적 등록` 버튼으로 상품을 추적 상품으로 저장할 수 있습니다.
- `동일 아님` 버튼으로 검색 상품과 동일하지 않은 결과를 수동 저장할 수 있습니다.
- `동일 아님`으로 저장된 row는 `추적 등록`이 `등록 불가`로 바뀌고, 버튼은 초록색 `재확인`으로 바뀝니다.
- `재확인` 버튼을 누르면 검증상태가 `확인 필요`로 변경되고 추적 등록이 다시 가능해집니다.
- `CLEAR` 버튼은 조회 결과와 검색어 입력란을 비웁니다.

### 추적 상품 리스트

- `/tracked-products`에서 추적 등록된 상품을 확인합니다.
- 검색어 기준으로 묶어서 상품코드, 상품명, 가격, 링크를 표시합니다.
- 서버 DB 데이터가 있으면 DB 데이터를 우선 표시합니다.
- 서버 DB 데이터가 없거나 조회에 실패했지만 브라우저 저장 캐시가 있으면 `localStorage`의 추적 상품 캐시를 표시합니다.
- 브라우저 저장 캐시로 표시되는 경우 요약에 `(브라우저 저장)` 문구가 붙습니다.

## 주요 API

### 가격 조회

```http
GET /api/products/price-search?keyword=DF18CB8600ER
```

검색 결과에는 가격 정보와 함께 검증 필드가 포함됩니다.

```json
{
  "keyword": "DF18CB8600ER",
  "searchedAt": "2026-06-17T10:00:00",
  "found": true,
  "resultCount": 1,
  "sourceTotalCount": 1,
  "results": [
    {
      "mallName": "A 쇼핑몰",
      "productCode": "2080262380",
      "mallProductName": "삼성 비스포크 에어드레서 DF18CB8600ER",
      "productName": "삼성 비스포크 에어드레서 DF18CB8600ER",
      "rentalYn": "X",
      "productPeriod": null,
      "price": 1234000,
      "priceText": "1,234,000원",
      "productUrl": "https://example.com/product/123",
      "saleType": "PURCHASE",
      "matchStatus": "MATCHED",
      "matchScore": 100,
      "matchReasons": ["모델코드가 정확히 일치합니다."],
      "tracked": false,
      "mallItemId": null,
      "rejected": false
    }
  ]
}
```

### 추적 상품 등록

```http
POST /api/tracked-products
Content-Type: application/json
```

```json
{
  "keyword": "DF18CB8600ER",
  "productCode": "DF18CB8600ER",
  "productName": "삼성 비스포크 에어드레서 DF18CB8600ER",
  "brandName": "삼성",
  "selectedItems": [
    {
      "mallName": "A 쇼핑몰",
      "mallProductName": "삼성 비스포크 에어드레서 DF18CB8600ER",
      "productCode": "2080262380",
      "normalizedProductName": "삼성 비스포크 에어드레서 DF18CB8600ER",
      "productUrl": "https://example.com/product/123",
      "price": 1234000,
      "priceText": "1,234,000원",
      "saleType": "PURCHASE",
      "matchStatus": "MATCHED",
      "matchScore": 100,
      "matchReasons": ["모델코드가 정확히 일치합니다."]
    }
  ]
}
```

### 추적 상품 리스트 조회

```http
GET /api/tracked-products
```

```json
[
  {
    "keyword": "DF18CB8600ER",
    "items": [
      {
        "productId": 2606170001,
        "mallItemId": 2606170001,
        "productCode": "2080262380",
        "productName": "삼성 비스포크 에어드레서 DF18CB8600ER",
        "price": 1234000,
        "priceText": "1,234,000원",
        "productUrl": "https://example.com/product/123"
      }
    ]
  }
]
```

### 동일 아님 저장

```http
POST /api/not-matched-products
Content-Type: application/json
```

```json
{
  "keyword": "DF18CB8600ER",
  "mallName": "A 쇼핑몰",
  "productCode": "2080262380",
  "productName": "다른 상품명",
  "productUrl": "https://example.com/product/456",
  "price": 99000,
  "priceText": "99,000원",
  "reason": "사용자가 동일하지 않은 상품으로 판정했습니다."
}
```

저장 후 같은 검색어로 재조회하면 해당 상품은 다음 상태로 표시됩니다.

- `matchStatus`: `NOT_MATCHED`
- `rejected`: `true`
- `tracked`: `false`
- 작업 컬럼: `추적 등록` 대신 `등록 불가`, `동일 아님` 대신 `재확인`

### 재확인 처리

```http
POST /api/not-matched-products/recheck
Content-Type: application/json
```

```json
{
  "keyword": "DF18CB8600ER",
  "mallName": "A 쇼핑몰",
  "productCode": "2080262380",
  "productName": "다른 상품명",
  "productUrl": "https://example.com/product/456",
  "price": 99000,
  "priceText": "99,000원"
}
```

재확인 후 같은 검색어로 재조회하면 해당 상품은 다음 상태로 표시됩니다.

- `matchStatus`: `POSSIBLE_MATCH`
- `rejected`: `false`
- `tracked`: `false`
- 작업 컬럼: 추적 등록 가능, `재확인` 대신 `동일 아님`

### 검증 상태 수동 변경

추적 등록된 쇼핑몰 상품에 대해서는 기존 수동 상태 변경 API도 유지됩니다.

```http
PATCH /api/mall-items/{mallItemId}/match-status
Content-Type: application/json
```

```json
{
  "matchStatus": "NOT_MATCHED",
  "manualConfirmed": true
}
```

## DB 테이블

`search_id`, `result_id`, `product_id`, `mall_item_id`, `not_matched_id`는 `yyMMdd + 4자리 일련번호` 형식으로 생성됩니다.

### TB_SEARCH_LOG

| 컬럼 | 설명 |
| --- | --- |
| search_id | 검색 로그 ID |
| keyword | 검색어 |
| result_count | 수집 결과 수 |
| success_yn | 성공 여부 |
| error_message | 실패 메시지 |
| searched_at | 검색 시각 |

### TB_PRODUCT_PRICE_RESULT

| 컬럼 | 설명 |
| --- | --- |
| result_id | 검색 결과 ID |
| search_id | 검색 로그 ID |
| mall_name | 쇼핑몰명 |
| product_code | 쇼핑몰 상품코드 |
| product_period | 기간 |
| product_name | 정리된 상품명 |
| price | 숫자 가격 |
| price_text | 가격 문자열 |
| product_url | 상품 링크 |
| sale_type | PURCHASE, RENTAL, USED, UNKNOWN |
| match_status | MATCHED, POSSIBLE_MATCH, NOT_MATCHED, UNKNOWN |
| match_score | 일치 점수 |
| match_reason | 검증 사유 |
| crawled_at | 수집 시각 |

### PRODUCT_MASTER

| 컬럼 | 설명 |
| --- | --- |
| product_id | 추적 상품 ID |
| product_code | 정규화된 모델코드 |
| search_keyword | 최초 등록 검색어 |
| product_name | 사용자가 확정한 기준 상품명 |
| brand_name | 브랜드명 |
| active_yn | 추적 활성 여부 |
| created_at | 등록 시각 |
| updated_at | 수정 시각 |

### PRODUCT_MALL_ITEM

| 컬럼 | 설명 |
| --- | --- |
| mall_item_id | 쇼핑몰 상품 ID |
| product_id | 추적 상품 ID |
| mall_name | 쇼핑몰명 |
| mall_product_name | 쇼핑몰 원본 상품명 |
| mall_product_code | 쇼핑몰 상품코드 |
| normalized_product_name | 정규화 상품명 |
| product_url | 상품 링크 |
| price | 숫자 가격 |
| price_text | 가격 문자열 |
| sale_type | 판매유형 |
| match_status | 검증 상태 |
| match_score | 일치 점수 |
| match_reason | 검증 사유 |
| manually_confirmed_yn | 사용자 수동 확정 여부 |
| active_yn | 가격 추적 활성 여부 |
| last_crawled_at | 마지막 수집 시각 |
| created_at | 등록 시각 |
| updated_at | 수정 시각 |

### PRODUCT_NOT_MATCHED_ITEM

| 컬럼 | 설명 |
| --- | --- |
| not_matched_id | 동일 아님/재확인 수동 판정 ID |
| keyword | 검색어 |
| mall_name | 쇼핑몰명 |
| mall_product_code | 쇼핑몰 상품코드 |
| product_name | 상품명 |
| product_url | 상품 링크 |
| price | 숫자 가격 |
| price_text | 가격 문자열 |
| reason | 수동 판정 사유 |
| match_status | NOT_MATCHED 또는 POSSIBLE_MATCH |
| created_at | 등록 시각 |
| updated_at | 수정 시각 |

## 검증 규칙

- 모델코드는 대문자 변환 후 공백, 하이픈, 언더스코어, 특수문자를 제거해 비교합니다.
- 검색어가 모델코드이고 상품명에 동일 모델코드가 있으면 `MATCHED`로 판단합니다.
- 필터, 부품, 액세서리, 케이스, 커버, 거치대, 호환, 리필, 전용, 교체용 키워드는 `NOT_MATCHED` 검토 대상으로 분리합니다.
- 렌탈 단어 또는 기간 정보가 있으면 판매유형을 `RENTAL`로 표시합니다.
- 중고 단어가 있으면 판매유형을 `USED`로 표시합니다.
- 정보가 부족하면 자동 제외하지 않고 `UNKNOWN` 또는 `POSSIBLE_MATCH`로 표시합니다.
- 사용자가 `동일 아님`으로 저장한 결과는 자동 검증보다 우선해 `NOT_MATCHED`로 표시합니다.
- 사용자가 `재확인`으로 변경한 결과는 자동 검증보다 우선해 `POSSIBLE_MATCH`로 표시합니다.

## 브라우저 저장소 정책

로컬 개발 편의를 위해 일부 화면 데이터는 브라우저 `localStorage`에 저장됩니다.

| 키 | 용도 |
| --- | --- |
| goodsPrice.lastSearch | 마지막 상품 가격 조회 상태. 현재는 화면 자동 복원에는 사용하지 않고 검색어/필터 상태 참고용으로만 사용합니다. |
| goodsPrice.searchHistory | 지금까지 상품 가격 조회로 수집한 결과 이력. 최근 20개 검색어 기준으로 저장합니다. |
| goodsPrice.trackedProducts | 추적 등록한 상품 리스트 캐시. 추적상품 리스트 화면에서 DB가 비어 있거나 조회 실패 시 보조 표시용으로 사용합니다. |

상품가격조회 화면은 서버 재시작 또는 페이지 새로고침 후에도 조회 결과를 자동으로 다시 그리지 않습니다. 즉, 화면은 비어 보이지만 조회 이력은 `goodsPrice.searchHistory`에 남습니다.

추적상품 리스트 화면은 서버 DB 결과를 우선 표시하고, DB 결과가 없을 때 브라우저 저장 캐시가 있으면 캐시 데이터를 표시합니다.

## 테스트

```powershell
.\gradlew.bat test
```

주요 테스트는 상품명 정규화, 기간 추출, 모델코드 정규화, 자동 검증 상태 판정을 확인합니다.
