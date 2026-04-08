-- =====================================================
-- JExOneblock Evolution System Migration Script V1
-- =====================================================
-- This script migrates the existing stage-based system to the new evolution-based system
-- Includes table renames, data migration, and constraint updates

-- =====================================================
-- BACKUP EXISTING DATA
-- =====================================================

-- Create backup tables for rollback purposes
CREATE TABLE IF NOT EXISTS backup_oneblock_stage AS SELECT * FROM oneblock_stage WHERE 1=0;
CREATE TABLE IF NOT EXISTS backup_stage_blocks AS SELECT * FROM stage_blocks WHERE 1=0;
CREATE TABLE IF NOT EXISTS backup_stage_entities AS SELECT * FROM stage_entities WHERE 1=0;
CREATE TABLE IF NOT EXISTS backup_stage_items AS SELECT * FROM stage_items WHERE 1=0;

-- Backup existing data
INSERT INTO backup_oneblock_stage SELECT * FROM oneblock_stage;
INSERT INTO backup_stage_blocks SELECT * FROM stage_blocks;
INSERT INTO backup_stage_entities SELECT * FROM stage_entities;
INSERT INTO backup_stage_items SELECT * FROM stage_items;

-- =====================================================
-- RENAME EXISTING TABLES TO EVOLUTION NAMING
-- =====================================================

-- Rename main stage table to evolution table
ALTER TABLE oneblock_stage RENAME TO oneblock_evolutions;

-- Rename stage content tables to evolution content tables
ALTER TABLE stage_blocks RENAME TO evolution_blocks;
ALTER TABLE stage_entities RENAME TO evolution_entities;
ALTER TABLE stage_items RENAME TO evolution_items;

-- =====================================================
-- UPDATE COLUMN NAMES FOR EVOLUTION TERMINOLOGY
-- =====================================================

-- Update main evolution table columns
ALTER TABLE oneblock_evolutions RENAME COLUMN stage_name TO evolution_name;
ALTER TABLE oneblock_evolutions RENAME COLUMN stage_number TO level;
ALTER TABLE oneblock_evolutions ADD COLUMN IF NOT EXISTS evolution_type VARCHAR(20) DEFAULT 'PREDEFINED';
ALTER TABLE oneblock_evolutions ADD COLUMN IF NOT EXISTS showcase_material VARCHAR(50);
ALTER TABLE oneblock_evolutions ADD COLUMN IF NOT EXISTS is_disabled BOOLEAN DEFAULT FALSE;

-- Update evolution blocks table
ALTER TABLE evolution_blocks RENAME COLUMN stage_id TO evolution_id;
ALTER TABLE evolution_blocks ADD COLUMN IF NOT EXISTS weight DOUBLE DEFAULT 1.0;
ALTER TABLE evolution_blocks ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;

-- Update evolution entities table  
ALTER TABLE evolution_entities RENAME COLUMN stage_id TO evolution_id;
ALTER TABLE evolution_entities ADD COLUMN IF NOT EXISTS weight DOUBLE DEFAULT 1.0;
ALTER TABLE evolution_entities ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE evolution_entities ADD COLUMN IF NOT EXISTS spawn_chance DOUBLE DEFAULT 0.1;
ALTER TABLE evolution_entities ADD COLUMN IF NOT EXISTS max_spawns_per_break INTEGER DEFAULT 1;

-- Update evolution items table
ALTER TABLE evolution_items RENAME COLUMN stage_id TO evolution_id;
ALTER TABLE evolution_items ADD COLUMN IF NOT EXISTS weight DOUBLE DEFAULT 1.0;
ALTER TABLE evolution_items ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE evolution_items ADD COLUMN IF NOT EXISTS drop_chance DOUBLE DEFAULT 0.05;
ALTER TABLE evolution_items ADD COLUMN IF NOT EXISTS max_drops_per_break INTEGER DEFAULT 1;
ALTER TABLE evolution_items ADD COLUMN IF NOT EXISTS requires_silk_touch BOOLEAN DEFAULT FALSE;

-- =====================================================
-- UPDATE PLAYER AND ISLAND TABLES
-- =====================================================

-- Update player table naming
ALTER TABLE oneblock_player RENAME TO oneblock_players;
ALTER TABLE oneblock_players ADD COLUMN IF NOT EXISTS first_joined TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE oneblock_players ADD COLUMN IF NOT EXISTS last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE oneblock_players ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE oneblock_players ADD COLUMN IF NOT EXISTS total_playtime_minutes BIGINT DEFAULT 0;

