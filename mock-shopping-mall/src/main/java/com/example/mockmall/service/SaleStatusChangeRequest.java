package com.example.mockmall.service;

import com.example.mockmall.domain.SaleStatus;
import jakarta.validation.constraints.NotNull;

public record SaleStatusChangeRequest(
		@NotNull SaleStatus saleStatus
) {
}
