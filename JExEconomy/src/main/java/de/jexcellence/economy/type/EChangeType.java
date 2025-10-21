package de.jexcellence.economy.type;

/**
 * Enum representing the type of balance change.
 */
public enum EChangeType {
	
	/**
	 * Money is being added to the account.
	 */
	DEPOSIT,
	
	/**
	 * Money is being removed from the account.
	 */
	WITHDRAW,
	
	/**
	 * The balance is being set to a specific amount.
	 */
	SET,
	
	/**
	 * The balance is being transferred to another player.
	 */
	TRANSFER_OUT,
	
	/**
	 * The balance is being received from another player.
	 */
	TRANSFER_IN
	;
}
