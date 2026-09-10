-- user-ms owns customers, refresh tokens, per version signing keys and login
-- attempts. Enum values are stored as plain VARCHAR strings (data model rule).

CREATE TABLE customer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(255) NOT NULL,
  mobile VARCHAR(20) NULL,
  password_hash VARCHAR(255) NOT NULL,
  password_updated_at DATETIME NOT NULL,
  key_version INT NOT NULL DEFAULT 1,
  language VARCHAR(5) NOT NULL DEFAULT 'ar',
  status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_customer_email UNIQUE (email),
  CONSTRAINT uk_customer_mobile UNIQUE (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  subject_type VARCHAR(20) NOT NULL,
  subject_id BIGINT NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  revoked_at DATETIME NULL,
  replaced_by VARCHAR(64) NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
  INDEX idx_refresh_subject (subject_type, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE signing_key (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  key_version INT NOT NULL,
  private_key TEXT NOT NULL,
  public_key TEXT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_signing_key_version UNIQUE (key_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE login_attempt (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  identifier VARCHAR(255) NOT NULL,
  ip_address VARCHAR(45) NULL,
  success BOOLEAN NOT NULL,
  reason VARCHAR(50) NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_login_attempt_identifier (identifier, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;