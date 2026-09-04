package com.eventplatform.service;

import com.eventplatform.dto.Result;
import com.eventplatform.entity.VoucherOrder;
import com.baomidou.mybatisplus.spring.service.IService;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

}
