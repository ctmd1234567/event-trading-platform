package com.eventplatform.security;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eventplatform.dto.Result;
import com.eventplatform.dto.UserDTO;
import com.eventplatform.utils.RedisConstants;
import com.eventplatform.utils.UserHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class TokenFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final Set<String> admins;
    private final ObjectMapper json;
    public TokenFilter(StringRedisTemplate redis, String adminIds, ObjectMapper json) {
        this.redis = redis;
        this.admins = new HashSet<>(Arrays.asList(adminIds.trim().split("\\s*,\\s*")));
        this.json = json;
    }
    public static String token(HttpServletRequest request) {
        String value = request.getHeader("authorization");
        if (value == null) return null;
        if (value.startsWith("Bearer ")) value = value.substring(7);
        return value.matches("[a-fA-F0-9]{32}") ? value : null;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        UserHolder.removeUser();
        SecurityContextHolder.clearContext();
        try {
            try {
                String token = token(req);
                if (token != null) {
                    String key = RedisConstants.LOGIN_USER_KEY + token;
                    Map<Object, Object> data = redis.opsForHash().entries(key);
                    if (!data.isEmpty()) {
                        UserDTO user = BeanUtil.fillBeanWithMap(data, new UserDTO(), false);
                        if (user.getId() != null && Boolean.TRUE.equals(redis.expire(key, Duration.ofMinutes(30)))) {
                            var roles = new ArrayList<SimpleGrantedAuthority>();
                            roles.add(new SimpleGrantedAuthority("ROLE_USER"));
                            if (admins.contains(user.getId().toString())) roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                            UserHolder.saveUser(user);
                            SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, roles));
                        }
                    }
                }
            } catch (DataAccessException ex) {
                res.setStatus(503);
                res.setContentType("application/json;charset=UTF-8");
            json.writeValue(res.getWriter(), Result.fail("Authentication service is temporarily unavailable"));
                return;
            }
            chain.doFilter(req, res);
        } finally {
            UserHolder.removeUser();
            SecurityContextHolder.clearContext();
        }
    }
}
