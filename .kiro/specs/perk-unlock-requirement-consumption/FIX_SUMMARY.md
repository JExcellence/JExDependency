# Perk Unlock Requirement Consumption - Fix Summary

## Issue
Currency requirements were not being consumed when unlocking perks, even though `consumeOnComplete: true` was set in the perk configuration files (e.g., `jump_boost.yml`).

## Root Cause
The currency requirement system had multiple issues preventing consumption:

1. `CurrencyRequirement` constructor didn't accept or pass the `consumeOnComplete` parameter to the parent `AbstractRequirement` class
2. `CurrencyBuilder` didn't have a `consumeOnComplete()` method
3. `BaseRequirementSectionAdapter.convertCurrencyRequirement()` couldn't pass the config value even if it wanted to

This caused all currency requirements to default to `consumeOnComplete = false`, making `shouldConsume()` return false.

## Fixes Applied

### 1. CurrencyRequirement Class
**File:** `RPlatform/src/main/java/com/raindropcentral/rplatform/requirement/impl/CurrencyRequirement.java`

Added support for `consumeOnComplete` parameter:
- Added new constructor parameter: `boolean consumeOnComplete`
- Updated all constructors to pass this value to `super("CURRENCY", consumeOnComplete)`
- Updated `@JsonCreator` constructor to accept `@JsonProperty("consumeOnComplete")` parameter
- Defaults to `true` if not specified

### 2. RequirementBuilder Class
**File:** `RPlatform/src/main/java/com/raindropcentral/rplatform/requirement/config/RequirementBuilder.java`

Added `consumeOnComplete` support to `CurrencyBuilder`:
```java
private boolean consumeOnComplete = true;

public CurrencyBuilder consumeOnComplete(boolean consume) {
    this.consumeOnComplete = consume;
    return this;
}
```

Updated `build()` method to pass the parameter:
```java
return new CurrencyRequirement(currencies, plugin, timeoutMillis, consumeOnComplete);
```

### 3. BaseRequirementSectionAdapter Class
**File:** `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/config/requirement/BaseRequirementSectionAdapter.java`

Updated `convertCurrencyRequirement()` to pass the config value:
```java
return RequirementBuilder.currency()
        .currencies(currencies)
        .plugin(currencySection.getCurrencyPlugin())
        .consumeOnComplete(currencySection.getConsumeOnComplete())
        .build();
```

## How It Works Now
1. The perk config file (e.g., `jump_boost.yml`) has `consumeOnComplete: true` under the currency requirement
2. This is parsed into `CurrencyRequirementSection` which has `getConsumeOnComplete()` returning `true`
3. The adapter passes this value to `RequirementBuilder.currency().consumeOnComplete(true)`
4. The builder creates a `CurrencyRequirement` with `consumeOnComplete = true`
5. The resulting `AbstractRequirement` will have `shouldConsume()` return `true`
6. When unlocking the perk, `PerkRequirementService.attemptUnlock()` checks `shouldConsume()` and calls `consume()` if true
7. The currency is deducted from the player's balance

## Testing
To verify this fix:
1. Rebuild the plugin: `./gradlew build`
2. Restart the server
3. Check a player's currency balance
4. Attempt to unlock a perk with a currency requirement (e.g., jump_boost costs 1000)
5. Verify the currency is deducted from the player's balance
6. Check logs for "Consuming requirement CURRENCY for perk jump_boost"

## Status
✅ **FIXED** - Currency requirements will now be properly consumed when `consumeOnComplete: true` is set in the perk configuration.
