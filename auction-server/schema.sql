CREATE DATABASE IF NOT EXISTS auction_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE auction_system;

-- ========== USERS (Bảng cha) ==========
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'SELLER', 'BIDDER') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ========== USERS (Bảng con) ==========
CREATE TABLE IF NOT EXISTS admin (
    id VARCHAR(36) PRIMARY KEY,
    CONSTRAINT fk_admin_user
        FOREIGN KEY (id) REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS seller (
    id VARCHAR(36) PRIMARY KEY,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_seller_user
        FOREIGN KEY (id) REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bidder (
    id VARCHAR(36) PRIMARY KEY,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_bidder_user
        FOREIGN KEY (id) REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);


-- ========== ITEMS (Bảng cha) ==========
CREATE TABLE IF NOT EXISTS items (
    id VARCHAR(36) PRIMARY KEY,
    seller_id VARCHAR(36) NOT NULL,
    type ENUM('ART', 'ELECTRONICS', 'VEHICLE') NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_date VARCHAR(50) NOT NULL,
    end_date VARCHAR(50) NOT NULL,
    start_price DECIMAL(15, 2) NOT NULL,
    min_increment DECIMAL(15, 2) NOT NULL,
    highest_current_price DECIMAL(15, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_items_seller
        FOREIGN KEY (seller_id) REFERENCES seller(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- ========== ITEMS (Bảng con) ==========
CREATE TABLE IF NOT EXISTS art (
    id VARCHAR(36) PRIMARY KEY,
    author VARCHAR(255) NOT NULL,
    CONSTRAINT fk_art_item
        FOREIGN KEY (id) REFERENCES items(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS electronics (
    id VARCHAR(36) PRIMARY KEY,
    warranty_months INT NOT NULL,
    CONSTRAINT fk_electronics_item
        FOREIGN KEY (id) REFERENCES items(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS vehicle (
    id VARCHAR(36) PRIMARY KEY,
    brand VARCHAR(255) NOT NULL,
    CONSTRAINT fk_vehicle_item
        FOREIGN KEY (id) REFERENCES items(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

-- ========== AUCTIONS & BIDS ==========
CREATE TABLE IF NOT EXISTS auctions (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL,
    status ENUM('OPEN', 'RUNNING', 'FINISHED', 'CANCELED') NOT NULL DEFAULT 'OPEN',
    last_bidder_id VARCHAR(36),
    current_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_auctions_item
        FOREIGN KEY (item_id) REFERENCES items(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_auctions_last_bidder
        FOREIGN KEY (last_bidder_id) REFERENCES bidder(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS bid_transactions (
    id VARCHAR(36) PRIMARY KEY,
    auction_id VARCHAR(36) NOT NULL,
    bidder_id VARCHAR(36) NOT NULL,
    bid_amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bids_auction
        FOREIGN KEY (auction_id) REFERENCES auctions(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_bids_bidder
        FOREIGN KEY (bidder_id) REFERENCES bidder(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- ========== INDEXES ==========
CREATE INDEX idx_items_seller ON items(seller_id);
CREATE INDEX idx_items_type ON items(type);
CREATE INDEX idx_auctions_item ON auctions(item_id);
CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_auctions_last_bidder ON auctions(last_bidder_id);
CREATE INDEX idx_bids_auction_amount ON bid_transactions(auction_id, bid_amount);
CREATE INDEX idx_bids_bidder ON bid_transactions(bidder_id);

