SET @v450 = 9900010450;
SET @v500 = 9900010500;
SET @v600 = 9900010600;

DELETE FROM tb_voucher_order WHERE voucher_id IN (@v450,@v500,@v600);
DELETE e FROM tb_outbox_event e
JOIN tb_order_request r ON r.id=e.id
WHERE r.voucher_id IN (@v450,@v500,@v600);
DELETE FROM tb_order_request WHERE voucher_id IN (@v450,@v500,@v600);
DELETE FROM tb_seckill_voucher WHERE voucher_id IN (@v450,@v500,@v600);
DELETE FROM tb_voucher WHERE id IN (@v450,@v500,@v600);

INSERT INTO tb_voucher
    (id,shop_id,title,sub_title,rules,pay_value,actual_value,type,status)
VALUES
    (@v450,1,'capacity 450 rps','isolated load test','synthetic users only',1,1,1,1),
    (@v500,1,'capacity 500 rps','isolated load test','synthetic users only',1,1,1,1),
    (@v600,1,'capacity 600 rps','isolated load test','synthetic users only',1,1,1,1);

INSERT INTO tb_seckill_voucher(voucher_id,stock,begin_time,end_time)
VALUES
    (@v450,4500,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY),
    (@v500,5000,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY),
    (@v600,6000,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY);

SELECT voucher_id,stock
FROM tb_seckill_voucher
WHERE voucher_id IN (@v450,@v500,@v600)
ORDER BY voucher_id;
