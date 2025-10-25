package de.jexcellence.economy.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyResponseTest {

        @Test
        void constructorRejectsNullOperationStatus() {
                assertThrows(
                        NullPointerException.class,
                        () -> new CurrencyResponse(10.0, 20.0, null, null)
                );
        }

        @Test
        void constructorNormalizesFailureMessageForSuccessResponses() {
                final CurrencyResponse response = new CurrencyResponse(
                        15.0,
                        30.0,
                        CurrencyResponse.ResponseType.SUCCESS,
                        "unexpected error"
                );

                assertNull(response.failureMessage());
        }

        @Test
        void constructorProvidesDefaultMessageWhenFailureMessageMissing() {
                final CurrencyResponse responseWithNullMessage = new CurrencyResponse(
                        -5.0,
                        10.0,
                        CurrencyResponse.ResponseType.FAILURE,
                        null
                );

                assertEquals(
                        "Operation failed without specific error message",
                        responseWithNullMessage.failureMessage()
                );

                final CurrencyResponse responseWithBlankMessage = new CurrencyResponse(
                        -5.0,
                        10.0,
                        CurrencyResponse.ResponseType.FAILURE,
                        "   "
                );

                assertEquals(
                        "Operation failed without specific error message",
                        responseWithBlankMessage.failureMessage()
                );
        }

        @Test
        void successfulResponseFactoryCreatesSuccessStatus() {
                final CurrencyResponse response = CurrencyResponse.createSuccessfulResponse(25.0, 75.0);

                assertEquals(25.0, response.transactionAmount());
                assertEquals(75.0, response.resultingBalance());
                assertEquals(CurrencyResponse.ResponseType.SUCCESS, response.operationStatus());
                assertNull(response.failureMessage());
        }

        @Test
        void failureResponseFactoryCreatesFailureStatus() {
                final CurrencyResponse response = CurrencyResponse.createFailureResponse(
                        12.5,
                        40.0,
                        "Insufficient funds"
                );

                assertEquals(12.5, response.transactionAmount());
                assertEquals(40.0, response.resultingBalance());
                assertEquals(CurrencyResponse.ResponseType.FAILURE, response.operationStatus());
                assertEquals("Insufficient funds", response.failureMessage());
        }

        @Test
        void failureResponseFactoryRejectsBlankMessages() {
                assertThrows(
                        IllegalArgumentException.class,
                        () -> CurrencyResponse.createFailureResponse(12.0, 15.0, "   ")
                );
        }

        @Test
        void notImplementedResponseFactoryCreatesExpectedMessage() {
                final CurrencyResponse response = CurrencyResponse.createNotImplementedResponse("withdraw");

                assertEquals(CurrencyResponse.ResponseType.NOT_IMPLEMENTED, response.operationStatus());
                assertEquals(0.0, response.transactionAmount());
                assertEquals(0.0, response.resultingBalance());
                assertEquals(
                        "Operation 'withdraw' is not implemented in the current adapter version",
                        response.failureMessage()
                );
        }

        @Test
        void recordImplementsEqualityHashCodeAndToString() {
                final CurrencyResponse first = CurrencyResponse.createSuccessfulResponse(30.0, 60.0);
                final CurrencyResponse second = CurrencyResponse.createSuccessfulResponse(30.0, 60.0);
                final CurrencyResponse different = CurrencyResponse.createFailureResponse(
                        30.0,
                        60.0,
                        "Failure"
                );

                assertEquals(first, second);
                assertEquals(first.hashCode(), second.hashCode());
                assertNotEquals(first, different);
                assertTrue(first.toString().contains("CurrencyResponse"));
                assertTrue(first.toString().contains("operationStatus=SUCCESS"));
        }
}
