package com.example.goodsprice.product;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductSearchPageController {

	@GetMapping("/")
	public String index() {
		return "index";
	}
}