-- Update island table naming and structure
ALTER TABLE oneblock_island RENAME TO oneblock_islands;
ALTER TABLE oneblock_islands RENAME COLUMN current_level TO level;
ALTER TABLE oneblock_islands RENAME COLUMN current_experience TO experience;
ALTER TABLE oneblock_islands RENAME COLUMN maximum_size TO maximum_size;
ALTER TABLE oneblock_islands ADD COLUMN IF NOT EXISTS island_name VARCHAR(100) DEFAULT 'Island';
ALTER TABLE oneblock_islands ADD COLUMN IF NOT EXISTS island_coins BIGINT DEFAULT 0;

-- Update oneblock core to use evolution terminology
ALTER TABLE oneblock_core ADD COLUMN IF NOT EXISTS current_evolution_name VARCHAR(100) DEFAULT 'Zeus';
ALTER TABLE oneblock_core RENAME COLUMN current_evolution TO evolution_level;
ALTER TABLE oneblock_core RENAME COLUMN current_prestige TO prestige_level;
ALTER TABLE oneblock_core RENAME COLUMN current_experience TO evolution_experience;
ALTER TABLE oneblock_core RENAME COLUMN blocks_broken TO total_blocks_broken;
ALTER TABLE oneblock_core RENAME COLUMN generator_location TO oneblock_location;
ALTER TABLE oneblock_core ADD COLUMN IF NOT EXISTS prestige_points BIGINT DEFAULT 0;
ALTER TABLE oneblock_core ADD COLUMN IF NOT EXISTS last_break_timestamp BIGINT;
ALTER TABLE oneblock_core ADD COLUMN IF NOT EXISTS break_streak INTEGER DEFAULT 0;
ALTER TABLE oneblock_core ADD COLUMN IF NOT EXISTS max_break_streak INTEGER DEFAULT 0;

-- =====================================================
-- UPDATE RARITY ENUM VALUES
-- =====================================================

-- Update rarity values from stage terminology to evolution terminology
UPDATE evolution_blocks SET rarity = REPLACE(rarity, 'STAGE_', 'EVOLUTION_') WHERE rarity LIKE 'STAGE_%';
UPDATE evolution_entities SET rarity = REPLACE(rarity, 'STAGE_', 'EVOLUTION_') WHERE rarity LIKE 'STAGE_%';
UPDATE evolution_items SET rarity = REPLACE(rarity, 'STAGE_', 'EVOLUTION_') WHERE rarity LIKE 'STAGE_%';

-- Standardize rarity naming
UPDATE evolution_blocks SET rarity = 'COMMON_RARITY' WHERE rarity IN ('COMMON', 'STAGE_COMMON');
UPDATE evolution_blocks SET rarity = 'UNCOMMON_RARITY' WHERE rarity IN ('UNCOMMON', 'STAGE_UNCOMMON');
UPDATE evolution_blocks SET rarity = 'RARE_RARITY' WHERE rarity IN ('RARE', 'STAGE_RARE');
UPDATE evolution_blocks SET rarity = 'EPIC_RARITY' WHERE rarity IN ('EPIC', 'STAGE_EPIC');
UPDATE evolution_blocks SET rarity = 'LEGENDARY_RARITY' WHERE rarity IN ('LEGENDARY', 'STAGE_LEGENDARY');
UPDATE evolution_blocks SET rarity = 'SPECIAL_RARITY' WHERE rarity IN ('SPECIAL', 'STAGE_SPECIAL');
UPDATE evolution_blocks SET rarity = 'UNIQUE_RARITY' WHERE rarity IN ('UNIQUE', 'STAGE_UNIQUE');

-- Apply same updates to entities and items
UPDATE evolution_entities SET rarity = 'COMMON_RARITY' WHERE rarity IN ('COMMON', 'STAGE_COMMON');
UPDATE evolution_entities SET rarity = 'UNCOMMON_RARITY' WHERE rarity IN ('UNCOMMON', 'STAGE_UNCOMMON');
UPDATE evolution_entities SET rarity = 'RARE_RARITY' WHERE rarity IN ('RARE', 'STAGE_RARE');
UPDATE evolution_entities SET rarity = 'EPIC_RARITY' WHERE rarity IN ('EPIC', 'STAGE_EPIC');
UPDATE evolution_entities SET rarity = 'LEGENDARY_RARITY' WHERE rarity IN ('LEGENDARY', 'STAGE_LEGENDARY');
UPDATE evolution_entities SET rarity = 'SPECIAL_RARITY' WHERE rarity IN ('SPECIAL', 'STAGE_SPECIAL');
UPDATE evolution_entities SET rarity = 'UNIQUE_RARITY' WHERE rarity IN ('UNIQUE', 'STAGE_UNIQUE');

