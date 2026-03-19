# Simplify Currency Requirement System

## Problem
The current currency requirement system is overcomplicated with unnecessary abstraction layers:
- Currency bridges that wrap economy plugins
- Complex registration systems
- Timing issues with service discovery
- Multiple layers of indirection

## Root Cause
We overcomplicated a simple problem. RDQ needs to check if a player has enough currency and optionally consume it. JExEconomy already provides this functionality through its `CurrencyAdapter` API.

## Solution
Simplify the CurrencyRequirement to directly use JExEconomy's API when available, with Vault as fallback.

## Key Insights
1. RDQ has compile-time access to JExEconomy (same codebase)
2. JExEconomy's `CurrencyAdapter` provides all needed functionality:
   - `getBalance(player, currency)` - check balance
   - `withdraw(player, currency, amount)` - consume currency
   - `getAllCurrencies()` - get available currencies
3. No need for bridges, registries, or complex service discovery

## Requirements
1. Remove unnecessary currency bridge system
2. Modify CurrencyRequirement to directly use JExEconomy API
3. Keep Vault support as fallback for servers not using JExEconomy
4. Ensure proper error handling and logging
5. Make it work immediately without timing issues
