package de.jexcellence.dependency.module;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeencapsulationTest {

    @BeforeAll
    static void requireModules() {
        Assumptions.assumeTrue(Runtime.version().feature() >= 9);
    }

    @AfterEach
    void cleanup() {
        Deencapsulation.closeOpenedPackages();
    }

    @Test
    void deencapsulateAndCloseOpenedPackagesExecuteWithoutError() {
        assertDoesNotThrow(() -> Deencapsulation.deencapsulate(SampleAnchor.class));
        assertDoesNotThrow(Deencapsulation::closeOpenedPackages);
    }

    @Test
    void privilegedLookupCanAccessPrivateField() throws Throwable {
        final MethodHandles.Lookup lookup = Deencapsulation.createPrivilegedLookup(Helper.class);
        final MethodHandle getter = lookup.findGetter(Helper.class, "secret", String.class);

        final Helper helper = new Helper();
        assertEquals("hidden", (String) getter.invoke(helper));
    }

    private static class SampleAnchor {
        // Simple anchor class used to verify module deencapsulation logic.
    }

    private static class Helper {

        private final String secret = "hidden";
    }
}
