package com.mooc.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WeatherResponse extends BaseResponse {

    private final String city;
    private final double temperature;
    private final String description;
    private final String icon;
    private final int humidity;

    @JsonProperty("wind_speed")
    private final double windSpeed;

    @JsonProperty("updated_at")
    private final String updatedAt;

    public WeatherResponse(String requestId, String city, double temperature,
                           String description, String icon, int humidity,
                           double windSpeed, String updatedAt) {
        super(requestId);
        this.city = city;
        this.temperature = temperature;
        this.description = description;
        this.icon = icon;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.updatedAt = updatedAt;
    }

    public String getCity() { return city; }
    public double getTemperature() { return temperature; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public int getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }
    public String getUpdatedAt() { return updatedAt; }
}
