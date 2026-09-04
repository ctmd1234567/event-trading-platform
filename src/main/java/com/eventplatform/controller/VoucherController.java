package com.eventplatform.controller;

import com.eventplatform.dto.Result;
import com.eventplatform.entity.Voucher;
import com.eventplatform.service.IVoucherService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.addVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }
}
