package com.eventplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.eventplatform.security.AuthCodes;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class AuthCodesTest {
    @Test void missingSmsConfigurationNeverPretendsToSend() {
        var codes=new AuthCodes(mock(StringRedisTemplate.class),new ObjectMapper(),new MockEnvironment(),"disabled","","");
        assertThatThrownBy(() -> codes.send("13900000001")).isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(503));
    }
    @Test void localCodeDisclosureRequiresLocalProfile() {
        var codes=new AuthCodes(mock(StringRedisTemplate.class),new ObjectMapper(),new MockEnvironment(),"local","","");
        assertThatThrownBy(() -> codes.send("13900000001")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void onlyAtomicVerificationSuccessAuthenticates() {
        var redis=mock(StringRedisTemplate.class);
        var codes=new AuthCodes(redis,new ObjectMapper(),new MockEnvironment(),"disabled","","");
        assertThat(codes.consume("13900000001",null)).isFalse();
        when(redis.execute(any(RedisScript.class),anyList(),eq("123456"))).thenReturn(1L,0L,-1L);
        assertThat(codes.consume("13900000001","123456")).isTrue();
        assertThat(codes.consume("13900000001","123456")).isFalse();
        assertThatThrownBy(() -> codes.consume("13900000001","123456")).isInstanceOf(ResponseStatusException.class);
    }
}
