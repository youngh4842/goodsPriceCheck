package com.example.mockmall.web;

import com.example.mockmall.domain.MockProduct;
import com.example.mockmall.domain.SaleStatus;
import com.example.mockmall.domain.SaleType;
import com.example.mockmall.service.MockProductRequest;
import com.example.mockmall.service.MockProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class MockMallPageController {

	private final MockProductService productService;

	public MockMallPageController(MockProductService productService) {
		this.productService = productService;
	}

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("products", productService.list(null, false));
		return "home";
	}

	@GetMapping("/search")
	public String search(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("keyword", keyword);
		model.addAttribute("products", productService.list(keyword, false));
		return "search";
	}

	@GetMapping("/products/{productId}")
	public String detail(@PathVariable Long productId, Model model) {
		model.addAttribute("product", productService.get(productId));
		return "detail";
	}

	@GetMapping("/admin/products")
	public String admin(@RequestParam(required = false) String keyword, Model model) {
		List<MockProduct> products = productService.list(keyword, true);
		model.addAttribute("keyword", keyword);
		model.addAttribute("products", products);
		model.addAttribute("saleTypes", SaleType.values());
		model.addAttribute("saleStatuses", SaleStatus.values());
		model.addAttribute("newProduct", new MockProductRequest("", "", "", "Mock Mall A", 1000L, SaleType.PURCHASE,
				SaleStatus.ACTIVE, "", "", "Y"));
		return "admin-products";
	}

	@PostMapping("/admin/products")
	public String create(@ModelAttribute MockProductRequest request) {
		productService.create(request);
		return "redirect:/admin/products";
	}

	@PostMapping("/admin/products/{productId}")
	public String update(@PathVariable Long productId, @ModelAttribute MockProductRequest request) {
		productService.update(productId, request);
		return "redirect:/admin/products";
	}

	@PostMapping("/admin/products/{productId}/price")
	public String changePrice(@PathVariable Long productId, @RequestParam Long price,
			@RequestParam(required = false) String changeReason) {
		productService.changePrice(productId, price, changeReason);
		return "redirect:/admin/products";
	}

	@PostMapping("/admin/products/{productId}/quick-price")
	public String quickPrice(@PathVariable Long productId, @RequestParam String mode) {
		productService.adjustPrice(productId, mode);
		return "redirect:/admin/products";
	}

	@PostMapping("/admin/products/{productId}/sale-status")
	public String changeSaleStatus(@PathVariable Long productId, @RequestParam SaleStatus saleStatus) {
		productService.changeSaleStatus(productId, saleStatus);
		return "redirect:/admin/products";
	}

	@PostMapping("/admin/products/{productId}/price-visible")
	public String changePriceVisible(@PathVariable Long productId, @RequestParam String priceVisibleYn) {
		productService.changePriceVisible(productId, priceVisibleYn);
		return "redirect:/admin/products";
	}

	@PostMapping("/admin/products/{productId}/hide")
	public String hide(@PathVariable Long productId) {
		productService.hide(productId);
		return "redirect:/admin/products";
	}
}
