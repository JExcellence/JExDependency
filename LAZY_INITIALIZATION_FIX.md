# Hibernate LazyInitializationException Fix

## Problem
When opening the machine GUI, a `LazyInitializationException` was thrown:
```
org.hibernate.LazyInitializationException: Cannot lazily initialize collection of role 
'com.raindropcentral.rdq.database.entity.machine.Machine.trustedPlayers' (no session)
```

This occurred at `MachineMainView.java:284` when trying to access `machine.getTrustedPlayers().size()`.

## Root Cause
The `Machine` entity has lazy-loaded relationships:
- `trustedPlayers` - `@OneToMany` relationship with `MachineTrust`
- `upgrades` - `@OneToMany` relationship with `MachineUpgrade`
- `storage` - `@OneToMany` relationship with `MachineStorage`

By default, Hibernate uses lazy loading for `@OneToMany` relationships. When the Hibernate session closes after loading the machine from the database, these collections cannot be accessed anymore, causing the `LazyInitializationException`.

## Solution
Added eager fetching using `LEFT JOIN FETCH` in all MachineRepository query methods to load the relationships within the same query/session.

### Changes Made

#### 1. Updated `findByLocation()`
```java
// Before
"SELECT m FROM Machine m WHERE m.world = :world AND m.x = :x AND m.y = :y AND m.z = :z"

// After
"SELECT m FROM Machine m " +
"LEFT JOIN FETCH m.trustedPlayers " +
"LEFT JOIN FETCH m.upgrades " +
"WHERE m.world = :world AND m.x = :x AND m.y = :y AND m.z = :z"
```

#### 2. Updated `findByOwner()`
```java
// Before
"SELECT m FROM Machine m WHERE m.ownerUuid = :ownerUuid"

// After
"SELECT DISTINCT m FROM Machine m " +
"LEFT JOIN FETCH m.trustedPlayers " +
"LEFT JOIN FETCH m.upgrades " +
"WHERE m.ownerUuid = :ownerUuid"
```

#### 3. Updated `findByType()`
```java
// Before
"SELECT m FROM Machine m WHERE m.machineType = :machineType"

// After
"SELECT DISTINCT m FROM Machine m " +
"LEFT JOIN FETCH m.trustedPlayers " +
"LEFT JOIN FETCH m.upgrades " +
"WHERE m.machineType = :machineType"
```

#### 4. Updated `findByWorld()`
```java
// Before
"SELECT m FROM Machine m WHERE m.world = :world"

// After
"SELECT DISTINCT m FROM Machine m " +
"LEFT JOIN FETCH m.trustedPlayers " +
"LEFT JOIN FETCH m.upgrades " +
"WHERE m.world = :world"
```

## Why DISTINCT?
When using multiple `JOIN FETCH` clauses, Hibernate can return duplicate rows (one for each combination of joined entities). The `DISTINCT` keyword ensures we get unique Machine entities.

## Why LEFT JOIN?
`LEFT JOIN` ensures that machines without trusted players or upgrades are still returned. If we used `INNER JOIN`, only machines with at least one trusted player AND one upgrade would be returned.

## Performance Considerations

### Pros:
- ✅ Prevents LazyInitializationException
- ✅ Single query instead of N+1 queries
- ✅ All data loaded in one transaction
- ✅ No need for open sessions in views

### Cons:
- ⚠️ Slightly larger initial query
- ⚠️ More data transferred from database
- ⚠️ More memory usage per machine

### When to Use Eager Fetching:
- ✅ When you ALWAYS need the related data (like in GUIs)
- ✅ When the relationship is small (few trusted players per machine)
- ✅ When you want to avoid N+1 query problems

### When to Use Lazy Loading:
- ✅ When you RARELY need the related data
- ✅ When the relationship is large (thousands of items)
- ✅ When you can keep the session open

## Alternative Solutions Considered

### 1. Open Session in View Pattern
```java
// Not recommended - keeps session open too long
@Transactional
public void openGUI(Player player, Machine machine) {
    // Session stays open during GUI rendering
}
```
**Rejected**: Keeping sessions open during user interaction is bad practice.

### 2. Initialize Collections Manually
```java
// Before passing to view
Hibernate.initialize(machine.getTrustedPlayers());
Hibernate.initialize(machine.getUpgrades());
```
**Rejected**: Requires extra code everywhere machines are used.

### 3. DTO Pattern
```java
public record MachineDTO(
    Long id,
    UUID ownerUuid,
    int trustedPlayerCount,
    List<UpgradeDTO> upgrades
) {
    public static MachineDTO from(Machine machine) {
        // Convert within transaction
    }
}
```
**Rejected**: Too much boilerplate for this use case, but good for complex scenarios.

### 4. Eager Fetching (CHOSEN)
```java
"SELECT m FROM Machine m " +
"LEFT JOIN FETCH m.trustedPlayers " +
"LEFT JOIN FETCH m.upgrades"
```
**Chosen**: Simple, effective, and appropriate for this use case.

## Build Status
✅ Build successful
✅ JAR file generated: `RDQ/rdq-premium/build/libs/RDQ-6.0.0-Alpha-Build-14-Premium.jar`

## Files Modified
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/repository/MachineRepository.java`

## Testing
After deploying the new JAR:
1. Place a machine
2. Right-click to open the GUI
3. The trust button should show the correct count without errors
4. All machine operations should work without LazyInitializationException

## Related Documentation
- Hibernate Fetching Strategies: https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#fetching
- N+1 Query Problem: https://vladmihalcea.com/n-plus-1-query-problem/
