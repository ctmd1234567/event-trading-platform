package com.eventplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher_order")
public class VoucherOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long userId;

    private Long voucherId;

    /** Payment method: 1 balance; 2 Alipay; 3 WeChat Pay. */
    private Integer payType;

    /** 1 pending; 2 paid; 3 redeemed; 4 canceled; 5 refunding; 6 refunded. */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private LocalDateTime useTime;

    private LocalDateTime refundTime;

    private LocalDateTime updateTime;

}
