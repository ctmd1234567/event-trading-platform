-- Run ONCE on an existing event_trading database, after backup and with the old app stopped.
-- Check duplicates first. Resolve them manually; this migration deliberately does not delete data.
SELECT user_id,voucher_id,COUNT(*) AS duplicates FROM tb_voucher_order GROUP BY user_id,voucher_id HAVING COUNT(*)>1;
ALTER TABLE tb_voucher_order ADD CONSTRAINT uk_order_user_voucher UNIQUE(user_id,voucher_id);

CREATE TABLE tb_order_request (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    voucher_id BIGINT NOT NULL,
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
    last_error VARCHAR(255),
    INDEX ix_outbox_due(completed,next_attempt)
) ENGINE=InnoDB;
-- Existing orders are already handled through tb_voucher_order; never replay old QA/QD messages here.
