# Tasks: Simplify Currency Requirement

## Phase 1: Rewrite CurrencyRequirement
- [x] Modify `CurrencyRequirement` to use JExEconomy API directly
- [x] Add JExEconomy dependency check at runtime
- [x] Implement direct `CurrencyAdapter` usage via reflection
- [x] Keep Vault fallback for single currency
- [x] Remove all direct type references to avoid ClassNotFoundException
- [x] Use Object types and reflection to avoid compile-time dependencies
- [x] Fix Jackson serialization to handle missing JExEconomy classes
- [ ] Add proper error messages for missing plugins
- [ ] Test with JExEconomy present
- [ ] Test with only Vault present
- [ ] Test with neither present

## Phase 2: Clean Up
- [x] Remove `CurrencyBridge` interface
- [x] Remove `JExEconomyCurrencyBridge` class
- [x] Remove `VaultCurrencyBridge` class  
- [x] Remove `CurrencyRegistry` class
- [x] Remove `CurrencyBridgeRegistrar` class
- [x] Remove currency bridge registration from `RDQ.java`
- [x] Remove currency bridge README files
- [x] Update CURRENCY_REQUIREMENT_REFACTOR.md


## Phase 3: Verification


- [x] Rebuild all modules

- [ ] Start server with JExEconomy
- [ ] Test currency requirement with "coins" currency
- [ ] Verify balance checking works
- [ ] Verify consumption works
- [ ] Check error messages are clear
- [ ] Verify no timing issues

## Success Criteria
- Currency requirement works immediately on server start
- No complex registration or timing issues
- Code is simple and maintainable
- Clear error messages when plugins missing
- Works with both JExEconomy and Vault
