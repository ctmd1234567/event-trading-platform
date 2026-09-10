SET @v800 = 9900020800;
SET @v1000 = 9900021000;
SET @v1200 = 9900021200;
SET @v1600 = 9900021600;
SET @v1800 = 9900021800;

DELETE FROM tb_voucher_order WHERE voucher_id IN (@v800,@v1000,@v1200,@v1600,@v1800);
DELETE e FROM tb_outbox_event e JOIN tb_order_request r ON r.id=e.id
WHERE r.voucher_id IN (@v800,@v1000,@v1200,@v1600,@v1800);
DELETE FROM tb_order_request WHERE voucher_id IN (@v800,@v1000,@v1200,@v1600,@v1800);
DELETE FROM tb_seckill_voucher_bucket WHERE voucher_id IN (@v800,@v1000,@v1200,@v1600,@v1800);
DELETE FROM tb_seckill_voucher WHERE voucher_id IN (@v800,@v1000,@v1200,@v1600,@v1800);
DELETE FROM tb_voucher WHERE id IN (@v800,@v1000,@v1200,@v1600,@v1800);

INSERT INTO tb_voucher(id,shop_id,title,sub_title,rules,pay_value,actual_value,type,status) VALUES
(@v800,1,'bucket capacity 800 rps','isolated load test','synthetic users only',1,1,1,1),
(@v1000,1,'bucket capacity 1000 rps','isolated load test','synthetic users only',1,1,1,1),
(@v1200,1,'bucket capacity 1200 rps','isolated load test','synthetic users only',1,1,1,1),
(@v1600,1,'bucket capacity 1600 rps','isolated load test','synthetic users only',1,1,1,1),
(@v1800,1,'bucket capacity 1800 rps','isolated load test','synthetic users only',1,1,1,1);
INSERT INTO tb_seckill_voucher(voucher_id,stock,begin_time,end_time) VALUES
(@v800,8100,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY),
(@v1000,10100,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY),
(@v1200,12100,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY),
(@v1600,16100,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY),
(@v1800,18100,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY);

INSERT INTO tb_seckill_voucher_bucket(voucher_id,bucket_id,stock)
WITH RECURSIVE buckets AS (SELECT 0 bucket_id UNION ALL SELECT bucket_id+1 FROM buckets WHERE bucket_id<15),
vouchers AS (SELECT @v800 voucher_id,8100 stock UNION ALL SELECT @v1000,10100 UNION ALL SELECT @v1200,12100
             UNION ALL SELECT @v1600,16100 UNION ALL SELECT @v1800,18100)
SELECT v.voucher_id,b.bucket_id,FLOOR(v.stock/16)+(b.bucket_id<MOD(v.stock,16))
FROM vouchers v CROSS JOIN buckets b;

SELECT voucher_id,SUM(stock) stock,COUNT(*) buckets
FROM tb_seckill_voucher_bucket WHERE voucher_id IN (@v800,@v1000,@v1200,@v1600,@v1800)
GROUP BY voucher_id ORDER BY voucher_id;
