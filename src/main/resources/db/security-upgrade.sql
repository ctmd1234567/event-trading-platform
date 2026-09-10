-- Run ONCE on an existing event_trading database, after backup and with the old app stopped.
-- Check duplicates first. Resolve them manually; this migration deliberately does not delete data.
SELECT user_id,voucher_id,COUNT(*) AS duplicates FROM tb_voucher_order GROUP BY user_id,voucher_id HAVING COUNT(*)>1;
ALTER TABLE tb_voucher_order ADD CONSTRAINT uk_order_user_voucher UNIQUE(user_id,voucher_id);

CREATE TABLE tb_order_request (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    voucher_id BIGINT NOT NULL,
    stock_bucket SMALLINT UNSIGNED,
    state VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_request_user_voucher UNIQUE(user_id,voucher_id)
) ENGINE=InnoDB;
CREATE TABLE tb_outbox_event (
    id BIGINT NOT NULL PRIMARY KEY,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_owner VARCHAR(36),
    last_error VARCHAR(255),
    INDEX ix_outbox_due(completed,next_attempt),
    INDEX ix_outbox_lease(lease_owner)
) ENGINE=InnoDB;
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
-- Existing orders are already handled through tb_voucher_order; never replay old QA/QD messages here.
