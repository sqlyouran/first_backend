package com.mooc.app.filter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Configures Jackson to globally filter out sensitive fields from all JSON responses.
 */
@Component
public class SensitiveFieldFilter {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password_hash", "passwordHash",
        "salt",
        "verification_code", "verificationCode"
    );

    private final ObjectMapper objectMapper;

    public SensitiveFieldFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void configure() {
        SimpleBeanPropertyFilter filter = new SimpleBeanPropertyFilter() {
            @Override
            public void serializeAsField(Object pojo, JsonGenerator jgen,
                                         SerializerProvider provider, PropertyWriter writer) throws Exception {
                if (!SENSITIVE_FIELDS.contains(writer.getName())) {
                    writer.serializeAsField(pojo, jgen, provider);
                }
            }
        };

        FilterProvider filters = new SimpleFilterProvider()
            .addFilter("sensitiveFilter", filter)
            .setFailOnUnknownId(false);
        objectMapper.setFilterProvider(filters);
    }
}
