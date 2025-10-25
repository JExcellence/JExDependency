package com.raindropcentral.rdq.view.rank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementProgressDataTest {

        private static RequirementProgressData createProgressData(
                final boolean completed,
                final double progressPercentage,
                final RequirementStatus status
        ) {
                return new RequirementProgressData(
                        "requirement-id",
                        "requirement-type",
                        "description.key",
                        completed,
                        progressPercentage,
                        status,
                        "status-message",
                        1
                );
        }

        @Nested
        @DisplayName("Constructor validation")
        class ConstructorValidation {

                @Test
                void shouldRejectNullRequirementId() {
                        assertThrows(NullPointerException.class, () -> new RequirementProgressData(
                                null,
                                "type",
                                "description",
                                false,
                                0.5,
                                RequirementStatus.IN_PROGRESS,
                                "message",
                                0
                        ));
                }

                @Test
                void shouldRejectNullRequirementType() {
                        assertThrows(NullPointerException.class, () -> new RequirementProgressData(
                                "id",
                                null,
                                "description",
                                false,
                                0.5,
                                RequirementStatus.IN_PROGRESS,
                                "message",
                                0
                        ));
                }

                @Test
                void shouldRejectNullDescriptionKey() {
                        assertThrows(NullPointerException.class, () -> new RequirementProgressData(
                                "id",
                                "type",
                                null,
                                false,
                                0.5,
                                RequirementStatus.IN_PROGRESS,
                                "message",
                                0
                        ));
                }

                @Test
                void shouldRejectNullStatus() {
                        assertThrows(NullPointerException.class, () -> new RequirementProgressData(
                                "id",
                                "type",
                                "description",
                                false,
                                0.5,
                                null,
                                "message",
                                0
                        ));
                }

                @Test
                void shouldRejectNullStatusMessage() {
                        assertThrows(NullPointerException.class, () -> new RequirementProgressData(
                                "id",
                                "type",
                                "description",
                                false,
                                0.5,
                                RequirementStatus.IN_PROGRESS,
                                null,
                                0
                        ));
                }

                @Test
                void shouldClampProgressPercentageToRange() {
                        RequirementProgressData belowZero = createProgressData(false, -0.25, RequirementStatus.NOT_STARTED);
                        RequirementProgressData atZero = createProgressData(false, 0.0, RequirementStatus.NOT_STARTED);
                        RequirementProgressData atOne = createProgressData(true, 1.0, RequirementStatus.COMPLETED);
                        RequirementProgressData aboveOne = createProgressData(false, 1.75, RequirementStatus.IN_PROGRESS);

                        assertEquals(0.0, belowZero.getProgressPercentage(), 0.0);
                        assertEquals(0.0, atZero.getProgressPercentage(), 0.0);
                        assertEquals(1.0, atOne.getProgressPercentage(), 0.0);
                        assertEquals(1.0, aboveOne.getProgressPercentage(), 0.0);
                }
        }

        @Test
        void getProgressAsPercentageShouldRoundToNearestWholeNumber() {
                RequirementProgressData roundDown = createProgressData(false, 0.554, RequirementStatus.IN_PROGRESS);
                RequirementProgressData roundUp = createProgressData(false, 0.555, RequirementStatus.IN_PROGRESS);
                RequirementProgressData zero = createProgressData(false, 0.004, RequirementStatus.NOT_STARTED);
                RequirementProgressData hundred = createProgressData(true, 1.0, RequirementStatus.COMPLETED);

                assertEquals(55, roundDown.getProgressAsPercentage());
                assertEquals(56, roundUp.getProgressAsPercentage());
                assertEquals(0, zero.getProgressAsPercentage());
                assertEquals(100, hundred.getProgressAsPercentage());
        }

        @Test
        void hasProgressShouldBeTrueOnlyWhenProgressGreaterThanZero() {
                RequirementProgressData zero = createProgressData(false, 0.0, RequirementStatus.NOT_STARTED);
                RequirementProgressData smallPositive = createProgressData(false, 0.0001, RequirementStatus.IN_PROGRESS);

                assertFalse(zero.hasProgress());
                assertTrue(smallPositive.hasProgress());
        }

        @Test
        void getFormattedProgressShouldAppendPercentSymbol() {
                RequirementProgressData progress = createProgressData(false, 0.255, RequirementStatus.IN_PROGRESS);

                assertEquals("26%", progress.getFormattedProgress());
        }

        @Test
        void canBeCompletedShouldRequireReadyStatusAndNotCompleted() {
                RequirementProgressData ready = createProgressData(false, 0.75, RequirementStatus.READY_TO_COMPLETE);
                RequirementProgressData alreadyCompleted = createProgressData(true, 1.0, RequirementStatus.READY_TO_COMPLETE);
                RequirementProgressData wrongStatus = createProgressData(false, 0.75, RequirementStatus.IN_PROGRESS);

                assertTrue(ready.canBeCompleted());
                assertFalse(alreadyCompleted.canBeCompleted());
                assertFalse(wrongStatus.canBeCompleted());
        }
}
