package com.eventplatform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eventplatform.dto.Result;
import com.eventplatform.entity.Shop;
import com.eventplatform.mapper.ShopMapper;
import com.eventplatform.service.IShopService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.eventplatform.utils.CacheClient;
import com.eventplatform.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.eventplatform.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient clientClient;
    @Override
    public Result queryById(Long id){

        Shop shop = clientClient.
                queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if(shop==null){

            return Result.fail("Shop not found");
        }
        return Result.ok(shop);

    }

    @Override
    @Transactional
    public Result create(Shop shop) {
        if (!save(shop)) return Result.fail("Failed to create shop");
        afterCommit(() -> stringRedisTemplate.delete(SHOP_GEO_KEY + shop.getTypeId()));
        return Result.ok(shop.getId());
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("Shop ID is required");
        }

        Shop previous = getById(id);
        if (previous == null || !updateById(shop)) return Result.fail("Shop not found");

        afterCommit(() -> {
            stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
            stringRedisTemplate.delete(SHOP_GEO_KEY + previous.getTypeId());
            if (shop.getTypeId() != null && !shop.getTypeId().equals(previous.getTypeId()))
                stringRedisTemplate.delete(SHOP_GEO_KEY + shop.getTypeId());
        });
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if (typeId == null || typeId <= 0 || current == null || current < 1 || current > 1000
            || ((x == null) != (y == null))
            || (x != null && (!Double.isFinite(x) || !Double.isFinite(y) || Math.abs(x)>180 || Math.abs(y)>85.05112878)))
            throw new IllegalArgumentException("Invalid search parameters");

        if(x==null||y==null){

            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        int from=(current-1)*SystemConstants.DEFAULT_PAGE_SIZE;
        int end=current*SystemConstants.DEFAULT_PAGE_SIZE;

        String key = SHOP_GEO_KEY + typeId;

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = searchGeo(key, x, y, end);
        if (results == null || results.getContent().isEmpty()) {
            rebuildGeoIndex(typeId, key);
            results = searchGeo(key, x, y, end);
        }
        if(results==null){
            return Result.ok(Collections.emptyList());
        }

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if(list.size()<=from){
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids=new ArrayList<>(list.size());
        Map<String,Distance> distanceMap=new HashMap<>(list.size());
        list.stream().skip(from).forEach(result->{

            String shopIdStr=result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));

            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr,distance);
        });

        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query()
                .in("id", ids).last("order by field(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }

        return Result.ok(shops);
    }

    private GeoResults<RedisGeoCommands.GeoLocation<String>> searchGeo(String key, double x, double y, int limit) {
        return stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().sortAscending().limit(limit)
        );
    }

    private void rebuildGeoIndex(Integer typeId, String key) {
        List<Shop> shops = query().eq("type_id", typeId).list();
        if (shops.isEmpty()) return;
        List<RedisGeoCommands.GeoLocation<String>> locations = shops.stream()
                .filter(shop -> shop.getX() != null && shop.getY() != null)
                .map(shop -> new RedisGeoCommands.GeoLocation<>(shop.getId().toString(), new Point(shop.getX(), shop.getY())))
                .toList();
        if (!locations.isEmpty()) stringRedisTemplate.opsForGeo().add(key, locations);
    }

    private void afterCommit(Runnable action) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() { action.run(); }
                });
    }
}
