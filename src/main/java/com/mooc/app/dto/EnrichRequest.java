package com.mooc.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record EnrichRequest(
        @JsonProperty("name_zh") String nameZh,
        @JsonProperty("ticket_price") String ticketPrice,
        @JsonProperty("opening_hours") String openingHours,
        String address,
        @DecimalMin("0") @DecimalMax("5") BigDecimal rating,
        String description,
        @JsonProperty("description_zh") String descriptionZh
) {}
