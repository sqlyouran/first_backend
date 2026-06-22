package com.mooc.app.service;

import java.time.Instant;

public record WeatherData(
        String city,
        double temperature,
        String description,
        String icon,
        int humidity,
        double windSpeed,
        Instant updatedAt
) {}
