# 상품 기반 가격 조회 MVP

Spring Boot 기반 단건 상품 가격 조회 MVP입니다. 사용자가 상품명 또는 상품코드를 입력하면 홈쇼핑모아 공개 검색 결과에서 쇼핑몰명, 상품명, 가격, 링크를 수집해 화면과 API로 제공합니다.

## 기술 스택

- Java 17
- Spring Boot 3.5
- Gradle Wrapper
- Thymeleaf
- Spring Data JPA
- H2 Database
- Playwright Java(브라우저 자동화 fallback)

## 실행 방법

1. Chromium 브라우저를 설치합니다.

```powershell
.\gradlew.bat playwrightInstall
```

2. 애플리케이션을 실행합니다.

```powershell
.\gradlew.bat bootRun
```

3. 브라우저에서 화면을 엽니다.

```text
http://localhost:8080
```

H2 콘솔은 `http://localhost:8080/h2-console`에서 확인할 수 있습니다.

- JDBC URL: `jdbc:h2:mem:goods_price`
- User Name: `sa`
- Password: 빈 값

## API

```http
GET /api/products/price-search?keyword=DF18CB8600ER
```

응답 예시:

```json
{
  "keyword": "DF18CB8600ER",
  "searchedAt": "2026-06-12T15:30:00",
  "found": true,
  "resultCount": 2,
  "sourceTotalCount": 146851,
  "results": [
    {
      "mallName": "CJ온스타일",
      "productCode": "2080262380",
      "productName": "Bespoke AI 에어드레서 [새틴 베이지]",
      "rentalYn": "X",
      "productPeriod": null,
      "price": 1059080,
      "priceText": "1,059,080원",
      "productUrl": "https://display.cjonstyle.com/p/item/2080262380"
    }
  ],
  "message": null
}
```

검색어가 비어 있으면 `400 Bad Request`와 함께 `검색어를 입력해 주세요.` 메시지를 반환합니다.

## DB 테이블

`search_id`, `result_id`는 `yyMMdd + 4자리 일련번호` 형식으로 저장합니다.
예를 들어 2026년 6월 15일 첫 번째 데이터는 `2606150001`입니다.

### TB_SEARCH_LOG

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| search_id | BIGINT, PK | 날짜 조합 일련번호 |
| keyword | VARCHAR | 검색어 |
| result_count | INT | MVP 수집 결과 수 |
| success_yn | CHAR | 성공 여부 |
| error_message | VARCHAR | 실패 메시지 |
| searched_at | DATETIME | 검색 시각 |

### TB_PRODUCT_PRICE_RESULT

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| result_id | BIGINT, PK | 날짜 조합 일련번호 |
| search_id | BIGINT, FK | 검색 로그 ID |
| mall_name | VARCHAR | 쇼핑몰명 |
| product_code | VARCHAR | 상품코드 |
| product_period | VARCHAR | 상품명에서 추출한 기간. 예: `60개월`, `5년` |
| product_name | VARCHAR | 상품명 |
| price | BIGINT | 숫자 가격 |
| price_text | VARCHAR | 원본 가격 문구 |
| product_url | VARCHAR | 상품 링크 |
| crawled_at | DATETIME | 수집 시각 |

## 주요 흐름

1. `ProductPriceApiController`가 `/api/products/price-search` 요청을 받습니다.
2. `ProductPriceSearchService`가 검색어를 검증하고 검색 로그를 생성합니다.
3. `HsmoaPriceCrawler`가 홈쇼핑모아 EP 검색 API를 조회합니다.
4. EP API가 실패하면 서버 렌더링 HTML 조회, Playwright 브라우저 검색 순서로 fallback합니다.
5. `crawler.max-results`가 0이면 제한 없이 수집하고, 양수이면 해당 건수까지만 저장/표시합니다.
6. 성공 시 `TB_SEARCH_LOG`, `TB_PRODUCT_PRICE_RESULT`에 로그와 결과를 저장합니다.
7. 실패 시 사용자 메시지를 반환하고 실패 사유를 `TB_SEARCH_LOG.error_message`에 저장합니다.

## 주요 클래스

- `HsmoaSelectors`: 홈쇼핑모아 selector 후보를 한 곳에서 관리합니다.
- `HsmoaPriceCrawler`: 홈쇼핑모아 접속, 검색 페이지 로딩, 결과 추출을 담당합니다.
- `PriceParser`: 가격 문자열과 숫자 가격 변환을 담당합니다.
- `SearchLog`, `ProductPriceResult`: 요구사항의 H2 테이블에 매핑되는 JPA 엔티티입니다.
- `DailyIdGenerator`: `yyMMdd + 4자리 일련번호` 형식으로 `search_id`, `result_id`를 채번합니다.
- `ProductPeriodParser`: 상품명에서 `60개월`, `5년` 같은 기간 표현을 추출합니다.
- `ProductPriceSearchService`: 크롤링 결과 저장과 API 응답 생성을 담당합니다.

## 예외 처리

- 검색어 미입력: API 요청 단계에서 검증합니다.
- 검색 결과 없음: 빈 배열과 `검색 결과가 없습니다.` 메시지를 반환하고 성공 로그로 저장합니다.
- 홈쇼핑모아 접속 실패, 검색창 탐색 실패, 결과 영역 탐색 실패, 타임아웃: 실패 메시지를 반환하고 실패 로그로 저장합니다.
- 가격 파싱 실패: 가격을 `null`, 가격 문구를 `가격정보 없음`으로 처리합니다.
- 상품 링크 추출 실패: 링크가 없는 상품은 결과에서 제외합니다.

## 운영 주의

초기 MVP는 공개 검색 결과만 단건 조회합니다. 로그인, 개인정보, 비공개 데이터 수집은 포함하지 않습니다.
`crawler.max-results=0`이면 수집 건수 제한을 두지 않으며, 필요한 경우 `crawler.max-results=50`처럼 상한을 설정할 수 있습니다.
