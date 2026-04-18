# Machine Fabrication System - Database Migration

## Overview

The Machine Fabrication System uses **Hibernate ORM** with automatic schema management. Database tables are created and updated automatically based on JPA entity annotations.

## Migration Strategy

### Automatic Schema Management

The project uses `hibernate.hbm2ddl.auto=update` configuration, which means:

1. **On First Startup**: Hibernate creates all machine-related tables automatically
2. **On Subsequent Startups**: Hibernate updates the schema if entity definitions change
3. **No Manual SQL Required**: Schema changes are handled by Hibernate

### Entity Classes

The following entity classes define the database schema:

- `Machine.java` → `rdq_machines` table
- `MachineStorage.java` → `rdq_machine_storage` table
- `MachineUpgrade.java` → `rdq_machine_upgrades` table
- `MachineTrust.java` → `rdq_machine_trust` table

### Schema Documentation

The `machine-schema.sql` file provides:
- Complete table definitions for reference
- Index specifications
- Foreign key relationships
- Sample queries
- Maintenance queries

**Note**: This file is for documentation only. Hibernate creates the actual tables.

## Testing Migration

### On Clean Database

1. Start the server with a fresh database
2. Hibernate will create all tables automatically
3. Check server logs for:
   ```
   [INFO] Machine system initialized successfully!
   ```

### Verification Steps

1. **Check Table Creation**:
   ```sql
   SHOW TABLES LIKE 'rdq_machine%';
   ```
   Should show 4 tables:
   - rdq_machines
   - rdq_machine_storage
   - rdq_machine_upgrades
   - rdq_machine_trust

2. **Verify Indexes**:
   ```sql
   SHOW INDEX FROM rdq_machines;
   ```
   Should show indexes on:
   - owner_uuid
   - location (world, x, y, z)
   - machine_type
   - state

3. **Check Foreign Keys**:
   ```sql
   SELECT 
     TABLE_NAME,
     CONSTRAINT_NAME,
     REFERENCED_TABLE_NAME
   FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
   WHERE TABLE_SCHEMA = 'your_database'
     AND TABLE_NAME LIKE 'rdq_machine%'
     AND REFERENCED_TABLE_NAME IS NOT NULL;
   ```

## Database Support

The machine system works with all databases supported by Hibernate:

- **H2** (default, embedded)
- **MySQL** / **MariaDB**
- **PostgreSQL**
- **Oracle**
- **Microsoft SQL Server**
- **SQLite**
- **HSQLDB**

## Rollback Strategy

If you need to remove the machine system:

```sql
-- Drop tables in correct order (respects foreign keys)
DROP TABLE IF EXISTS rdq_machine_trust;
DROP TABLE IF EXISTS rdq_machine_upgrades;
DROP TABLE IF EXISTS rdq_machine_storage;
DROP TABLE IF EXISTS rdq_machines;
```

## Production Recommendations

### Before Deployment

1. **Backup Database**: Always backup before deploying
2. **Test on Staging**: Test migration on a staging server first
3. **Monitor Logs**: Watch for Hibernate warnings/errors
4. **Verify Indexes**: Ensure indexes are created for performance

### Configuration Changes

For production, consider updating `hibernate.properties`:

```properties
# Use 'validate' instead of 'update' for production
hibernate.hbm2ddl.auto=validate

# Disable SQL logging
hibernate.show_sql=false
hibernate.format_sql=false
```

### Performance Tuning

```properties
# Increase batch size for better performance
hibernate.jdbc.batch_size=50

# Enable second-level cache if needed
hibernate.cache.use_second_level_cache=true
hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
```

## Troubleshooting

### Tables Not Created

**Symptom**: Machine system fails to initialize, tables missing

**Solution**:
1. Check `hibernate.hbm2ddl.auto` is set to `update` or `create`
2. Verify database connection in `hibernate.properties`
3. Check server logs for Hibernate errors
4. Ensure database user has CREATE TABLE permissions

### Schema Mismatch

**Symptom**: Hibernate throws schema validation errors

**Solution**:
1. Backup database
2. Set `hibernate.hbm2ddl.auto=update`
3. Restart server to apply schema changes
4. If issues persist, drop and recreate tables

### Performance Issues

**Symptom**: Slow queries, high database load

**Solution**:
1. Verify indexes exist: `SHOW INDEX FROM rdq_machines;`
2. Analyze query performance: `EXPLAIN SELECT ...`
3. Consider adding composite indexes for common queries
4. Enable query caching in Hibernate

## Migration History

### Version 1.0.0 (2026-04-12)
- Initial schema creation
- Tables: machines, storage, upgrades, trust
- Indexes: owner, location, type, state
- Foreign keys: CASCADE delete for child tables

## Support

For issues or questions:
1. Check server logs for Hibernate errors
2. Verify database configuration
3. Review entity class annotations
4. Consult Hibernate documentation

## References

- [Hibernate ORM Documentation](https://hibernate.org/orm/documentation/)
- [JPA Specification](https://jakarta.ee/specifications/persistence/)
- [Database Schema Reference](machine-schema.sql)
