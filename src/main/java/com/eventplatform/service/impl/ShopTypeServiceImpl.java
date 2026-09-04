package com.eventplatform.service.impl;

import cn.hutool.json.JSONUtil;
import com.eventplatform.dto.Result;
import com.eventplatform.entity.ShopType;
import com.eventplatform.mapper.ShopTypeMapper;
import com.eventplatform.service.IShopTypeService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.eventplatform.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result querySort() {
        List<String> shopTypeJson = stringRedisTemplate.opsForList().range(RedisConstants.SHOP_TYPE_KEY, 0, -1);
        if (shopTypeJson != null && !shopTypeJson.isEmpty()) {
            List<ShopType> shopTypes = shopTypeJson.stream()
                    .map(shopType -> JSONUtil.toBean(shopType, ShopType.class))
                    .collect(Collectors.toList());
            return Result.ok(shopTypes);
        }
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if(shopTypes==null||shopTypes.isEmpty()){
        return Result.fail("No category data");
        }
        for (ShopType shopType : shopTypes) {
            String json = JSONUtil.toJsonStr(shopType);
            stringRedisTemplate.opsForList().rightPush(RedisConstants.SHOP_TYPE_KEY,json);
        }
        return Result.ok(shopTypes);

    }
}
