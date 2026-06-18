package com.example.mockmall;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void listProductsReturnsInitialProducts() throws Exception {
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()", greaterThanOrEqualTo(5)))
				.andExpect(jsonPath("$[0].productCode").exists())
				.andExpect(jsonPath("$[0].productName").exists())
				.andExpect(jsonPath("$[0].price").exists())
				.andExpect(jsonPath("$[0].updatedAt").exists());
	}

	@Test
	void getProductReturnsProduct() throws Exception {
		mockMvc.perform(get("/api/products/DF18CB8600ER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productCode", is("DF18CB8600ER")))
				.andExpect(jsonPath("$.productName", is("Samsung Bespoke AirDresser DF18CB8600ER")));
	}

	@Test
	void changePriceUpdatesProductPrice() throws Exception {
		mockMvc.perform(put("/api/products/DF18CB8600ER/price")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "price": 35000
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productCode", is("DF18CB8600ER")))
				.andExpect(jsonPath("$.price", is(35000)));
	}

	@Test
	void changePriceRejectsNegativePrice() throws Exception {
		mockMvc.perform(put("/api/products/DF18CB8600ER/price")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "price": -1
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("price must be greater than or equal to 0")));
	}

	@Test
	void getProductReturnsNotFoundForUnknownCode() throws Exception {
		mockMvc.perform(get("/api/products/UNKNOWN"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("product not found: UNKNOWN")));
	}
}
