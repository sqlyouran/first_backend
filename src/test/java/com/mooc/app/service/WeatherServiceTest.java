package com.mooc.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mooc.app.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private RestClient geocodingClient;

    @Mock
    private RestClient forecastClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private WeatherService createService() {
        return new WeatherService(geocodingClient, forecastClient, redisTemplate, objectMapper);
    }

    @SuppressWarnings("unchecked")
    private void mockCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
    }

    @SuppressWarnings("unchecked")
    private void mockGeocodingResponse(String json) {
        RestClient.RequestHeadersUriSpec geoUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec geoHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec geoResponseSpec = mock(RestClient.ResponseSpec.class);

        when(geocodingClient.get()).thenReturn(geoUriSpec);
        when(geoUriSpec.uri(anyString(), any(Object[].class))).thenReturn(geoHeadersSpec);
        when(geoHeadersSpec.retrieve()).thenReturn(geoResponseSpec);
        when(geoResponseSpec.body(String.class)).thenReturn(json);
    }

    @SuppressWarnings("unchecked")
    private void mockForecastResponse(String json) {
        RestClient.RequestHeadersUriSpec forecastUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec forecastHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec forecastResponseSpec = mock(RestClient.ResponseSpec.class);

        when(forecastClient.get()).thenReturn(forecastUriSpec);
        when(forecastUriSpec.uri(anyString(), any(Object[].class))).thenReturn(forecastHeadersSpec);
        when(forecastHeadersSpec.retrieve()).thenReturn(forecastResponseSpec);
        when(forecastResponseSpec.body(String.class)).thenReturn(json);
    }

    @Test
    void getWeather_validCity_returnsWeatherData() {
        mockCacheMiss();
        mockGeocodingResponse("""
                {
                  "results": [{
                    "name": "Beijing",
                    "latitude": 39.9,
                    "longitude": 116.4
                  }]
                }
                """);
        mockForecastResponse("""
                {
                  "current": {
                    "temperature_2m": 25.3,
                    "relative_humidity_2m": 41,
                    "weather_code": 0,
                    "wind_speed_10m": 13.6
                  }
                }
                """);

        WeatherService service = createService();
        WeatherData result = service.getWeather("Beijing");

        assertThat(result.city()).isEqualTo("Beijing");
        assertThat(result.temperature()).isEqualTo(25.3);
        assertThat(result.description()).isEqualTo("Clear sky");
        assertThat(result.icon()).isEqualTo("0");
        assertThat(result.humidity()).isEqualTo(41);
        assertThat(result.windSpeed()).isEqualTo(13.6);
        assertThat(result.updatedAt()).isNotNull();
    }

    @Test
    void getWeather_cityNotFound_throwsException() {
        mockCacheMiss();
        mockGeocodingResponse("{\"results\": []}");

        WeatherService service = createService();

        assertThatThrownBy(() -> service.getWeather("NonExistentCity123"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("City not found");
    }

    @Test
    void getWeather_externalApiFails_throwsException() {
        mockCacheMiss();
        RestClient.RequestHeadersUriSpec geoUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(geocodingClient.get()).thenReturn(geoUriSpec);
        when(geoUriSpec.uri(anyString(), any(Object[].class)))
                .thenThrow(new RuntimeException("Connection refused"));

        WeatherService service = createService();

        assertThatThrownBy(() -> service.getWeather("Beijing"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Weather service unavailable");
    }

    @Test
    void getWeather_cacheHit_returnsCachedData() {
        String cachedJson = """
                {"city":"Beijing","temperature":25.5,"description":"clear sky","icon":"01d","humidity":60,"windSpeed":3.5,"updatedAt":"2026-06-21T10:00:00Z"}
                """;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("service:weather:Beijing")).thenReturn(cachedJson);

        WeatherService service = createService();
        WeatherData result = service.getWeather("Beijing");

        assertThat(result.city()).isEqualTo("Beijing");
        assertThat(result.temperature()).isEqualTo(25.5);
        verifyNoInteractions(geocodingClient);
        verifyNoInteractions(forecastClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getWeather_cacheMiss_storesInCache() {
        mockCacheMiss();
        mockGeocodingResponse("""
                {
                  "results": [{
                    "name": "Shanghai",
                    "latitude": 31.2,
                    "longitude": 121.5
                  }]
                }
                """);
        mockForecastResponse("""
                {
                  "current": {
                    "temperature_2m": 28.0,
                    "relative_humidity_2m": 75,
                    "weather_code": 61,
                    "wind_speed_10m": 5.0
                  }
                }
                """);

        WeatherService service = createService();
        service.getWeather("Shanghai");

        verify(valueOperations).set(eq("service:weather:Shanghai"), anyString(), any());
    }
}
