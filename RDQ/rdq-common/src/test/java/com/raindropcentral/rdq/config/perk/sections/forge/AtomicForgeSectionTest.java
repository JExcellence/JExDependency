package com.raindropcentral.rdq.config.perk.sections.forge;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AtomicForgeSectionTest {

    @Test
    void itDoesNotInstantiateSubsectionsByDefault() {
        final AtomicForgeSection forgeSection = new AtomicForgeSection(new EvaluationEnvironmentBuilder());

        assertNull(forgeSection.getAtomicAcceleratorSection(),
            "getAtomicAcceleratorSection should return null when no subsection was configured");
        assertNull(forgeSection.getAtomicEconomizerSection(),
            "getAtomicEconomizerSection should return null when no subsection was configured");
        assertNull(forgeSection.getAtomicInvestorSection(),
            "getAtomicInvestorSection should return null when no subsection was configured");
    }

    @Test
    void itReturnsConfiguredSubsectionsFromGetters() throws Exception {
        final AtomicForgeSection forgeSection = new AtomicForgeSection(new EvaluationEnvironmentBuilder());
        final AtomicAcceleratorSection acceleratorSection = new TestAtomicAcceleratorSection();
        final AtomicEconomizerSection economizerSection = new TestAtomicEconomizerSection();
        final AtomicInvestorSection investorSection = new TestAtomicInvestorSection();

        setField(forgeSection, "atomicAcceleratorSection", acceleratorSection);
        setField(forgeSection, "atomicEconomizerSection", economizerSection);
        setField(forgeSection, "atomicInvestorSection", investorSection);

        assertSame(acceleratorSection, forgeSection.getAtomicAcceleratorSection(),
            "getAtomicAcceleratorSection should expose the configured accelerator subsection reference");
        assertSame(economizerSection, forgeSection.getAtomicEconomizerSection(),
            "getAtomicEconomizerSection should expose the configured economizer subsection reference");
        assertSame(investorSection, forgeSection.getAtomicInvestorSection(),
            "getAtomicInvestorSection should expose the configured investor subsection reference");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestAtomicAcceleratorSection extends AtomicAcceleratorSection {

        private TestAtomicAcceleratorSection() {
            super(new EvaluationEnvironmentBuilder());
        }
    }

    private static final class TestAtomicEconomizerSection extends AtomicEconomizerSection {

        private TestAtomicEconomizerSection() {
            super(new EvaluationEnvironmentBuilder());
        }
    }

    private static final class TestAtomicInvestorSection extends AtomicInvestorSection {

        private TestAtomicInvestorSection() {
            super(new EvaluationEnvironmentBuilder());
        }
    }
}
