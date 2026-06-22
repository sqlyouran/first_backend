package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ExchangeRateResponse extends BaseResponse {

    private final String base;
    private final Map<String, String> rates;

    @JsonProperty("updated_at")
    private final String updatedAt;

    public ExchangeRateResponse(String requestId, String base, Map<String, String> rates, String updatedAt) {
        super(requestId);
        this.base = base;
        this.rates = rates;
        this.updatedAt = updatedAt;
    }

    public String getBase() { return base; }
    public Map<String, String> getRates() { return rates; }
    public String getUpdatedAt() { return updatedAt; }
}
