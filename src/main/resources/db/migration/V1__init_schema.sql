-- V1__init_schema.sql
-- Initial database schema for API Health Platform

-- Users table
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- Monitored APIs table
CREATE TABLE monitored_apis (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(200) NOT NULL,
    url                     VARCHAR(2048) NOT NULL,
    http_method             VARCHAR(10) NOT NULL DEFAULT 'GET',
    request_headers         TEXT,
    request_body            TEXT,
    check_interval_seconds  INT NOT NULL DEFAULT 60,
    timeout_millis          INT NOT NULL DEFAULT 10000,
    latency_threshold_millis INT NOT NULL DEFAULT 500,
    expected_status_codes   VARCHAR(100) NOT NULL DEFAULT '200',
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    last_status             VARCHAR(20),
    last_checked_at         TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    owner_id                BIGINT NOT NULL,
    CONSTRAINT fk_monitored_apis_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_monitored_apis_owner ON monitored_apis(owner_id);
CREATE INDEX idx_monitored_apis_active ON monitored_apis(active);

-- Health Checks table
CREATE TABLE health_checks (
    id                  BIGSERIAL PRIMARY KEY,
    monitored_api_id    BIGINT NOT NULL,
    status              VARCHAR(20) NOT NULL,
    http_status_code    INT,
    response_time_millis BIGINT,
    error_type          VARCHAR(50),
    error_message       VARCHAR(500),
    checked_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_health_checks_api FOREIGN KEY (monitored_api_id) REFERENCES monitored_apis(id) ON DELETE CASCADE
);

CREATE INDEX idx_health_checks_api ON health_checks(monitored_api_id);
CREATE INDEX idx_health_checks_checked_at ON health_checks(checked_at);
CREATE INDEX idx_health_checks_api_checked_at ON health_checks(monitored_api_id, checked_at DESC);

-- Alerts table
CREATE TABLE alerts (
    id                  BIGSERIAL PRIMARY KEY,
    monitored_api_id    BIGINT NOT NULL,
    alert_type          VARCHAR(30) NOT NULL,
    severity            VARCHAR(20) NOT NULL,
    message             VARCHAR(1000) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    acknowledged_at     TIMESTAMP WITH TIME ZONE,
    resolved_at         TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_alerts_api FOREIGN KEY (monitored_api_id) REFERENCES monitored_apis(id) ON DELETE CASCADE
);

CREATE INDEX idx_alerts_api ON alerts(monitored_api_id);
CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_created_at ON alerts(created_at DESC);
