# Design: Simplified Currency Requirement

## Architecture

### Current (Overcomplicated)
```
CurrencyRequirement → CurrencyRegistry → CurrencyBridge → JExEconomy
                                      ↓
                                  VaultBridge → Vault
```

### New (Simple)
```
CurrencyRequirement → JExEconomy CurrencyAdapter (direct)
                   ↓
                   → Vault Economy (fallback)
```

## Implementation Plan

### 1. Modify CurrencyRequirement
- Add direct dependency on JExEconomy (compile-time, optional at runtime)
- Check if JExEconomy is available first
- Use JExEconomy's `CurrencyAdapter` directly via ServiceManager
- Fall back to Vault if JExEconomy not available
- Remove dependency on CurrencyRegistry

### 2. Keep Vault Support
- Vault only supports single currency ("vault")
- Use Vault as fallback when JExEconomy not present

### 3. Remove Unnecessary Code
- Delete CurrencyBridge interface and implementations
- Delete CurrencyRegistry
- Delete CurrencyBridgeRegistrar
- Remove currency bridge registration from RDQ

## API Usage

### JExEconomy Direct Usage
```java
// Get CurrencyAdapter from Bukkit Services
RegisteredServiceProvider<CurrencyAdapter> provider = 
    Bukkit.getServicesManager().getRegistration(CurrencyAdapter.class);
    
if (provider != null) {
    CurrencyAdapter adapter = provider.getProvider();
    
    // Find currency by identifier
    Currency currency = adapter.getAllCurrencies().values().stream()
        .filter(c -> c.getIdentifier().equalsIgnoreCase(currencyId))
        .findFirst().orElse(null);
    
    // Check balance
    double balance = adapter.getBalance(player, currency).join();
    
    // Consume (withdraw)
    CurrencyResponse response = adapter.withdraw(player, currency, amount).join();
    boolean success = response.isTransactionSuccessful();
}
```

### Vault Fallback
```java
RegisteredServiceProvider<Economy> provider = 
    Bukkit.getServicesManager().getRegistration(Economy.class);
    
if (provider != null) {
    Economy economy = provider.getProvider();
    double balance = economy.getBalance(player);
    EconomyResponse response = economy.withdrawPlayer(player, amount);
}
```

## Benefits
1. No timing issues - services are checked when needed, not at startup
2. No complex registration - use Bukkit's ServiceManager directly
3. Simpler code - fewer classes, less indirection
4. More reliable - direct API usage instead of reflection
5. Easier to maintain - less code to break