UPDATE evolution_items SET rarity = 'COMMON_RARITY' WHERE rarity IN ('COMMON', 'STAGE_COMMON');
UPDATE evolution_items SET rarity = 'UNCOMMON_RARITY' WHERE rarity IN ('UNCOMMON', 'STAGE_UNCOMMON');
UPDATE evolution_items SET rarity = 'RARE_RARITY' WHERE rarity IN ('RARE', 'STAGE_RARE');
UPDATE evolution_items SET rarity = 'EPIC_RARITY' WHERE rarity IN ('EPIC', 'STAGE_EPIC');
UPDATE evolution_items SET rarity = 'LEGENDARY_RARITY' WHERE rarity IN ('LEGENDARY', 'STAGE_LEGENDARY');
UPDATE evolution_items SET rarity = 'SPECIAL_RARITY' WHERE rarity IN ('SPECIAL', 'STAGE_SPECIAL');
UPDATE evolution_items SET rarity = 'UNIQUE_RARITY' WHERE rarity IN ('UNIQUE', 'STAGE_UNIQUE');

-- =====================================================
-- UPDATE FOREIGN KEY CONSTRAINTS
-- =====================================================

-- Drop old foreign key constraints
ALTER TABLE evolution_blocks DROP CONSTRAINT IF EXISTS fk_stage_blocks_stage;
ALTER TABLE evolution_entities DROP CONSTRAINT IF EXISTS fk_stage_entities_stage;
ALTER TABLE evolution_items DROP CONSTRAINT IF EXISTS fk_stage_items_stage;

-- Add new foreign key constraints with evolution naming
ALTER TABLE evolution_blocks 
ADD CONSTRAINT fk_evolution_blocks_evolution 
FOREIGN KEY (evolution_id) REFERENCES oneblock_evolutions(id) 
ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE evolution_entities 
ADD CONSTRAINT fk_evolution_entities_evolution 
FOREIGN KEY (evolution_id) REFERENCES oneblock_evolutions(id) 
ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE evolution_items 
ADD CONSTRAINT fk_evolution_items_evolution 
FOREIGN KEY (evolution_id) REFERENCES oneblock_evolutions(id) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- =====================================================
-- CREATE NEW INDEXES FOR PERFORMANCE
-- =====================================================

-- Main evolution table indexes
CREATE INDEX IF NOT EXISTS idx_evolution_name ON oneblock_evolutions(evolution_name);
CREATE INDEX IF NOT EXISTS idx_evolution_level ON oneblock_evolutions(level);
CREATE INDEX IF NOT EXISTS idx_evolution_disabled ON oneblock_evolutions(is_disabled);
CREATE INDEX IF NOT EXISTS idx_evolution_type ON oneblock_evolutions(evolution_type);

-- Evolution content indexes
CREATE INDEX IF NOT EXISTS idx_evolution_block_evolution_id ON evolution_blocks(evolution_id);
CREATE INDEX IF NOT EXISTS idx_evolution_block_rarity ON evolution_blocks(rarity);
CREATE INDEX IF NOT EXISTS idx_evolution_block_enabled ON evolution_blocks(is_enabled);

CREATE INDEX IF NOT EXISTS idx_evolution_entity_evolution_id ON evolution_entities(evolution_id);
CREATE INDEX IF NOT EXISTS idx_evolution_entity_rarity ON evolution_entities(rarity);
CREATE INDEX IF NOT EXISTS idx_evolution_entity_enabled ON evolution_entities(is_enabled);

CREATE INDEX IF NOT EXISTS idx_evolution_item_evolution_id ON evolution_items(evolution_id);
CREATE INDEX IF NOT EXISTS idx_evolution_item_rarity ON evolution_items(rarity);
CREATE INDEX IF NOT EXISTS idx_evolution_item_enabled ON evolution_items(is_enabled);

-- Player and island indexes
CREATE INDEX IF NOT EXISTS idx_oneblock_player_uuid ON oneblock_players(unique_id);
CREATE INDEX IF NOT EXISTS idx_oneblock_player_name ON oneblock_players(player_name);
CREATE INDEX IF NOT EXISTS idx_oneblock_player_active ON oneblock_players(is_active);

