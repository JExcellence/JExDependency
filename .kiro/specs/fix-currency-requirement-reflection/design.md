# Design Document

## Overview

This design removes unnecessary reflection from CurrencyRequirement and replaces it with direct API calls to JExEconomy's CurrencyAdapter. Since RPlatform already has JExEconomy as a compileOnly dependency, we can use the classes directly at compile-time while still handling the case where JExEconomy is not present at runtime.

## Architecture

### Current Architecture (Problematic)
```
CurrencyRequirement
  ├─ Try: Class.forName("de.jexcellence.economy.adapter.CurrencyAdapter")
  ├─ Get service via reflection
  ├─ Call getMethod("getBalance", Player.class, Currency.class)  ❌ Wrong signature!
  └─ Invoke via reflection
```

### New Architecture (Direct API)
```
CurrencyRequirement
  ├─ Try: Import CurrencyAdapter directly
  ├─ Get service via Bukkit.getServicesManager()
  ├─ Call adapter.getBalance(offlinePlayer, currency)  ✓ Type-safe!
  └─ Catch ClassNotFoundException if JExEconomy not present
```

## Components

### 1. CurrencyRequirement Class Modifications

#### Remove Reflection-Based Approach
- Remove all `Class.forName()` calls
- Remove all `getMethod()` and `invoke()` calls
- Remove Method caching logic

#### Add Direct API Calls
- Import JExEconomy classes directly (CurrencyAdapter, Currency, CurrencyResponse)
- Use ServiceManager to get CurrencyAdapter instance
- Call methods directly with proper types
- Wrap in try-catch for ClassNotFoundException/NoClassDefFoundError

#### Method Signature Fixes
Current (wrong):
```java
Method getBalanceMethod = adapter.getClass().getMethod("getBalance", Player.class, currency.getClass());
```

New (correct):
```java
CompletableFuture<Double> balanceFuture = adapter.getBalance(offlinePlayer, currency);
```

### 2. Error Handling Strategy

#### JExEconomy Available
```java
try {
    RegisteredServiceProvider<CurrencyAdapter> provider = 
        Bukkit.getServicesManager().getRegistration(CurrencyAdapter.class);
    
    if (provider != null) {
        CurrencyAdapter adapter = provider.getProvider();
        // Direct API calls here
    }
} catch (NoClassDefFoundError | ClassNotFoundException e) {
    // JExEconomy not present, fall back to Vault
}
```

#### JExEconomy Not Available
- Catch ClassNotFoundException or NoClassDefFoundError
- Log at FINE level that JExEconomy is not available
- Fall back to Vault implementation
- Continue gracefully

### 3. Service Discovery

#### Current (Reflection)
```java
Class<?> currencyAdapterClass = Class.forName("de.jexcellence.economy.adapter.CurrencyAdapter");
RegisteredServiceProvider<?> jexProvider = 
    Bukkit.getServicesManager().getRegistration(currencyAdapterClass);
Object adapter = jexProvider.getProvider();
```

#### New (Direct)
```java
RegisteredServiceProvider<CurrencyAdapter> jexProvider = 
    Bukkit.getServicesManager().getRegistration(CurrencyAdapter.class);
CurrencyAdapter adapter = jexProvider.getProvider();
```

## Data Models

### Currency Lookup
```java
// Current (reflection)
Method getAllCurrenciesMethod = adapter.getClass().getMethod("getAllCurrencies");
Map<Long, ?> currencies = (Map<Long, ?>) getAllCurrenciesMethod.invoke(adapter);

// New (direct)
Map<Long, Currency> currencies = adapter.getAllCurrencies();
Currency currency = currencies.values().stream()
    .filter(c -> c.getIdentifier().equalsIgnoreCase(currencyId))
    .findFirst()
    .orElse(null);
```

### Balance Operations
```java
// Current (reflection with wrong signature)
Method getBalanceMethod = adapter.getClass().getMethod("getBalance", Player.class, currency.getClass());
CompletableFuture<Double> balanceFuture = (CompletableFuture<Double>) getBalanceMethod.invoke(adapter, player, currency);

// New (direct with correct signature)
CompletableFuture<Double> balanceFuture = adapter.getBalance(offlinePlayer, currency);
```

### Transaction Operations
```java
// Current (reflection)
Method withdrawMethod = adapter.getClass().getMethod("withdraw", Player.class, currency.getClass(), double.class);
CompletableFuture<?> withdrawFuture = (CompletableFuture<?>) withdrawMethod.invoke(adapter, player, currency, amount);

// New (direct)
CompletableFuture<CurrencyResponse> withdrawFuture = adapter.withdraw(offlinePlayer, currency, amount);
CurrencyResponse response = withdrawFuture.join();
boolean success = response.isTransactionSuccessful();
```

## Error Handling

### ClassNotFoundException Handling
```java
private double getCurrentBalance(@NotNull Player player) {
    try {
        // Try JExEconomy first
        RegisteredServiceProvider<CurrencyAdapter> jexProvider = 
            Bukkit.getServicesManager().getRegistration(CurrencyAdapter.class);
        
        if (jexProvider != null) {
            CurrencyAdapter adapter = jexProvider.getProvider();
            Currency currency = findJExCurrency(adapter, currencyId);
            
            if (currency != null) {
                return adapter.getBalance(player, currency).join();
            }
        }
    } catch (NoClassDefFoundError | ClassNotFoundException e) {
        LOGGER.log(Level.FINE, "JExEconomy not available, trying Vault");
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error getting JExEconomy balance", e);
    }
    
    // Fall back to Vault
    return getVaultBalance(player);
}
```

### Currency Not Found
```java
private Currency findJExCurrency(CurrencyAdapter adapter, String identifier) {
    Map<Long, Currency> currencies = adapter.getAllCurrencies();
    
    Currency currency = currencies.values().stream()
        .filter(c -> c.getIdentifier().equalsIgnoreCase(identifier))
        .findFirst()
        .orElse(null);
    
    if (currency == null) {
        List<String> available = currencies.values().stream()
            .map(Currency::getIdentifier)
            .collect(Collectors.toList());
        
        LOGGER.log(Level.WARNING, 
            "Currency ''{0}'' not found. Available: {1}", 
            new Object[]{identifier, available});
    }
    
    return currency;
}
```

## Testing Strategy

### Unit Tests
- Test with JExEconomy present (mock CurrencyAdapter)
- Test with JExEconomy absent (ClassNotFoundException)
- Test Vault fallback
- Test currency not found scenarios

### Integration Tests
- Test on server with JExEconomy installed
- Test on server with only Vault
- Test on server with neither
- Verify no NoSuchMethodException errors
- Verify correct balance retrieval
- Verify successful withdrawals

## Benefits

1. **Type Safety**: Compile-time checking of method signatures
2. **Performance**: No reflection overhead
3. **Maintainability**: Easier to read and understand
4. **Reliability**: No method signature mismatches
5. **IDE Support**: Auto-completion and refactoring support
6. **Debugging**: Easier to debug with direct calls

## Migration Notes

### Code Changes Required
- Remove all reflection-based method invocations
- Add direct imports for JExEconomy classes
- Update method calls to use correct signatures (OfflinePlayer instead of Player)
- Simplify error handling (no NoSuchMethodException)

### Backward Compatibility
- Maintains same external API
- Still supports Vault fallback
- No changes to serialization format
- No changes to configuration

### Risk Mitigation
- Wrap all JExEconomy code in try-catch for NoClassDefFoundError
- Test thoroughly with and without JExEconomy present
- Maintain existing Vault fallback logic
- Keep logging for debugging
