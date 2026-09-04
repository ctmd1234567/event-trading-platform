package com.eventplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.eventplatform.config.SecurityConfig;
import com.eventplatform.dto.UserDTO;
import com.eventplatform.security.TokenFilter;
import com.eventplatform.utils.UserHolder;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.*;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=SecurityRegressionTest.Probe.class,properties="app.security.admin-user-ids=1",
    excludeAutoConfiguration=org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class,SecurityRegressionTest.Probe.class})
class SecurityRegressionTest {
    @MockitoBean StringRedisTemplate redis;
    @Autowired MockMvc mvc;
    HashOperations<String,Object,Object> hashes;
    static final String ADMIN="a".repeat(32), USER="b".repeat(32);
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        hashes=mock(HashOperations.class); when(redis.opsForHash()).thenReturn(hashes);
        when(hashes.entries(anyString())).thenReturn(Map.of());
        when(hashes.entries("login:token:"+ADMIN)).thenReturn(Map.of("id","1"));
        when(hashes.entries("login:token:"+USER)).thenReturn(Map.of("id","2"));
        when(redis.expire(anyString(),any(Duration.class))).thenReturn(true);
    }
    @RestController static class Probe {
        @GetMapping("/shop/1") String publicShop() { return "shop"; }
        @PostMapping("/shop") String writeShop() { return "written"; }
        @PutMapping("/shop") String updateShop() { return "updated"; }
        @PostMapping("/voucher/seckill") String voucher() { return "created"; }
        @GetMapping("/user/me") Long me() { return UserHolder.getUser().getId(); }
    }
    @Test void publicReadsAndAdminOnlyWrites() throws Exception {
        mvc.perform(get("/shop/1")).andExpect(status().isOk());
        mvc.perform(post("/shop")).andExpect(status().isUnauthorized());
        mvc.perform(put("/shop").header("authorization",USER)).andExpect(status().isForbidden());
        mvc.perform(post("/voucher/seckill").header("authorization",USER)).andExpect(status().isForbidden());
        mvc.perform(post("/shop").header("authorization","Bearer "+ADMIN)).andExpect(status().isOk());
    }
    @Test void reusedRequestThreadNeverInheritsIdentity() throws Exception {
        mvc.perform(get("/user/me").header("authorization",USER)).andExpect(status().isOk()).andExpect(content().string("2"));
        assertThat(UserHolder.getUser()).isNull();
        mvc.perform(get("/user/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/user/me").header("authorization","c".repeat(32))).andExpect(status().isUnauthorized());
    }
    @Test void exceptionAlsoClearsIdentity() {
        TokenFilter filter=new TokenFilter(redis,"1",new ObjectMapper());
        MockHttpServletRequest request=new MockHttpServletRequest();
        request.addHeader("authorization",USER);
        assertThatThrownBy(() -> filter.doFilter(request,new MockHttpServletResponse(),(req,res) -> {
            assertThat(UserHolder.getUser().getId()).isEqualTo(2);
            throw new ServletException("test");
        })).isInstanceOf(ServletException.class);
        assertThat(UserHolder.getUser()).isNull();
    }
}
