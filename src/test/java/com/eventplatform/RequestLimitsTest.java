package com.eventplatform;

import com.eventplatform.security.AuthCodes;
import com.eventplatform.security.RequestLimits;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestLimitsTest {
    @Test void orderAdmissionAppliesUserVoucherAndGlobalLimits() {
        AuthCodes codes = mock(AuthCodes.class);
        RequestLimits limits = new RequestLimits(codes, 10, 10, 800, 1, 2000, 1);

        limits.order(7, 42);

        verify(codes).limitAll(
                List.of("order:user:{7}", "order:voucher:{42}", "order:global"),
                List.of(10, 800, 2000),
                List.of(10, 1, 1));
    }
}
