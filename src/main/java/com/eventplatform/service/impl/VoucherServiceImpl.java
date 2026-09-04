package com.eventplatform.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.eventplatform.dto.Result;
import com.eventplatform.entity.Voucher;
import com.eventplatform.entity.SeckillVoucher;
import com.eventplatform.mapper.VoucherMapper;
import com.eventplatform.service.IVoucherService;
import com.eventplatform.service.ISeckillVoucherService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper,Voucher> implements IVoucherService {
    private final ISeckillVoucherService seckill;
    private final JdbcTemplate db;
    public VoucherServiceImpl(ISeckillVoucherService seckill,JdbcTemplate db) { this.seckill=seckill; this.db=db; }
    @Override public Result queryVoucherOfShop(Long shopId) { return Result.ok(getBaseMapper().queryVoucherOfShop(shopId)); }
    public static void validate(Voucher voucher,boolean flashSale) {
        if(voucher.getId()!=null || voucher.getShopId()==null || voucher.getShopId()<=0
            || voucher.getTitle()==null || voucher.getTitle().isBlank() || voucher.getTitle().length()>255
            || voucher.getPayValue()==null || voucher.getPayValue()<0 || voucher.getActualValue()==null
            || voucher.getActualValue()<=0 || voucher.getPayValue()>voucher.getActualValue())
            throw new IllegalArgumentException("Invalid voucher");
        if(flashSale && (voucher.getStock()==null || voucher.getStock()<=0 || voucher.getBeginTime()==null
            || voucher.getEndTime()==null || !voucher.getBeginTime().isBefore(voucher.getEndTime())))
            throw new IllegalArgumentException("Invalid seckill window or stock");
    }
    @Override @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        validate(voucher,true);
        requireShop(voucher.getShopId());
        voucher.setType(1); voucher.setStatus(1); voucher.setCreateTime(null); voucher.setUpdateTime(null);
        if(!save(voucher)) throw new IllegalStateException("Voucher insert failed");
        SeckillVoucher stock=new SeckillVoucher();
        stock.setVoucherId(voucher.getId()); stock.setStock(voucher.getStock());
        stock.setBeginTime(voucher.getBeginTime()); stock.setEndTime(voucher.getEndTime());
        if(!seckill.save(stock)) throw new IllegalStateException("Stock insert failed");
        // Inventory is authoritative in MySQL; no non-transactional Redis pre-decrement.
    }
    @Override @Transactional
    public void addVoucher(Voucher voucher) {
        validate(voucher,false);
        requireShop(voucher.getShopId());
        voucher.setType(0); voucher.setStatus(1); voucher.setCreateTime(null); voucher.setUpdateTime(null);
        if(!save(voucher)) throw new IllegalStateException("Voucher insert failed");
    }
    private void requireShop(long id) {
        if(db.queryForObject("SELECT COUNT(*) FROM tb_shop WHERE id=?",Integer.class,id)==0)
            throw new IllegalArgumentException("Unknown shop");
    }
}
