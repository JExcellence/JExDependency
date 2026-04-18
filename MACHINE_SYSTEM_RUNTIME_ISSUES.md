# Machine System Runtime Issues

## Issue 1: Hibernate Entity Registration ❌

### Problem
```
Failed to load machines for chunk: java.lang.IllegalArgumentException: 
org.hibernate.query.sqm.UnknownEntityException: Could not resolve root entity 'Machine'
```

### Root Cause
The `Machine`, `MachineStorage`, `MachineTrust`, and `MachineUpgrade` entities are not being registered with Hibernate's EntityManagerFactory.

### Current State
- RDQ uses RDR's EntityManagerFactory (from RPlatform)
- RDR entities are in package: `com.raindropcentral.rdr.database.entity`
- RDQ machine entities are in package: `com.raindropcentral.rdq.machine.entity`
- JEHibernate library likely only scans RDR's entity package

### Solution Options

#### Option 1: Add RDQ Entities to Hibernate Configuration (Recommended)
The JEHibernate library needs to be configured to scan the RDQ entity package.

**Check JEHibernate documentation for:**
- Package scanning configuration
- Explicit entity registration
- Multiple package support

**Likely needs something like:**
```java
// In RPlatform initialization
jeHibernate.addPackageToScan("com.raindropcentral.rdq.machine.entity");
// OR
jeHibernate.registerEntity(Machine.class);
jeHibernate.registerEntity(MachineStorage.class);
jeHibernate.registerEntity(MachineTrust.class);
jeHibernate.registerEntity(MachineUpgrade.class);
```

#### Option 2: Create RDQ-Specific EntityManagerFactory
RDQ could create its own EntityManagerFactory with proper entity registration.

**Pros:**
- Complete control over RDQ's database configuration
- Isolated from RDR changes

**Cons:**
- Duplicate database connection management
- More complex setup

### Files Affected
- `RPlatform/src/main/java/com/raindropcentral/rplatform/RPlatform.java` (line ~461)
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/RDQ.java` (initialization)

### Entities to Register
1. `com.raindropcentral.rdq.machine.entity.Machine`
2. `com.raindropcentral.rdq.machine.entity.MachineStorage`
3. `com.raindropcentral.rdq.machine.entity.MachineTrust`
4. `com.raindropcentral.rdq.machine.entity.MachineUpgrade`

---

## Issue 2: Listener Registration Warnings ⚠️

### Problem
```
[WARN] No compatible constructor found for listener: 
com.raindropcentral.rdq.machine.listener.MachineBlockListener 
(requires JavaPlugin or context object)
```

### Analysis
This warning is misleading. The listeners ARE being registered correctly in `RDQ.java`:

```java
pluginManager.registerEvents(
    new MachineBlockListener(plugin, machineService, machineManager, ...),
    plugin
);
```

### Root Cause
The warning is coming from a different system (possibly an event listener auto-registration system) that's trying to find and register listeners automatically but can't instantiate them because they require constructor parameters.

### Impact
- **No functional impact** - listeners are working correctly
- Just a warning message in logs

### Solution
This can be safely ignored, or the auto-registration system can be configured to skip these listeners since they're manually registered.

---

## Issue 3: Block Layout Converter (Future Enhancement)

### Requirement
Need a converter for block layout strings like:
```yaml
layout:
  - "XXX"
  - "XCX"
  - "XXX"
```

### Current State
- No converter exists for this format
- Layouts are currently hardcoded in structure configuration

### Proposed Solution
Create a converter in either:
1. **RPlatform** - If this is a general-purpose feature
2. **RDQ** - If this is machine-specific

### Converter Location Options

#### Option 1: RPlatform Converter (Recommended if reusable)
```
RPlatform/src/main/java/com/raindropcentral/rplatform/converter/BlockLayoutConverter.java
```

**Pros:**
- Reusable across all plugins
- Centralized location
- Follows RPlatform patterns

#### Option 2: RDQ Converter (If machine-specific)
```
RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/converter/BlockLayoutConverter.java
```

**Pros:**
- Machine-specific logic
- Easier to customize for machine needs

### Converter Interface
```java
public class BlockLayoutConverter implements IConverter<String[], BlockLayout> {
    @Override
    public BlockLayout convert(String[] layout) {
        // Parse layout strings into BlockLayout object
        // Handle character mapping (X = structure block, C = core, etc.)
        return new BlockLayout(layout);
    }
}
```

### Usage
```yaml
# In machines.yml
fabricator:
  structure:
    layout:
      - "XXX"
      - "XCX"  # C = core block
      - "XXX"
```

---

## Priority Actions

### Immediate (Blocking)
1. ✅ **Fix Hibernate entity registration** - Machine system cannot function without this
   - Investigate JEHibernate package scanning
   - Add RDQ entity package to scan list
   - OR explicitly register Machine entities

### Short Term
2. ⚠️ **Verify listener warnings** - Confirm they're harmless
   - Check if auto-registration can be disabled for these listeners
   - Document that manual registration is intentional

### Future Enhancement
3. 📋 **Implement BlockLayoutConverter** - Nice to have for cleaner config
   - Decide on location (RPlatform vs RDQ)
   - Implement converter
   - Update configuration loading

---

## Testing Checklist

After fixing Hibernate registration:
- [ ] Server starts without entity errors
- [ ] Machines can be created
- [ ] Machines persist to database
- [ ] Machines load from database on chunk load
- [ ] Machine storage works
- [ ] Machine upgrades work
- [ ] Machine trust system works
- [ ] No "UnknownEntityException" errors in logs