CREATE INDEX IF NOT EXISTS idx_oneblock_island_owner ON oneblock_islands(owner_id);
CREATE INDEX IF NOT EXISTS idx_oneblock_island_level ON oneblock_islands(level);
CREATE INDEX IF NOT EXISTS idx_oneblock_island_privacy ON oneblock_islands(privacy);

-- =====================================================
-- DATA VALIDATION AND CLEANUP
-- =====================================================

-- Set default evolution names for existing evolutions
UPDATE oneblock_evolutions SET evolution_name = CONCAT('Evolution_', level) 
WHERE evolution_name IS NULL OR evolution_name = '';

-- Set default evolution type for existing evolutions
UPDATE oneblock_evolutions SET evolution_type = 'PREDEFINED' 
WHERE evolution_type IS NULL OR evolution_type = '';

-- Set default showcase material
UPDATE oneblock_evolutions SET showcase_material = 'GRASS_BLOCK' 
WHERE showcase_material IS NULL;

-- Ensure all evolution content has proper weights
UPDATE evolution_blocks SET weight = 1.0 WHERE weight IS NULL OR weight <= 0;
UPDATE evolution_entities SET weight = 1.0 WHERE weight IS NULL OR weight <= 0;
UPDATE evolution_items SET weight = 1.0 WHERE weight IS NULL OR weight <= 0;

-- Set default spawn and drop chances
UPDATE evolution_entities SET spawn_chance = 0.1 WHERE spawn_chance IS NULL OR spawn_chance <= 0;
UPDATE evolution_items SET drop_chance = 0.05 WHERE drop_chance IS NULL OR drop_chance <= 0;

-- Set default max spawns/drops per break
UPDATE evolution_entities SET max_spawns_per_break = 1 WHERE max_spawns_per_break IS NULL OR max_spawns_per_break <= 0;
UPDATE evolution_items SET max_drops_per_break = 1 WHERE max_drops_per_break IS NULL OR max_drops_per_break <= 0;

-- Update oneblock core evolution names
UPDATE oneblock_core SET current_evolution_name = 'Zeus' WHERE current_evolution_name IS NULL;

-- =====================================================
-- CREATE MIGRATION LOG TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS evolution_migration_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    migration_version VARCHAR(50) NOT NULL,
    migration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    records_migrated INTEGER DEFAULT 0,
    migration_status VARCHAR(20) DEFAULT 'COMPLETED',
    notes TEXT
);

-- Log this migration
INSERT INTO evolution_migration_log (migration_version, records_migrated, notes) 
VALUES ('V1_Evolution_System_Migration', 
        (SELECT COUNT(*) FROM oneblock_evolutions), 
        'Migrated from stage-based system to evolution-based system with enhanced features');

-- =====================================================
-- VALIDATION QUERIES
-- =====================================================

-- Validate evolution table structure
SELECT 'Evolution table validation' as check_type, 
       COUNT(*) as total_evolutions,
       COUNT(CASE WHEN evolution_name IS NOT NULL THEN 1 END) as named_evolutions,
       COUNT(CASE WHEN evolution_type = 'PREDEFINED' THEN 1 END) as predefined_evolutions
FROM oneblock_evolutions;

-- Validate evolution content
SELECT 'Evolution content validation' as check_type,
       (SELECT COUNT(*) FROM evolution_blocks WHERE is_enabled = TRUE) as active_blocks,
       (SELECT COUNT(*) FROM evolution_entities WHERE is_enabled = TRUE) as active_entities,
       (SELECT COUNT(*) FROM evolution_items WHERE is_enabled = TRUE) as active_items;

-- Validate player data
SELECT 'Player data validation' as check_type,
       COUNT(*) as total_players,
       COUNT(CASE WHEN is_active = TRUE THEN 1 END) as active_players
FROM oneblock_players;

-- Validate island data  
SELECT 'Island data validation' as check_type,
       COUNT(*) as total_islands,
       AVG(level) as average_level,
       MAX(level) as max_level
FROM oneblock_islands;

-- =====================================================
-- CLEANUP TEMPORARY TABLES (OPTIONAL)
-- =====================================================

-- Uncomment these lines after verifying migration success
-- DROP TABLE IF EXISTS backup_oneblock_stage;
-- DROP TABLE IF EXISTS backup_stage_blocks;
-- DROP TABLE IF EXISTS backup_stage_entities;
-- DROP TABLE IF EXISTS backup_stage_items;

-- =====================================================
-- MIGRATION COMPLETE
-- =====================================================

SELECT 'Migration V1 completed successfully' as status, 
       NOW() as completion_time,
       'Evolution system migration from stage-based to evolution-based naming and structure' as description;