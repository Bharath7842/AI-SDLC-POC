CREATE TABLE port_requests (
    id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    customer_id VARCHAR(255) NOT NULL,
    INDEX idx_created_at (created_at),
    INDEX idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
