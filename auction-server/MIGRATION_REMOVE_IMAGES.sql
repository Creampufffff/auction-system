-- Migration script to remove item_images artifacts added previously
USE auction_system;

-- Drop the table if exists (this will also remove any indexes on it)
DROP TABLE IF EXISTS item_images;

-- If you created any specific standalone index (should be removed by DROP TABLE), drop it explicitly
-- DROP INDEX IF EXISTS idx_items_images ON item_images; -- not necessary after DROP TABLE

-- Verify
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'auction_system' AND TABLE_NAME = 'item_images';

