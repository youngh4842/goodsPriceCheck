package com.example.mockmall.web;

import jakarta.validation.constraints.NotNull;

public record PriceChangeRequest(@NotNull Long price) {
}
