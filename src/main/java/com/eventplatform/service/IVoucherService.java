package com.eventplatform.service;

import com.eventplatform.dto.Result;
import com.eventplatform.entity.Voucher;
import com.baomidou.mybatisplus.spring.service.IService;

public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
    void addVoucher(Voucher voucher);
}
