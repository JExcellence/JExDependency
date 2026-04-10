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

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.NationInvite;
import com.raindropcentral.rdt.database.entity.NationInviteStatus;
import com.raindropcentral.rdt.database.entity.NationInviteType;
import com.raindropcentral.rdt.database.entity.RNation;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.service.NationActionStatus;
import com.raindropcentral.rdt.service.NationCreationProgressSnapshot;
import com.raindropcentral.rdt.service.NationInviteResponseStatus;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Shared initial-data, lookup, and translation helpers for nation views.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class TownNationViewSupport {

    static final String ENTRY_KEY = "nation_entry_key";
    static final String CONTRIBUTION_STATUS_KEY = "nation_contribution_status";
    static final String CONTRIBUTION_AMOUNT_KEY = "nation_contribution_amount";
    static final String CONTRIBUTION_COMPLETED_KEY = "nation_requirement_completed";
    static final String READY_TO_CREATE_KEY = "nation_ready";
    static final String DRAFT_NATION_NAME_KEY = "draft_nation_name";
    static final String RENAMED_NATION_NAME_KEY = "renamed_nation_name";
    static final String SELECTED_TOWN_UUIDS_KEY = "selected_town_uuids";

    private TownNationViewSupport() {
    }

    static @Nullable Map<String, Object> copyInitialData(final @NotNull Context context) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> rawMap)) {
            return null;
        }

        final Map<String, Object> copied = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                copied.put(key, entry.getValue());
            }
        }
        return copied;
    }

    static @NotNull Map<String, Object> mergeInitialData(
        final @NotNull Context context,
        final @NotNull Map<String, Object> extraData
    ) {
        final Map<String, Object> copiedData = copyInitialData(context);
        final Map<String, Object> mergedData = copiedData == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(copiedData);
        mergedData.putAll(extraData);
        return mergedData;
    }

    static @NotNull Map<String, Object> stripTransientData(final @NotNull Map<String, Object> data) {
        final Map<String, Object> sanitizedData = new LinkedHashMap<>(data);
        sanitizedData.remove(ENTRY_KEY);
        sanitizedData.remove(CONTRIBUTION_STATUS_KEY);
        sanitizedData.remove(CONTRIBUTION_AMOUNT_KEY);
        sanitizedData.remove(CONTRIBUTION_COMPLETED_KEY);
        sanitizedData.remove(READY_TO_CREATE_KEY);
        return sanitizedData;
    }

    static @Nullable RDT plugin(final @NotNull Context context) {
        final Map<String, Object> data = copyInitialData(context);
        return data != null && data.get("plugin") instanceof RDT plugin ? plugin : null;
    }

    static @Nullable RTown resolveTown(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        if (plugin == null || plugin.getTownRuntimeService() == null) {
            return null;
        }

        final Map<String, Object> data = copyInitialData(context);
        if (data == null || !(data.get("town_uuid") instanceof UUID townUuid)) {
            return null;
        }
        return plugin.getTownRuntimeService().getTown(townUuid);
    }

    static @Nullable RNation resolveNation(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        if (plugin == null || plugin.getTownRuntimeService() == null) {
            return null;
        }

        final Map<String, Object> data = copyInitialData(context);
        if (data != null && data.get("nation_uuid") instanceof UUID nationUuid) {
            return plugin.getTownRuntimeService().getNation(nationUuid);
        }

        final RTown town = resolveTown(context);
        return town == null ? null : plugin.getTownRuntimeService().getNationForTown(town);
    }

    static @Nullable RNation resolvePendingNation(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        final RTown town = resolveTown(context);
        return plugin == null || plugin.getTownRuntimeService() == null || town == null
            ? null
            : plugin.getTownRuntimeService().getPendingNationCreatedBy(town);
    }

    static @Nullable NationInvite resolvePendingInvite(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        final RTown town = resolveTown(context);
        return plugin == null || plugin.getTownRuntimeService() == null || town == null
            ? null
            : plugin.getTownRuntimeService().getPendingNationInviteFor(town);
    }

    static @Nullable NationCreationProgressSnapshot resolveCreationSnapshot(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        return plugin == null || plugin.getTownRuntimeService() == null
            ? null
            : plugin.getTownRuntimeService().getNationCreationProgress(context.getPlayer());
    }

    static @NotNull Map<String, Object> createRootNavigationData(
        final @NotNull Context context,
        final @NotNull RTown town
    ) {
        return mergeInitialData(
            context,
            Map.of(
                "plugin", plugin(context),
                "town_uuid", town.getTownUUID()
            )
        );
    }

    static @NotNull Map<String, Object> createNationNavigationData(
        final @NotNull Context context,
        final @NotNull RTown town,
        final @NotNull RNation nation
    ) {
        return mergeInitialData(
            context,
            Map.of(
                "plugin", plugin(context),
                "town_uuid", town.getTownUUID(),
                "nation_uuid", nation.getNationUuid()
            )
        );
    }

    static @NotNull List<UUID> readUuidList(final @NotNull Context context, final @NotNull String key) {
        final Map<String, Object> data = copyInitialData(context);
        if (data == null) {
            return List.of();
        }
        return readUuidList(data.get(key));
    }

    static @NotNull List<UUID> readUuidList(final @Nullable Object rawValue) {
        if (!(rawValue instanceof Collection<?> rawCollection)) {
            return List.of();
        }

        final LinkedHashSet<UUID> values = new LinkedHashSet<>();
        for (final Object rawEntry : rawCollection) {
            if (rawEntry instanceof UUID uuid) {
                values.add(uuid);
                continue;
            }
            if (!(rawEntry instanceof String serializedUuid)) {
                continue;
            }
            try {
                values.add(UUID.fromString(serializedUuid.trim()));
            } catch (final IllegalArgumentException ignored) {
            }
        }
        return List.copyOf(values);
    }

    static @NotNull String inviteStatusText(
        final @NotNull Player player,
        final @NotNull NationInviteStatus status
    ) {
        return plainText("town_nation_shared.invite_status." + status.name().toLowerCase(Locale.ROOT), player);
    }

    static @NotNull String inviteTypeText(
        final @NotNull Player player,
        final @NotNull NationInviteType type
    ) {
        return plainText("town_nation_shared.invite_type." + type.name().toLowerCase(Locale.ROOT), player);
    }

    static @NotNull String nationRoleText(final @NotNull Player player, final boolean capital) {
        return plainText("town_nation_shared.role." + (capital ? "capital" : "member"), player);
    }

    static @NotNull String toActionMessageKey(final @NotNull NationActionStatus status) {
        return switch (status) {
            case SUCCESS -> "success";
            case NO_PERMISSION -> "no_permission";
            case NOT_READY -> "not_ready";
            case INVALID_NAME -> "invalid_name";
            case NAME_TAKEN -> "name_taken";
            case ALREADY_IN_NATION -> "already_in_nation";
            case ALREADY_PENDING -> "already_pending";
            case LOCKED -> "locked";
            case NOT_ENOUGH_TOWNS -> "not_enough_towns";
            case INVALID_SELECTION -> "invalid_selection";
            case CAPITAL_REQUIRED -> "capital_required";
            case BELOW_MINIMUM -> "below_minimum";
            case INVALID_TARGET, FAILED -> "failed";
        };
    }

    static @NotNull String toInviteResponseMessageKey(final @NotNull NationInviteResponseStatus status) {
        return switch (status) {
            case ACCEPTED -> "accepted";
            case DECLINED -> "declined";
            case NO_PERMISSION -> "no_permission";
            case INVALID_TARGET, FAILED -> "failed";
        };
    }

    private static @NotNull String plainText(final @NotNull String key, final @NotNull Player player) {
        return PlainTextComponentSerializer.plainText().serialize(new I18n.Builder(key, player).build().component());
    }
}
