-- Library Management System schema
-- Run this once against a fresh MySQL database, e.g.:
--   mysql -u root -p < schema.sql
--   mysql -u root -p library_db < dummy_data.sql

CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(64) PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('STUDENT', 'TEACHER', 'LIBRARIAN') NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resources (
    id               VARCHAR(64) PRIMARY KEY,
    type             ENUM('BOOK', 'DVD') NOT NULL DEFAULT 'BOOK',
    title            VARCHAR(200) NOT NULL,
    author           VARCHAR(150),
    total_copies     INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS borrow_records (
    id          VARCHAR(64) PRIMARY KEY,
    user_id     VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    borrowed_on DATE NOT NULL,
    due_date    DATE NOT NULL,
    returned_on DATE NULL,
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_borrow_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE
);

CREATE INDEX idx_borrow_user ON borrow_records(user_id);
CREATE INDEX idx_borrow_resource ON borrow_records(resource_id);
CREATE INDEX idx_borrow_active ON borrow_records(user_id, resource_id, returned_on);
