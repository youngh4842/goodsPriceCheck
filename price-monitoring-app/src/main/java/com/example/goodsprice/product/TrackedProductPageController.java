package com.example.goodsprice.product;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TrackedProductPageController {

	@GetMapping("/tracked-products")
	public String trackedProducts() {
		return "tracked-products";
	}
}
