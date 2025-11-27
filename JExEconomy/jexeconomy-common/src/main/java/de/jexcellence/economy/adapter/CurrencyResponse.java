package de.jexcellence.economy.adapter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable response object representing the result of a currency operation within the JExEconomy system.
 * <p>
 * This record encapsulates all relevant information about a currency transaction or operation,
 * including the amount involved, resulting balance, operation status, and any error details.
 * It serves as a standardized response format for all currency adapter operations, providing
 * consistent feedback to calling code about the success or failure of requested operations.
 * </p>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Immutability:</strong> All fields are final and cannot be modified after creation</li>
 *   <li><strong>Type Safety:</strong> Uses enum for operation status to prevent invalid states</li>
 *   <li><strong>Comprehensive Information:</strong> Includes all data needed to understand operation results</li>
 *   <li><strong>Error Handling:</strong> Provides detailed error messages for failed operations</li>
 * </ul>
 *
 * <h3>Usage Patterns:</h3>
 * <p>
 * This response object is returned by all currency operations in the {@link ICurrencyAdapter}
 * interface. Calling code should always check the {@link #isTransactionSuccessful()} method
 * before proceeding with the assumption that an operation completed successfully.
 * </p>
 *
 * <h3>Response Types:</h3>
 * <ul>
 *   <li><strong>SUCCESS:</strong> Operation completed successfully with expected results</li>
 *   <li><strong>FAILURE:</strong> Operation failed due to validation, insufficient funds, or other business logic</li>
 *   <li><strong>NOT_IMPLEMENTED:</strong> Requested operation is not supported by the current implementation</li>
 * </ul>
 *
 * @param transactionAmount the amount that was modified during the operation (positive for deposits, negative for withdrawals)
 * @param resultingBalance the new balance of the account after the operation completed
 * @param operationStatus the status indicating success, failure, or not implemented
 * @param failureMessage detailed error message if the operation failed, null for successful operations
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see ICurrencyAdapter
 * @see ResponseType
 */
public record CurrencyResponse(
	double transactionAmount,
	double resultingBalance,
	@NotNull ResponseType operationStatus,
	@Nullable String failureMessage
) {
	
	/**
	 * Compact constructor for CurrencyResponse with validation and normalization.
	 * <p>
	 * This constructor validates the input parameters and ensures the response object
	 * is in a consistent state. It performs null checks on required fields and
	 * normalizes the error message based on the operation status.
	 * </p>
	 *
	 * <h3>Validation Rules:</h3>
	 * <ul>
	 *   <li>Operation status must not be null</li>
	 *   <li>Error message is normalized based on operation status</li>
	 *   <li>Successful operations should not have error messages</li>
	 *   <li>Failed operations should provide meaningful error messages</li>
	 * </ul>
	 *
	 * @throws IllegalArgumentException if operationStatus is null
	 */
	public CurrencyResponse {
		Objects.requireNonNull(
			operationStatus,
			"Operation status cannot be null"
		);
		
		if (
			operationStatus == ResponseType.SUCCESS &&
			failureMessage != null
		) {
			failureMessage = null;
		}
		
		if (
			operationStatus == ResponseType.FAILURE &&
			(failureMessage == null || failureMessage.trim().isEmpty())
		) {
			failureMessage = "Operation failed without specific error message";
		}
	}
	
	/**
	 * Determines whether the currency operation completed successfully.
	 * <p>
	 * This method provides a convenient way to check if an operation succeeded
	 * without having to directly compare the operation status enum. It should
	 * be used by all calling code to determine if the operation results are
	 * valid and can be trusted.
	 * </p>
	 *
	 * <h3>Success Criteria:</h3>
	 * <ul>
	 *   <li>Operation status must be {@link ResponseType#SUCCESS}</li>
	 *   <li>No error message should be present</li>
	 *   <li>Transaction amount and resulting balance should be valid</li>
	 * </ul>
	 *
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * CurrencyResponse response = adapter.deposit(player, currency, 100.0).join();
	 * if (response.isTransactionSuccessful()) {
	 *     // Process successful transaction
	 *     double newBalance = response.resultingBalance();
	 * } else {
	 *     // Handle failure
	 *     String error = response.failureMessage();
	 * }
	 * }</pre>
	 *
	 * @return true if the operation completed successfully, false otherwise
	 */
	public boolean isTransactionSuccessful() {
		return operationStatus == ResponseType.SUCCESS;
	}
	
	/**
	 * Determines whether the currency operation failed due to an error.
	 * <p>
	 * This method provides a convenient way to check if an operation failed
	 * and requires error handling. Failed operations typically include validation
	 * errors, insufficient funds, database errors, or other business logic failures.
	 * </p>
	 *
	 * <h3>Failure Indicators:</h3>
	 * <ul>
	 *   <li>Operation status is {@link ResponseType#FAILURE}</li>
	 *   <li>Error message should be present and descriptive</li>
	 *   <li>Transaction amount and balance may not reflect intended changes</li>
	 * </ul>
	 *
	 * @return true if the operation failed, false otherwise
	 */
	public boolean isTransactionFailed() {
		return operationStatus == ResponseType.FAILURE;
	}
	
	/**
	 * Determines whether the requested operation is not implemented.
	 * <p>
	 * This method checks if the operation was not performed because the
	 * functionality is not implemented in the current adapter version.
	 * This is useful for maintaining backward compatibility and graceful
	 * degradation when new features are added.
	 * </p>
	 *
	 * <h3>Not Implemented Scenarios:</h3>
	 * <ul>
	 *   <li>Feature is planned but not yet developed</li>
	 *   <li>Operation is not supported by the underlying system</li>
	 *   <li>Functionality is disabled by configuration</li>
	 * </ul>
	 *
	 * @return true if the operation is not implemented, false otherwise
	 */
	public boolean isOperationNotImplemented() {
		return operationStatus == ResponseType.NOT_IMPLEMENTED;
	}
	
	/**
	 * Creates a successful response with the specified transaction details.
	 * <p>
	 * This factory method provides a convenient way to create successful response
	 * objects without having to specify all parameters. It automatically sets
	 * the operation status to SUCCESS and ensures no error message is included.
	 * </p>
	 *
	 * <h3>Use Cases:</h3>
	 * <ul>
	 *   <li>Successful deposit operations</li>
	 *   <li>Successful withdrawal operations</li>
	 *   <li>Successful balance transfers</li>
	 *   <li>Any completed currency operation</li>
	 * </ul>
	 *
	 * @param transactionAmount the amount that was processed in the transaction
	 * @param newBalance the resulting balance after the transaction
	 * @return a new CurrencyResponse indicating successful operation
	 */
	public static @NotNull CurrencyResponse createSuccessfulResponse(
		final double transactionAmount,
		final double newBalance
	) {
		return new CurrencyResponse(
			transactionAmount,
			newBalance,
			ResponseType.SUCCESS,
			null
		);
	}
	
	/**
	 * Creates a failure response with the specified error details.
	 * <p>
	 * This factory method provides a convenient way to create failure response
	 * objects when operations cannot be completed. It automatically sets the
	 * operation status to FAILURE and includes the provided error message.
	 * </p>
	 *
	 * <h3>Common Failure Scenarios:</h3>
	 * <ul>
	 *   <li>Insufficient funds for withdrawal</li>
	 *   <li>Invalid currency or player data</li>
	 *   <li>Database connection errors</li>
	 *   <li>Validation failures</li>
	 * </ul>
	 *
	 * @param attemptedAmount the amount that was attempted to be processed
	 * @param currentBalance the current balance before the failed operation
	 * @param errorMessage detailed description of why the operation failed, must not be null
	 * @return a new CurrencyResponse indicating failed operation
	 * @throws IllegalArgumentException if errorMessage is null or empty
	 */
	public static @NotNull CurrencyResponse createFailureResponse(
		final double attemptedAmount,
		final double currentBalance,
		final @NotNull String errorMessage
	) {
		Objects.requireNonNull(
			errorMessage,
			"Error message cannot be null for failure responses"
		);
		
		if (
			errorMessage.trim().isEmpty()
		) {
			throw new IllegalArgumentException(
				"Error message cannot be empty for failure responses"
			);
		}
		
		return new CurrencyResponse(
			attemptedAmount,
			currentBalance,
			ResponseType.FAILURE,
			errorMessage
		);
	}
	
	/**
	 * Creates a not-implemented response for unsupported operations.
	 * <p>
	 * This factory method creates response objects for operations that are not
	 * yet implemented or not supported by the current adapter version. It helps
	 * maintain API compatibility while indicating that functionality is unavailable.
	 * </p>
	 *
	 * <h3>Usage Scenarios:</h3>
	 * <ul>
	 *   <li>New features not yet implemented</li>
	 *   <li>Operations disabled by configuration</li>
	 *   <li>Platform-specific limitations</li>
	 *   <li>Deprecated functionality</li>
	 * </ul>
	 *
	 * @param operationDescription brief description of the operation that is not implemented
	 * @return a new CurrencyResponse indicating the operation is not implemented
	 */
	public static @NotNull CurrencyResponse createNotImplementedResponse(
		final @NotNull String operationDescription
	) {
		Objects.requireNonNull(
			operationDescription,
			"Operation description cannot be null"
		);
		
		final String notImplementedMessage = String.format(
			"Operation '%s' is not implemented in the current adapter version",
			operationDescription
		);
		
		return new CurrencyResponse(
			0.0,
			0.0,
			ResponseType.NOT_IMPLEMENTED,
			notImplementedMessage
		);
	}
	
	/**
	 * Enumeration defining the possible outcomes of currency operations.
	 * <p>
	 * This enum provides a type-safe way to indicate the status of currency operations,
	 * ensuring that only valid states can be represented in response objects. Each
	 * value has specific semantics that should be understood by both implementers
	 * and consumers of the currency adapter API.
	 * </p>
	 *
	 * <h3>Status Meanings:</h3>
	 * <ul>
	 *   <li><strong>SUCCESS:</strong> Operation completed as requested without errors</li>
	 *   <li><strong>FAILURE:</strong> Operation could not be completed due to an error condition</li>
	 *   <li><strong>NOT_IMPLEMENTED:</strong> Operation is not supported by the current implementation</li>
	 * </ul>
	 *
	 * @since 1.0.0
	 */
	public enum ResponseType {
		
		/**
		 * Indicates that the currency operation completed successfully.
		 * <p>
		 * When this status is returned, the operation has been fully processed
		 * and the resulting balance and transaction amount reflect the actual
		 * changes made to the player's account. No error message should be
		 * present in responses with this status.
		 * </p>
		 */
		SUCCESS,
		
		/**
		 * Indicates that the currency operation failed to complete.
		 * <p>
		 * This status is used when an operation cannot be completed due to
		 * validation errors, insufficient funds, database issues, or other
		 * business logic failures. Responses with this status should always
		 * include a descriptive error message explaining the failure reason.
		 * </p>
		 */
		FAILURE,
		
		/**
		 * Indicates that the requested operation is not implemented.
		 * <p>
		 * This status is used for operations that are defined in the API but
		 * not yet implemented in the current adapter version. It allows for
		 * graceful handling of unsupported functionality and helps maintain
		 * backward compatibility during development.
		 * </p>
		 */
		NOT_IMPLEMENTED
	}
}