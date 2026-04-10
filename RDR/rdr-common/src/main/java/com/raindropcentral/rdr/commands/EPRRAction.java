/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdr.commands;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the available command actions for {@code /prr}.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public enum EPRRAction {
    ADMIN,
    INFO,
    SCOREBOARD,
    STORAGE,
    TRADE,
    TAXES;

    private static final Map<String, EPRRAction> ACTIONS_BY_LABEL = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(
            action -> action.name().toLowerCase(Locale.ROOT),
            Function.identity()
        ));
    private static final Set<String> RESERVED_SUBCOMMAND_LABELS = Stream.concat(
            ACTIONS_BY_LABEL.keySet().stream(),
            Stream.of("backup", "rollback")
        )
        .collect(Collectors.toUnmodifiableSet());

    /**
     * Resolves a first-argument command label to its matching action.
     *
     * @param rawLabel raw user-entered command label
     * @return matching action, or {@code null} when the label is blank or unsupported
     */
    public static @Nullable EPRRAction fromLabel(final @Nullable String rawLabel) {
        if (rawLabel == null || rawLabel.isBlank()) {
            return null;
        }
        return ACTIONS_BY_LABEL.get(rawLabel.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Returns whether the supplied label is reserved by any supported {@code /rr} subcommand.
     *
     * <p>This includes top-level actions such as {@code storage} as well as nested admin labels such
     * as {@code backup} and {@code rollback} so hotkey inputs can reject ambiguous names before they
     * are saved.</p>
     *
     * @param rawLabel raw player-entered label
     * @return {@code true} when the label is reserved by {@code /rr}
     */
    public static boolean isReservedSubcommandLabel(final @Nullable String rawLabel) {
        if (rawLabel == null || rawLabel.isBlank()) {
            return false;
        }
        return RESERVED_SUBCOMMAND_LABELS.contains(rawLabel.trim().toLowerCase(Locale.ROOT));
    }

}
