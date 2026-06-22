package com.mooc.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
    private static final String KEY_PREFIX = "service:exchange-rate:";
    private static final Duration TTL = Duration.ofHours(6);

    private final RestClient restClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public ExchangeRateService(@Qualifier("exchangeRateRestClient") RestClient restClient,
                               StringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper,
                               @Value("${app.exchange-rate.api-key}") String apiKey) {
        this.restClient = restClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    @Tool(description = "Get exchange rates from a base currency. Returns rates against all available currencies including CNY.")
    public ExchangeRateData getRates(@ToolParam(description = "Base currency code, e.g. 'USD', 'EUR', 'GBP'") String base) {
        String key = KEY_PREFIX + base;

        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Exchange rate cache hit for key={}", key);
                return objectMapper.readValue(cached, ExchangeRateData.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read exchange rate cache for key={}", key, e);
        }

        log.debug("Exchange rate cache miss for key={}", key);
        ExchangeRateData data = fetchFromApi(base);

        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (Exception e) {
            log.warn("Failed to store exchange rate cache for key={}", key, e);
        }

        return data;
    }

    private ExchangeRateData fetchFromApi(String base) {
        try {
            String response = restClient.get()
                    .uri("/{apiKey}/latest/{base}", apiKey, base)
                    .retrieve()
                    .body(String.class);

            if (response == null) {
                throw new ServiceException(HttpStatus.BAD_GATEWAY, "exchange_rate_service_unavailable",
                        "Exchange rate service unavailable");
            }

            JsonNode root = objectMapper.readTree(response);

            if ("error".equals(root.path("result").asText())) {
                throw new ServiceException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid_currency",
                        "Invalid currency code: " + base);
            }

            String baseCode = root.path("base_code").asText();
            JsonNode ratesNode = root.path("conversion_rates");
            Map<String, BigDecimal> rates = new HashMap<>();

            Iterator<Map.Entry<String, JsonNode>> fields = ratesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                rates.put(entry.getKey(), new BigDecimal(entry.getValue().asText()));
            }

            return new ExchangeRateData(baseCode, rates, Instant.now());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch exchange rates for base={}", base, e);
            throw new ServiceException(HttpStatus.BAD_GATEWAY, "exchange_rate_service_unavailable",
                    "Exchange rate service unavailable");
        }
    }
}
