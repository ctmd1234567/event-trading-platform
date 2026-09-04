package com.eventplatform.service;

import com.eventplatform.dto.Result;
import com.eventplatform.entity.Shop;
import com.baomidou.mybatisplus.spring.service.IService;

public interface IShopService extends IService<Shop> {
    Result queryById(Long id);

    Result create(Shop shop);

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
