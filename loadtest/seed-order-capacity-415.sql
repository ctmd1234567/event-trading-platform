SET @voucher = 9900010415;
DELETE FROM tb_voucher_order WHERE voucher_id=@voucher;
DELETE e FROM tb_outbox_event e
JOIN tb_order_request r ON r.id=e.id
WHERE r.voucher_id=@voucher;
DELETE FROM tb_order_request WHERE voucher_id=@voucher;
DELETE FROM tb_seckill_voucher WHERE voucher_id=@voucher;
DELETE FROM tb_voucher WHERE id=@voucher;

INSERT INTO tb_voucher
    (id,shop_id,title,sub_title,rules,pay_value,actual_value,type,status)
VALUES
    (@voucher,1,'capacity 415 rps','isolated load test','synthetic users only',1,1,1,1);
INSERT INTO tb_seckill_voucher(voucher_id,stock,begin_time,end_time)
VALUES
    (@voucher,4150,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY);

SELECT voucher_id,stock FROM tb_seckill_voucher WHERE voucher_id=@voucher;
