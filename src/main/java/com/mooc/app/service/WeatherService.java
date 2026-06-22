package com.mooc.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final String KEY_PREFIX = "service:weather:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private static final Map<Integer, String> WMO_CODES = Map.ofEntries(
            Map.entry(0, "Clear sky"),
            Map.entry(1, "Mainly clear"),
            Map.entry(2, "Partly cloudy"),
            Map.entry(3, "Overcast"),
            Map.entry(45, "Foggy"),
            Map.entry(48, "Depositing rime fog"),
            Map.entry(51, "Light drizzle"),
            Map.entry(53, "Moderate drizzle"),
            Map.entry(55, "Dense drizzle"),
            Map.entry(61, "Slight rain"),
            Map.entry(63, "Moderate rain"),
            Map.entry(65, "Heavy rain"),
            Map.entry(71, "Slight snow"),
            Map.entry(73, "Moderate snow"),
            Map.entry(75, "Heavy snow"),
            Map.entry(80, "Slight rain showers"),
            Map.entry(81, "Moderate rain showers"),
            Map.entry(82, "Violent rain showers"),
            Map.entry(95, "Thunderstorm"),
            Map.entry(96, "Thunderstorm with hail"),
            Map.entry(99, "Thunderstorm with heavy hail")
    );

    private final RestClient geocodingClient;
    private final RestClient forecastClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public WeatherService(@Qualifier("weatherGeocodingRestClient") RestClient geocodingClient,
                          @Qualifier("weatherForecastRestClient") RestClient forecastClient,
                          StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper) {
        this.geocodingClient = geocodingClient;
        this.forecastClient = forecastClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Get current weather information for a city. Returns temperature in Celsius, weather description, humidity, and wind speed.")
    public WeatherData getWeather(@ToolParam(description = "City name, e.g. 'Beijing', 'Shanghai'") String city) {
        String key = KEY_PREFIX + city;

        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Weather cache hit for key={}", key);
                return objectMapper.readValue(cached, WeatherData.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read weather cache for key={}", key, e);
        }

        log.debug("Weather cache miss for key={}", key);
        WeatherData data = fetchFromApi(city);

        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (Exception e) {
            log.warn("Failed to store weather cache for key={}", key, e);
        }

        return data;
    }

    private WeatherData fetchFromApi(String city) {
        try {
            // Step 1: Geocode city name to coordinates
            String geoResponse = geocodingClient.get()
                    .uri("/v1/search?name={city}&count=1", city)
                    .retrieve()
                    .body(String.class);

            if (geoResponse == null) {
                throw new ServiceException(HttpStatus.NOT_FOUND, "city_not_found", "City not found");
            }

            JsonNode geoRoot = objectMapper.readTree(geoResponse);
            JsonNode results = geoRoot.path("results");
            if (!results.isArray() || results.isEmpty()) {
                throw new ServiceException(HttpStatus.NOT_FOUND, "city_not_found", "City not found: " + city);
            }

            JsonNode location = results.get(0);
            String cityName = location.path("name").asText();
            double lat = location.path("latitude").asDouble();
            double lon = location.path("longitude").asDouble();

            // Step 2: Fetch weather by coordinates
            String weatherResponse = forecastClient.get()
                    .uri("/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m", lat, lon)
                    .retrieve()
                    .body(String.class);

            if (weatherResponse == null) {
                throw new ServiceException(HttpStatus.BAD_GATEWAY, "weather_service_unavailable",
                        "Weather service unavailable");
            }

            JsonNode weatherRoot = objectMapper.readTree(weatherResponse);
            JsonNode current = weatherRoot.path("current");

            int weatherCode = current.path("weather_code").asInt();
            String description = WMO_CODES.getOrDefault(weatherCode, "Unknown");

            return new WeatherData(
                    cityName,
                    current.path("temperature_2m").asDouble(),
                    description,
                    String.valueOf(weatherCode),
                    current.path("relative_humidity_2m").asInt(),
                    current.path("wind_speed_10m").asDouble(),
                    Instant.now()
            );
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch weather for city={}", city, e);
            throw new ServiceException(HttpStatus.BAD_GATEWAY, "weather_service_unavailable",
                    "Weather service unavailable");
        }
    }
}
