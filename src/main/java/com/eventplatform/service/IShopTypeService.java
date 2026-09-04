package com.eventplatform.service;

import com.eventplatform.dto.Result;
import com.eventplatform.entity.ShopType;
import com.baomidou.mybatisplus.spring.service.IService;

public interface IShopTypeService extends IService<ShopType> {

    Result querySort();
}
