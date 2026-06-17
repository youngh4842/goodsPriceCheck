# 상품 기반 가격 조회 MVP

Spring Boot 기반 상품 가격 조회 MVP입니다. 사용자가 상품명 또는 상품코드를 입력하면 홈쇼핑모아 공개 검색 결과에서 가격 정보를 수집하고, 검색 결과별 상품 일치 여부를 규칙 기반으로 검증합니다. 사용자가 확인한 쇼핑몰 상품은 추적 상품으로 등록할 수 있습니다.

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

- 화면: `http://localhost:8080`
- H2 콘솔: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:goods_price`
- User Name: `sa`
- Password: 빈 값

## 주요 API

### 가격 조회

```http
GET /api/products/price-search?keyword=DF18CB8600ER
```

검색 결과에는 기존 가격 정보와 함께 다음 검증 필드가 포함됩니다.

```json
{
  "mallName": "A 쇼핑몰",
  "productCode": "2080262380",
  "mallProductName": "삼성 비스포크 에어드레서 DF18CB8600ER",
  "productName": "삼성 비스포크 에어드레서",
  "rentalYn": "X",
  "productPeriod": null,
  "price": 1234000,
  "priceText": "1,234,000원",
  "productUrl": "https://example.com/product/123",
  "saleType": "PURCHASE",
  "matchStatus": "MATCHED",
  "matchScore": 100,
  "matchReasons": ["모델코드가 정확히 일치합니다."]
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
  "productName": "삼성 비스포크 에어드레서",
  "brandName": "삼성",
  "selectedItems": [
    {
      "mallName": "A 쇼핑몰",
      "mallProductName": "삼성 비스포크 에어드레서 DF18CB8600ER",
      "normalizedProductName": "삼성 비스포크 에어드레서",
      "productUrl": "https://example.com/product/123",
      "saleType": "PURCHASE",
      "matchStatus": "MATCHED",
      "matchScore": 100,
      "matchReasons": ["모델코드가 정확히 일치합니다."]
    }
  ]
}
```

### 검증 상태 수동 변경

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

기존 검색 로그 테이블은 유지됩니다. `search_id`, `result_id`, `product_id`, `mall_item_id`는 `yyMMdd + 4자리 일련번호` 형식으로 생성됩니다.

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
| normalized_product_name | 정규화 상품명 |
| product_url | 상품 링크 |
| sale_type | 판매유형 |
| match_status | 검증 상태 |
| match_score | 일치 점수 |
| match_reason | 검증 사유 |
| manually_confirmed_yn | 사용자 수동 확정 여부 |
| active_yn | 가격 추적 활성 여부 |
| last_crawled_at | 마지막 수집 시각 |
| created_at | 등록 시각 |
| updated_at | 수정 시각 |

## 검증 규칙

- 모델코드는 대문자 변환 후 공백, 하이픈, 언더스코어, 특수문자를 제거해 비교합니다.
- 검색어가 모델코드이고 상품명에 동일 모델코드가 있으면 `MATCHED`로 판단합니다.
- 필터, 부품, 액세서리, 케이스, 커버, 거치대, 호환, 리필, 전용, 교체용 키워드는 `NOT_MATCHED` 검토 대상으로 분리합니다.
- 렌탈 단어 또는 기간 정보가 있으면 판매유형을 `RENTAL`로 표시합니다.
- 중고 단어가 있으면 판매유형을 `USED`로 표시합니다.
- 정보가 부족하면 자동 제외하지 않고 `UNKNOWN` 또는 `POSSIBLE_MATCH`로 표시합니다.

## 테스트

```powershell
.\gradlew.bat test
```

주요 테스트는 상품명 정규화, 기간 추출, 모델코드 정규화, 자동 검증 상태 판정을 확인합니다.
