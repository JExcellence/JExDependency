# Implementation Plan

- [x] 1. Refactor getCurrentBalance method to use direct API calls


  - Remove Class.forName and reflection for CurrencyAdapter
  - Import CurrencyAdapter directly from JExEconomy
  - Use ServiceManager.getRegistration(CurrencyAdapter.class) directly
  - Call adapter.getBalance(offlinePlayer, currency) without reflection
  - Wrap in try-catch for NoClassDefFoundError
  - _Requirements: 1.1, 1.5, 2.1, 2.2, 2.5_

- [x] 2. Refactor findJExCurrency method to use direct API calls


  - Remove reflection for getAllCurrencies method
  - Call adapter.getAllCurrencies() directly
  - Use Currency type instead of Object
  - Call currency.getIdentifier() directly without reflection
  - Update CURRENCY_CACHE to use Currency type
  - _Requirements: 2.1, 2.2, 2.5, 3.1_

- [x] 3. Refactor consume method to use direct API calls


  - Remove reflection for withdraw method
  - Call adapter.withdraw(offlinePlayer, currency, amount) directly
  - Use CurrencyResponse type instead of Object
  - Call response.isTransactionSuccessful() directly
  - Wrap in try-catch for NoClassDefFoundError
  - _Requirements: 1.2, 2.1, 2.2, 2.5_

- [x] 4. Refactor getCurrencyDisplayName method to use direct API calls


  - Remove reflection for getDisplayName method
  - Call currency.getDisplayName() directly
  - Simplify error handling (no NoSuchMethodException)
  - _Requirements: 2.1, 2.2, 2.5_

- [x] 5. Update error handling and logging


  - Replace NoSuchMethodException catches with NoClassDefFoundError
  - Update log messages to reflect direct API usage
  - Add FINE level logging for JExEconomy not available
  - Remove reflection-related error messages
  - Use centralized logging system
  - _Requirements: 1.4, 2.4, 3.2, 3.3, 3.4, 3.5_

- [x] 6. Add direct imports for JExEconomy classes

  - Import de.jexcellence.economy.adapter.CurrencyAdapter
  - Import de.jexcellence.economy.database.entity.Currency
  - Import de.jexcellence.economy.adapter.CurrencyResponse
  - _Requirements: 2.1, 2.2_

- [x] 7. Update type declarations and casts

  - Change CURRENCY_CACHE from Map<String, Object> to Map<String, Currency>
  - Remove @SuppressWarnings("unchecked") annotations where no longer needed
  - Update method return types to use Currency instead of Object
  - _Requirements: 2.1, 2.2, 2.5_

- [x] 8. Test with JExEconomy present



  - Start server with JExEconomy installed
  - Create a test currency (e.g., "coins")
  - Test balance retrieval
  - Test currency consumption
  - Verify no NoSuchMethodException errors
  - _Requirements: 1.1, 1.2, 1.5_

- [x] 9. Test with JExEconomy absent

  - Start server without JExEconomy
  - Verify ClassNotFoundException is caught
  - Verify fallback to Vault works
  - Check log messages are appropriate
  - _Requirements: 1.4, 2.4, 3.2, 3.3_

- [x] 10. Verify logging output


  - Check that successful operations log at FINE level
  - Check that errors log with sufficient detail
  - Verify currency not found messages list available currencies
  - Confirm centralized logging system is used
  - _Requirements: 3.1, 3.4, 3.5_
