-- =====================================================
-- JExOneblock Evolution System Database Optimization
-- =====================================================
-- This file contains database optimization scripts for the evolution system
-- including indexes, constraints, and performance tuning

-- =====================================================
-- EVOLUTION TABLES INDEXES
-- =====================================================

-- Main evolution table indexes (already defined in @Table annotations)
-- These are created automatically by JPA/Hibernate:
-- - idx_evolution_name ON oneblock_evolutions(evolution_name)
-- - idx_evolution_level ON oneblock_evolutions(level)
-- - idx_evolution_disabled ON oneblock_evolutions(is_disabled)

-- Additional composite indexes for complex queries
CREATE INDEX IF NOT EXISTS idx_evolution_level_disabled 
ON oneblock_evolutions(level, is_disabled);

CREATE INDEX IF NOT EXISTS idx_evolution_experience_level 
ON oneblock_evolutions(experience_to_pass, level);

-- =====================================================
-- EVOLUTION CONTENT TABLES INDEXES
-- =====================================================

-- Evolution blocks indexes (defined in @Table annotations)
-- - idx_evolution_block_evolution_id ON evolution_blocks(evolution_id)
-- - idx_evolution_block_rarity ON evolution_blocks(rarity)
-- - idx_evolution_block_enabled ON evolution_blocks(is_enabled)
-- - idx_evolution_block_weight ON evolution_blocks(weight)
-- - idx_evolution_block_composite ON evolution_blocks(evolution_id, rarity, is_enabled)

-- Evolution entities indexes (defined in @Table annotations)
-- - idx_evolution_entity_evolution_id ON evolution_entities(evolution_id)
-- - idx_evolution_entity_rarity ON evolution_entities(rarity)
-- - idx_evolution_entity_enabled ON evolution_entities(is_enabled)
-- - idx_evolution_entity_spawn_chance ON evolution_entities(spawn_chance)
-- - idx_evolution_entity_composite ON evolution_entities(evolution_id, rarity, is_enabled)

-- Evolution items indexes (defined in @Table annotations)
-- - idx_evolution_item_evolution_id ON evolution_items(evolution_id)
-- - idx_evolution_item_rarity ON evolution_items(rarity)
-- - idx_evolution_item_enabled ON evolution_items(is_enabled)
-- - idx_evolution_item_drop_chance ON evolution_items(drop_chance)
-- - idx_evolution_item_silk_touch ON evolution_items(requires_silk_touch)
-- - idx_evolution_item_composite ON evolution_items(evolution_id, rarity, is_enabled)

-- =====================================================
-- FOREIGN KEY CONSTRAINTS
-- =====================================================

-- Ensure referential integrity for evolution content
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
-- CHECK CONSTRAINTS FOR DATA INTEGRITY
-- =====================================================

-- Evolution level constraints
ALTER TABLE oneblock_evolutions 
ADD CONSTRAINT chk_evolution_level_positive 
CHECK (level >= 0);

ALTER TABLE oneblock_evolutions 
ADD CONSTRAINT chk_evolution_experience_positive 
CHECK (experience_to_pass > 0);

-- Evolution content weight constraints
ALTER TABLE evolution_blocks 
ADD CONSTRAINT chk_evolution_block_weight_positive 
CHECK (weight > 0);

ALTER TABLE evolution_entities 
ADD CONSTRAINT chk_evolution_entity_weight_positive 
CHECK (weight > 0);

ALTER TABLE evolution_items 
ADD CONSTRAINT chk_evolution_item_weight_positive 
CHECK (weight > 0);

-- Evolution entity spawn constraints
ALTER TABLE evolution_entities 
ADD CONSTRAINT chk_evolution_entity_spawn_chance 
CHECK (spawn_chance >= 0 AND spawn_chance <= 1);

ALTER TABLE evolution_entities 
ADD CONSTRAINT chk_evolution_entity_max_spawns 
CHECK (max_spawns_per_break > 0);

-- Evolution item drop constraints
ALTER TABLE evolution_items 
ADD CONSTRAINT chk_evolution_item_drop_chance 
CHECK (drop_chance >= 0 AND drop_chance <= 1);

ALTER TABLE evolution_items 
ADD CONSTRAINT chk_evolution_item_max_drops 
CHECK (max_drops_per_break > 0);

-- Predefined evolution constraints
ALTER TABLE oneblock_evolutions 
ADD CONSTRAINT chk_predefined_evolution_priority 
CHECK (evolution_type != 'PREDEFINED' OR priority >= 0);

-- =====================================================
-- PERFORMANCE OPTIMIZATION VIEWS
-- =====================================================

-- View for active evolutions with content summary
CREATE OR REPLACE VIEW v_active_evolutions AS
SELECT 
    e.id,
    e.evolution_name,
    e.level,
    e.experience_to_pass,
    e.evolution_type,
    e.showcase_material,
    e.description,
    COUNT(DISTINCT eb.id) as block_count,
    COUNT(DISTINCT ee.id) as entity_count,
    COUNT(DISTINCT ei.id) as item_count,
    (COUNT(DISTINCT eb.id) + COUNT(DISTINCT ee.id) + COUNT(DISTINCT ei.id)) as total_content
