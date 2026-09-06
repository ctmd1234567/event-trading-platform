SET @v250 = 9900010250;
SET @v300 = 9900010300;
SET @v350 = 9900010350;
SET @v400 = 9900010400;

DELETE FROM tb_voucher_order WHERE voucher_id IN (@v250,@v300,@v350,@v400);
DELETE e FROM tb_outbox_event e
JOIN tb_order_request r ON r.id=e.id
WHERE r.voucher_id IN (@v250,@v300,@v350,@v400);
DELETE FROM tb_order_request WHERE voucher_id IN (@v250,@v300,@v350,@v400);
DELETE FROM tb_seckill_voucher WHERE voucher_id IN (@v250,@v300,@v350,@v400);
DELETE FROM tb_voucher WHERE id IN (@v250,@v300,@v350,@v400);

INSERT INTO tb_voucher
    (id,shop_id,title,sub_title,rules,pay_value,actual_value,type,status)
VALUES
    (@v250,1,'capacity 250 rps','isolated load test','synthetic users only',1,1,1,1),
    (@v300,1,'capacity 300 rps','isolated load test','synthetic users only',1,1,1,1),
    (@v350,1,'capacity 350 rps','isolated load test','synthetic users only',1,1,1,1),
    (@v400,1,'capacity 400 rps','isolated load test','synthetic users only',1,1,1,1);

INSERT INTO tb_seckill_voucher(voucher_id,stock,begin_time,end_time)
VALUES
    (@v250,2500,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY),
    (@v300,3000,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY),
    (@v350,3500,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY),
    (@v400,4000,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY);

SELECT voucher_id,stock
FROM tb_seckill_voucher
WHERE voucher_id IN (@v250,@v300,@v350,@v400)
ORDER BY voucher_id;
