package com.mooc.app.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ExchangeRateData(
        String base,
        Map<String, BigDecimal> rates,
        Instant updatedAt
) {}