FROM oneblock_evolutions e
LEFT JOIN evolution_blocks eb ON e.id = eb.evolution_id AND eb.is_enabled = true
LEFT JOIN evolution_entities ee ON e.id = ee.evolution_id AND ee.is_enabled = true
LEFT JOIN evolution_items ei ON e.id = ei.evolution_id AND ei.is_enabled = true
WHERE e.is_disabled = false
GROUP BY e.id, e.evolution_name, e.level, e.experience_to_pass, e.evolution_type, e.showcase_material, e.description
ORDER BY e.level;

-- View for evolution content by rarity
CREATE OR REPLACE VIEW v_evolution_content_by_rarity AS
SELECT 
    e.id as evolution_id,
    e.evolution_name,
    e.level,
    'BLOCK' as content_type,
    eb.rarity,
    COUNT(*) as content_count,
    AVG(eb.weight) as avg_weight
FROM oneblock_evolutions e
JOIN evolution_blocks eb ON e.id = eb.evolution_id
WHERE e.is_disabled = false AND eb.is_enabled = true
GROUP BY e.id, e.evolution_name, e.level, eb.rarity

UNION ALL

SELECT 
    e.id as evolution_id,
    e.evolution_name,
    e.level,
    'ENTITY' as content_type,
    ee.rarity,
    COUNT(*) as content_count,
    AVG(ee.weight) as avg_weight
FROM oneblock_evolutions e
JOIN evolution_entities ee ON e.id = ee.evolution_id
WHERE e.is_disabled = false AND ee.is_enabled = true
GROUP BY e.id, e.evolution_name, e.level, ee.rarity

UNION ALL

SELECT 
    e.id as evolution_id,
    e.evolution_name,
    e.level,
    'ITEM' as content_type,
    ei.rarity,
    COUNT(*) as content_count,
    AVG(ei.weight) as avg_weight
FROM oneblock_evolutions e
JOIN evolution_items ei ON e.id = ei.evolution_id
WHERE e.is_disabled = false AND ei.is_enabled = true
GROUP BY e.id, e.evolution_name, e.level, ei.rarity

ORDER BY evolution_id, content_type, rarity;

-- =====================================================
-- PERFORMANCE MONITORING QUERIES
-- =====================================================

-- Query to find evolutions with no content
-- SELECT e.evolution_name, e.level 
-- FROM oneblock_evolutions e
-- LEFT JOIN evolution_blocks eb ON e.id = eb.evolution_id AND eb.is_enabled = true
-- LEFT JOIN evolution_entities ee ON e.id = ee.evolution_id AND ee.is_enabled = true  
-- LEFT JOIN evolution_items ei ON e.id = ei.evolution_id AND ei.is_enabled = true
-- WHERE e.is_disabled = false 
-- AND eb.id IS NULL AND ee.id IS NULL AND ei.id IS NULL;

-- Query to find heavily weighted content (potential performance issues)
-- SELECT 'BLOCK' as type, e.evolution_name, eb.rarity, eb.weight
-- FROM oneblock_evolutions e
-- JOIN evolution_blocks eb ON e.id = eb.evolution_id
-- WHERE eb.weight > 10
-- UNION ALL
-- SELECT 'ENTITY' as type, e.evolution_name, ee.rarity, ee.weight
-- FROM oneblock_evolutions e  
-- JOIN evolution_entities ee ON e.id = ee.evolution_id
-- WHERE ee.weight > 10
-- UNION ALL
-- SELECT 'ITEM' as type, e.evolution_name, ei.rarity, ei.weight
-- FROM oneblock_evolutions e
-- JOIN evolution_items ei ON e.id = ei.evolution_id  
-- WHERE ei.weight > 10
-- ORDER BY weight DESC;

-- =====================================================
-- MAINTENANCE PROCEDURES
-- =====================================================

-- Procedure to update evolution statistics (would be implemented in application code)
-- This is a conceptual example of what could be implemented

-- DELIMITER //
-- CREATE PROCEDURE UpdateEvolutionStatistics()
-- BEGIN
--     -- Update cached content counts, average weights, etc.
--     -- This would be implemented in the application layer
-- END //
-- DELIMITER ;

-- =====================================================
-- CLEANUP AND OPTIMIZATION NOTES
-- =====================================================

-- Regular maintenance tasks:
-- 1. ANALYZE TABLE oneblock_evolutions, evolution_blocks, evolution_entities, evolution_items;
-- 2. Check for unused evolutions and content
-- 3. Monitor query performance with EXPLAIN
-- 4. Consider partitioning for very large datasets
-- 5. Regular backup of evolution configurations

-- Performance considerations:
-- 1. Use LIMIT clauses for large result sets
-- 2. Consider caching frequently accessed evolution data
-- 3. Monitor foreign key constraint performance
-- 4. Use appropriate fetch strategies in JPA (LAZY vs EAGER)
-- 5. Consider read replicas for heavy query workloads