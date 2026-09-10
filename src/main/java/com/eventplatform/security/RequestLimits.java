package com.eventplatform.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequestLimits {
    private final AuthCodes codes;
    private final int userMax;
    private final int userWindowSeconds;
    private final int voucherMax;
    private final int voucherWindowSeconds;
    private final int globalMax;
    private final int globalWindowSeconds;

    public RequestLimits(
            AuthCodes codes,
            @Value("${app.order.rate-limit.user-max:10}") int userMax,
            @Value("${app.order.rate-limit.user-window-seconds:10}") int userWindowSeconds,
            @Value("${app.order.rate-limit.voucher-max:420}") int voucherMax,
            @Value("${app.order.rate-limit.voucher-window-seconds:1}") int voucherWindowSeconds,
            @Value("${app.order.rate-limit.global-max:800}") int globalMax,
            @Value("${app.order.rate-limit.global-window-seconds:1}") int globalWindowSeconds) {
        this.codes = codes;
        this.userMax = userMax;
        this.userWindowSeconds = userWindowSeconds;
        this.voucherMax = voucherMax;
        this.voucherWindowSeconds = voucherWindowSeconds;
        this.globalMax = globalMax;
        this.globalWindowSeconds = globalWindowSeconds;
    }

    public void auth(HttpServletRequest request) {
        // Do not trust arbitrary X-Forwarded-For headers; configure a trusted reverse proxy separately.
        codes.limit("auth:ip:{"+request.getRemoteAddr()+"}",30,60);
    }

    public void order(long userId, long voucherId) {
        // One Lua round-trip evaluates all admission layers atomically.
        codes.limitAll(
                List.of("order:user:{"+userId+"}", "order:voucher:{"+voucherId+"}", "order:global"),
                List.of(userMax, voucherMax, globalMax),
                List.of(userWindowSeconds, voucherWindowSeconds, globalWindowSeconds));
    }
}
