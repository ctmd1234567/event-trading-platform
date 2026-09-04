package com.eventplatform;

import com.eventplatform.entity.Shop;
import com.eventplatform.service.IShopService;
import com.eventplatform.utils.CacheClient;
import com.eventplatform.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@org.junit.jupiter.api.Tag("manual")
public class ShopCacheTest {

    @Autowired
    private IShopService shopService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheClient cacheClient;

    @Test
    public void testCacheAllShops() {
        // 1. Query all shops from the database.
        List<Shop> shopList = shopService.list();

        // 2. Write each shop to Redis.
        for (Shop shop : shopList) {
            // Build the Redis key.
            String key = RedisConstants.CACHE_SHOP_KEY + shop.getId();

            // Write the shop with a logical expiration time.
            cacheClient.setWithLogicalExpire(key, shop, RedisConstants.CACHE_SHOP_TTL, TimeUnit.HOURS);
        }

        System.out.println("All shop information has been cached to Redis.");
    }
}
