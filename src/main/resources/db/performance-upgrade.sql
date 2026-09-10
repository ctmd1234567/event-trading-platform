-- Run ONCE on a database created before the stock-bucket/outbox-batch release.
-- Stop the old application and back up the database first.
ALTER TABLE tb_order_request ADD COLUMN stock_bucket SMALLINT UNSIGNED NULL AFTER voucher_id;
ALTER TABLE tb_outbox_event
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER next_attempt,
    ADD COLUMN lease_owner VARCHAR(36) NULL AFTER created_at,
    ADD INDEX ix_outbox_lease(lease_owner);

CREATE TABLE tb_seckill_voucher_bucket (
    voucher_id BIGINT UNSIGNED NOT NULL,
    bucket_id SMALLINT UNSIGNED NOT NULL,
    stock INT NOT NULL,
    PRIMARY KEY(voucher_id,bucket_id)
) ENGINE=InnoDB;

INSERT INTO tb_seckill_voucher_bucket(voucher_id,bucket_id,stock)
WITH RECURSIVE buckets AS (SELECT 0 bucket_id UNION ALL SELECT bucket_id+1 FROM buckets WHERE bucket_id<15)
SELECT s.voucher_id,b.bucket_id,FLOOR(s.stock/16)+(b.bucket_id<MOD(s.stock,16))
FROM tb_seckill_voucher s CROSS JOIN buckets b;
