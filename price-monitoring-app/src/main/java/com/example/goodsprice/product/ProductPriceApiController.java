package com.example.goodsprice.product;

import com.example.goodsprice.crawler.CrawlerSourceResponse;
import com.example.goodsprice.crawler.CrawlerSourceService;
import com.example.goodsprice.crawler.CrawlerSourceUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductPriceApiController {

	private final ProductPriceSearchService searchService;
	private final CrawlerSourceService crawlerSourceService;

	public ProductPriceApiController(ProductPriceSearchService searchService, CrawlerSourceService crawlerSourceService) {
		this.searchService = searchService;
		this.crawlerSourceService = crawlerSourceService;
	}

	@GetMapping("/price-search")
	public ResponseEntity<ProductPriceSearchResponse> search(@RequestParam String keyword) {
		return ResponseEntity.ok(searchService.search(keyword));
	}

	@GetMapping("/crawler-source")
	public CrawlerSourceResponse crawlerSource() {
		return CrawlerSourceResponse.from(crawlerSourceService.currentSource());
	}

	@PutMapping("/crawler-source")
	public ResponseEntity<CrawlerSourceResponse> changeCrawlerSource(
			@Valid @RequestBody CrawlerSourceUpdateRequest request) {
		return ResponseEntity.status(HttpStatus.OK)
				.body(CrawlerSourceResponse.from(crawlerSourceService.changeSource(request.source())));
	}
}
