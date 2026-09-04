package com.eventplatform;

import cn.hutool.json.JSONUtil;
import com.eventplatform.utils.*;
import com.eventplatform.entity.Shop;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheRegressionTest {
    StringRedisTemplate redis;
    ValueOperations<String,String> values;
    CacheClient cache;
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        redis=mock(StringRedisTemplate.class); values=mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values); cache=new CacheClient(redis);
    }
    @AfterEach void stop() { cache.close(); }
    @Test void coldCacheLoadsDatabaseAndNegativeCacheExpires() {
        Shop shop=new Shop(); shop.setId(1L); shop.setName("shop");
        assertThat(cache.queryWithLogicalExpire("cache:shop:",1L,Shop.class,id -> shop,30L,TimeUnit.MINUTES)).isSameAs(shop);
        verify(values).set(eq("cache:shop:1"),contains("shop"),eq(3600L),eq(TimeUnit.SECONDS));
        assertThat(cache.queryWithLogicalExpire("cache:shop:",2L,Shop.class,id -> null,30L,TimeUnit.MINUTES)).isNull();
        verify(values).set("cache:shop:2","",2,TimeUnit.MINUTES);
    }
    @Test void refreshReleasesOnlyOwnedLockAndKeepsNewCache() {
        Shop old=new Shop(); old.setId(1L); old.setName("old");
        Shop fresh=new Shop(); fresh.setId(1L); fresh.setName("fresh");
        RedisData data=new RedisData(); data.setData(old); data.setExpireTime(LocalDateTime.now().minusSeconds(1));
        when(values.get("cache:shop:1")).thenReturn(JSONUtil.toJsonStr(data));
        when(values.setIfAbsent(eq("lock:shop:1"),anyString(),eq(10L),eq(TimeUnit.SECONDS))).thenReturn(true);
        assertThat(cache.queryWithLogicalExpire("cache:shop:",1L,Shop.class,id -> fresh,30L,TimeUnit.MINUTES).getName()).isEqualTo("old");
        verify(values,timeout(3000)).set(eq("cache:shop:1"),contains("fresh"),eq(3600L),eq(TimeUnit.SECONDS));
        verify(redis,timeout(3000)).execute(any(RedisScript.class),eq(List.of("lock:shop:1")),anyString());
        verify(redis,never()).delete(anyString());
    }
}
