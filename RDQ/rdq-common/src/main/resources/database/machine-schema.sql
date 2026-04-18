-- ============================================
-- Machine Fabrication System Database Schema
-- ============================================
-- This file documents the database schema for the machine fabrication system.
-- The actual tables are created automatically by Hibernate based on entity annotations.
-- This file is for reference and documentation purposes only.
--
-- Schema Version: 1.0.0
-- Created: 2026-04-12
-- ============================================

-- ============================================
-- Table: rdq_machines
-- Description: Stores machine instances with their configuration and state
-- ============================================
CREATE TABLE IF NOT EXISTS rdq_machines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_uuid BINARY(16) NOT NULL,
    machine_type VARCHAR(50) NOT NULL,
    world VARCHAR(50) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    fuel_level INT NOT NULL DEFAULT 0,
    recipe_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    
    INDEX idx_machine_owner (owner_uuid),
    INDEX idx_machine_location (world, x, y, z),
    INDEX idx_machine_type (machine_type),
    INDEX idx_machine_state (state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: rdq_machine_storage
-- Description: Stores virtual storage items for machines
-- ============================================
CREATE TABLE IF NOT EXISTS rdq_machine_storage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    item_data TEXT NOT NULL,
    quantity INT NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (machine_id) REFERENCES rdq_machines(id) ON DELETE CASCADE,
    INDEX idx_machine_storage_machine (machine_id),
    INDEX idx_machine_storage_type (storage_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: rdq_machine_upgrades
-- Description: Stores applied upgrades for machines
-- ============================================
CREATE TABLE IF NOT EXISTS rdq_machine_upgrades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    upgrade_type VARCHAR(30) NOT NULL,
    level INT NOT NULL DEFAULT 1,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (machine_id) REFERENCES rdq_machines(id) ON DELETE CASCADE,
    UNIQUE KEY uk_machine_upgrade (machine_id, upgrade_type),
    INDEX idx_machine_upgrade_machine (machine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: rdq_machine_trust
-- Description: Stores trusted players for machines
-- ============================================
CREATE TABLE IF NOT EXISTS rdq_machine_trust (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    trusted_uuid BINARY(16) NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (machine_id) REFERENCES rdq_machines(id) ON DELETE CASCADE,
    UNIQUE KEY uk_machine_trust (machine_id, trusted_uuid),
    INDEX idx_machine_trust_machine (machine_id),
    INDEX idx_machine_trust_player (trusted_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Sample Queries
-- ============================================

-- Find all machines owned by a player
-- SELECT * FROM rdq_machines WHERE owner_uuid = UNHEX(REPLACE('player-uuid-here', '-', ''));

-- Find all active machines
-- SELECT * FROM rdq_machines WHERE state = 'ACTIVE';

-- Find machines at a specific location
-- SELECT * FROM rdq_machines WHERE world = 'world' AND x = 100 AND y = 64 AND z = 200;

-- Get machine with all its storage
-- SELECT m.*, s.* FROM rdq_machines m
-- LEFT JOIN rdq_machine_storage s ON m.id = s.machine_id
-- WHERE m.id = 1;

-- Get machine with all its upgrades
-- SELECT m.*, u.* FROM rdq_machines m
-- LEFT JOIN rdq_machine_upgrades u ON m.id = u.machine_id
-- WHERE m.id = 1;

-- Get machine with all trusted players
-- SELECT m.*, t.* FROM rdq_machines m
-- LEFT JOIN rdq_machine_trust t ON m.id = t.machine_id
-- WHERE m.id = 1;

-- Count machines by type
-- SELECT machine_type, COUNT(*) as count FROM rdq_machines GROUP BY machine_type;

-- Count machines by state
-- SELECT state, COUNT(*) as count FROM rdq_machines GROUP BY state;

-- ============================================
-- Maintenance Queries
-- ============================================

-- Clean up orphaned storage entries (should not happen with CASCADE)
-- DELETE FROM rdq_machine_storage WHERE machine_id NOT IN (SELECT id FROM rdq_machines);

-- Clean up orphaned upgrade entries (should not happen with CASCADE)
-- DELETE FROM rdq_machine_upgrades WHERE machine_id NOT IN (SELECT id FROM rdq_machines);

-- Clean up orphaned trust entries (should not happen with CASCADE)
-- DELETE FROM rdq_machine_trust WHERE machine_id NOT IN (SELECT id FROM rdq_machines);

-- ============================================
-- Performance Optimization
-- ============================================

-- Analyze tables for query optimization
-- ANALYZE TABLE rdq_machines, rdq_machine_storage, rdq_machine_upgrades, rdq_machine_trust;

-- Optimize tables to reclaim space
-- OPTIMIZE TABLE rdq_machines, rdq_machine_storage, rdq_machine_upgrades, rdq_machine_trust;

-- ============================================
-- Backup Recommendations
-- ============================================
-- 1. Regular backups of all machine tables
-- 2. Test restore procedures periodically
-- 3. Keep backups before major updates
-- 4. Consider point-in-time recovery for production

-- Example backup command (MySQL):
-- mysqldump -u username -p database_name rdq_machines rdq_machine_storage rdq_machine_upgrades rdq_machine_trust > machine_backup.sql

-- Example restore command (MySQL):
-- mysql -u username -p database_name < machine_backup.sql
