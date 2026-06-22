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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private ExchangeRateService createService() {
        return new ExchangeRateService(restClient, redisTemplate, objectMapper, "test-api-key");
    }

    @SuppressWarnings("unchecked")
    private void mockCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
    }

    @SuppressWarnings("unchecked")
    private void mockApiResponse(String json) {
        RestClient.RequestHeadersUriSpec headersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(headersUriSpec);
        when(headersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(json);
    }

    @Test
    void getRates_validBase_returnsRates() {
        mockCacheMiss();
        mockApiResponse("""
                {
                  "result": "success",
                  "base_code": "USD",
                  "conversion_rates": {
                    "CNY": 7.24,
                    "EUR": 0.92,
                    "GBP": 0.79,
                    "JPY": 157.5
                  }
                }
                """);

        ExchangeRateService service = createService();
        ExchangeRateData result = service.getRates("USD");

        assertThat(result.base()).isEqualTo("USD");
        assertThat(result.rates()).containsKey("CNY");
        assertThat(result.rates().get("CNY")).isEqualByComparingTo(new BigDecimal("7.24"));
        assertThat(result.rates()).containsKey("EUR");
        assertThat(result.updatedAt()).isNotNull();
    }

    @Test
    void getRates_invalidCurrency_throwsException() {
        mockCacheMiss();
        mockApiResponse("""
                {
                  "result": "error",
                  "error-type": "unsupported-code"
                }
                """);

        ExchangeRateService service = createService();

        assertThatThrownBy(() -> service.getRates("INVALID"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Invalid currency code");
    }

    @Test
    void getRates_externalApiFails_throwsException() {
        mockCacheMiss();
        RestClient.RequestHeadersUriSpec headersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(headersUriSpec);
        when(headersUriSpec.uri(anyString(), any(Object[].class)))
                .thenThrow(new RuntimeException("Connection refused"));

        ExchangeRateService service = createService();

        assertThatThrownBy(() -> service.getRates("USD"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Exchange rate service unavailable");
    }

    @Test
    void getRates_cacheHit_returnsCachedData() {
        String cachedJson = """
                {"base":"USD","rates":{"CNY":7.24,"EUR":0.92},"updatedAt":"2026-06-21T10:00:00Z"}
                """;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("service:exchange-rate:USD")).thenReturn(cachedJson);

        ExchangeRateService service = createService();
        ExchangeRateData result = service.getRates("USD");

        assertThat(result.base()).isEqualTo("USD");
        assertThat(result.rates()).containsKey("CNY");
        verifyNoInteractions(restClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getRates_cacheMiss_storesInCache() {
        mockCacheMiss();
        mockApiResponse("""
                {
                  "result": "success",
                  "base_code": "EUR",
                  "conversion_rates": { "CNY": 7.89, "USD": 1.09 }
                }
                """);

        ExchangeRateService service = createService();
        service.getRates("EUR");

        verify(valueOperations).set(eq("service:exchange-rate:EUR"), anyString(), any());
    }
}
