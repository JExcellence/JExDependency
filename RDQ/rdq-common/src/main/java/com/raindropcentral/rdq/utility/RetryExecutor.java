package com.raindropcentral.rdq.utility;

import com.raindropcentral.rplatform.logging.CentralLogger;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetryExecutor {
	
	private static final int MAX_RETRY_ATTEMPTS = 3;
	
	private static final long RETRY_DELAY_MS = 1000;
	
	private static final Logger LOGGER = CentralLogger.getLogger(RetryExecutor.class);
	
	public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
		Exception lastException = null;
		
		for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				return operation.get();
			} catch (Exception exception) {
				lastException = exception;
				LOGGER.log(Level.WARNING, "Attempt " + attempt + "/" + MAX_RETRY_ATTEMPTS + " failed for " + operationName + ": " + exception.getMessage());
				
				if (attempt < MAX_RETRY_ATTEMPTS) {
					try {
						Thread.sleep(RETRY_DELAY_MS * attempt);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("Interrupted during retry delay", ie);
					}
				}
			}
		}
		
		throw new RuntimeException("Failed to " + operationName + " after " + MAX_RETRY_ATTEMPTS + " attempts", lastException);
	}
	
}
