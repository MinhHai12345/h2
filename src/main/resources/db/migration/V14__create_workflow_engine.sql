CREATE TABLE workflow_runs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workflow_type VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    request_payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_stage VARCHAR(100) NULL,
    lease_owner VARCHAR(64) NULL,
    lease_expires_at TIMESTAMP(6) NULL,
    started_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    last_error VARCHAR(2000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_workflow_run_idempotency UNIQUE (workflow_type, idempotency_key),
    INDEX idx_workflow_runs_status_lease (status, lease_expires_at),
    INDEX idx_workflow_runs_created_at (created_at)
) ENGINE=InnoDB;

CREATE TABLE workflow_stage_executions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workflow_run_id BIGINT NOT NULL,
    stage_key VARCHAR(100) NOT NULL,
    stage_order INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    checkpoint_payload JSON NULL,
    last_error VARCHAR(2000) NULL,
    started_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_workflow_stage_execution UNIQUE (workflow_run_id, stage_key),
    CONSTRAINT fk_workflow_stage_execution_run
        FOREIGN KEY (workflow_run_id) REFERENCES workflow_runs (id) ON DELETE CASCADE,
    INDEX idx_workflow_stage_status (workflow_run_id, status, stage_order)
) ENGINE=InnoDB;
