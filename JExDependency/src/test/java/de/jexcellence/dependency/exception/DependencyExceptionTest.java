package de.jexcellence.dependency.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DependencyExceptionTest {

    @ParameterizedTest(name = "{0} message constructor retains provided message")
    @MethodSource("messageConstructors")
    void messageConstructorRetainsMessage(
            final String displayName,
            final Class<? extends DependencyException> expectedType,
            final Function<String, DependencyException> constructor
    ) {
        final String message = "Dependency failure";

        final DependencyException exception = constructor.apply(message);

        assertEquals(expectedType, exception.getClass());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @ParameterizedTest(name = "{0} message and cause constructor retains provided values")
    @MethodSource("messageAndCauseConstructors")
    void messageAndCauseConstructorRetainsValues(
            final String displayName,
            final Class<? extends DependencyException> expectedType,
            final BiFunction<String, Throwable, DependencyException> constructor
    ) {
        final String message = "Dependency failure";
        final Throwable cause = new IllegalStateException("root cause");

        final DependencyException exception = constructor.apply(message, cause);

        assertEquals(expectedType, exception.getClass());
        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    private static Stream<Arguments> messageConstructors() {
        return Stream.of(
                Arguments.of(
                        "DependencyException",
                        DependencyException.class,
                        (Function<String, DependencyException>) DependencyException::new
                ),
                Arguments.of(
                        "DownloadException",
                        DownloadException.class,
                        (Function<String, DependencyException>) DownloadException::new
                ),
                Arguments.of(
                        "InjectionException",
                        InjectionException.class,
                        (Function<String, DependencyException>) InjectionException::new
                )
        );
    }

    private static Stream<Arguments> messageAndCauseConstructors() {
        return Stream.of(
                Arguments.of(
                        "DependencyException",
                        DependencyException.class,
                        (BiFunction<String, Throwable, DependencyException>) DependencyException::new
                ),
                Arguments.of(
                        "DownloadException",
                        DownloadException.class,
                        (BiFunction<String, Throwable, DependencyException>) DownloadException::new
                ),
                Arguments.of(
                        "InjectionException",
                        InjectionException.class,
                        (BiFunction<String, Throwable, DependencyException>) InjectionException::new
                )
        );
    }
}
