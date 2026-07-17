package com.mooc.app;

import com.mooc.app.service.RateLimitService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

/**
 * 测试辅助：清理 RateLimitService 内存 fallback 状态。
 * 用于 @Transactional 测试中，替代 @DirtiesContext 来重置非 DB 状态。
 */
public final class RateLimitTestHelper {

    private RateLimitTestHelper() {}

    public static void reset(RateLimitService rateLimitService) {
        Object field = ReflectionTestUtils.getField(rateLimitService, "fallbackWindows");
        if (field instanceof Map) {
            ((Map<?, ?>) field).clear();
        }
    }
}
