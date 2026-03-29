/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.core.service.central;

import com.raindropcentral.core.RCoreImpl;
import com.raindropcentral.core.service.central.cookie.DropletCookieDefinition;
import com.raindropcentral.core.service.central.cookie.DropletCookieDefinitions;
import com.raindropcentral.core.service.central.cookie.DropletCookieEffectType;
import com.raindropcentral.core.service.central.cookie.DropletCookieTargetType;
import com.raindropcentral.core.view.DropletClaimsView;
import com.raindropcentral.core.view.DropletJobSelectionView;
import com.raindropcentral.core.view.DropletSkillSelectionView;
import com.raindropcentral.rplatform.job.JobBridge;
import com.raindropcentral.rplatform.skill.SkillBridge;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles droplet claim loading, item delivery, and droplet-cookie redemption.
 */
public final class DropletClaimService {

    private static final long CLAIM_COMMAND_COOLDOWN_MILLIS = 5_000L;
    private static final byte COOKIE_VERSION = 2;

    private final RCoreImpl plugin;
    private final Logger logger;
    private final RCentralService centralService;
    private final NamespacedKey itemCodeKey;
    private final NamespacedKey itemTypeKey;
    private final NamespacedKey itemVersionKey;
    private final Set<String> inFlightClaims = ConcurrentHashMap.newKeySet();
    private final Map<UUID, List<ItemStack>> pendingRewards = new ConcurrentHashMap<>();
    private final Map<UUID, Long> claimCommandTimestamps = new ConcurrentHashMap<>();

    public DropletClaimService(final @NotNull RCoreImpl plugin) {
        this.plugin = plugin;
        this.logger = plugin.getPlugin().getLogger();
        this.centralService = plugin.getRCentralService();
        this.itemCodeKey = new NamespacedKey(plugin.getPlugin(), "droplet_item_code");
        this.itemTypeKey = new NamespacedKey(plugin.getPlugin(), "droplet_item_type");
        this.itemVersionKey = new NamespacedKey(plugin.getPlugin(), "droplet_item_version");
    }

    public void openClaimsMenu(final @NotNull Player player) {
        if (!this.isDropletStoreEnabled()) {
            this.send(player, "rcclaim.error.store_disabled");
            return;
        }
        if (!this.centralService.isConnected()) {
            this.send(player, "rcclaim.error.not_connected");
            return;
        }
        final long remainingCooldownMillis = this.getRemainingClaimCommandCooldownMillis(player.getUniqueId());
        if (remainingCooldownMillis > 0L) {
            this.send(player, "rcclaim.error.cooldown", Map.of(
                    "seconds", this.toRemainingCooldownSeconds(remainingCooldownMillis)
            ));
            return;
        }

        this.claimCommandTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
        this.send(player, "rcclaim.loading");
        this.centralService.getUnclaimedDropletPurchases(player.getUniqueId())
                .thenAccept(response -> this.runSync(() -> this.handleClaimsMenuResponse(player.getUniqueId(), response)))
                .exceptionally(throwable -> {
                    this.logger.log(Level.WARNING, "Failed to load droplet claims menu", throwable);
                    this.runSync(() -> {
                        final Player onlinePlayer = this.getOnlinePlayer(player.getUniqueId());
                        if (onlinePlayer != null) {
                            this.send(onlinePlayer, "rcclaim.error.api_unavailable", Map.of(
                                    "error", this.resolveThrowableMessage(throwable)
                            ));
                        }
                    });
                    return null;
                });
    }

    public @NotNull ClaimSupportStatus getSupportStatus(
            final @NotNull Player player,
            final @NotNull RCentralApiClient.DropletStorePurchaseData purchase
    ) {
        final DropletCookieDefinition definition = DropletCookieDefinitions.get(purchase.itemCodeOrBlank());
        if (definition == null) {
            return ClaimSupportStatus.UNSUPPORTED_ITEM;
        }
        if (!this.centralService.isDropletStoreRewardEnabled(definition.itemCode())) {
            return ClaimSupportStatus.ITEM_DISABLED;
        }
        if (definition.targetType() == DropletCookieTargetType.SKILL && this.getAvailableSkillDescriptors(player).isEmpty()) {
            return ClaimSupportStatus.NO_SKILLS_AVAILABLE;
        }
        if (definition.targetType() == DropletCookieTargetType.JOB && this.getAvailableJobDescriptors(player).isEmpty()) {
            return ClaimSupportStatus.NO_JOBS_AVAILABLE;
        }
        return ClaimSupportStatus.SUPPORTED;
    }

    public void handlePurchaseSelection(
            final @NotNull Player player,
            final @NotNull RCentralApiClient.DropletStorePurchaseData purchase
    ) {
        if (!this.isDropletStoreEnabled()) {
            this.send(player, "rcclaim.error.store_disabled");
            return;
        }
        final ClaimSupportStatus supportStatus = this.getSupportStatus(player, purchase);
        switch (supportStatus) {
            case UNSUPPORTED_ITEM -> {
                this.send(player, "rcclaim.error.unsupported_item", Map.of("item_code", purchase.itemCodeOrBlank()));
                return;
            }
            case ITEM_DISABLED -> {
                this.send(player, "rcclaim.error.item_disabled", Map.of(
                        "item_name", purchase.itemNameOrCode(),
                        "item_code", purchase.itemCodeOrBlank()
                ));
                return;
            }
            case NO_SKILLS_AVAILABLE -> {
                this.send(player, "rcclaim.error.no_supported_skills");
                return;
            }
            case NO_JOBS_AVAILABLE -> {
                this.send(player, "rcclaim.error.no_supported_jobs");
                return;
            }
            case SUPPORTED -> {
            }
        }
        if (player.getInventory().firstEmpty() < 0) {
            this.send(player, "rcclaim.error.inventory_full");
            return;
        }

        final String claimKey = player.getUniqueId() + ":" + purchase.id();
        if (!this.inFlightClaims.add(claimKey)) {
            this.send(player, "rcclaim.error.claim_in_progress");
            return;
        }

        this.send(player, "rcclaim.claiming_item", Map.of("item_name", purchase.itemNameOrCode()));
        this.centralService.claimDropletPurchase(player.getUniqueId(), purchase.id())
                .thenAccept(response -> this.runSync(() -> this.handleClaimResponse(player.getUniqueId(), purchase, response)))
                .exceptionally(throwable -> {
                    this.logger.log(Level.WARNING, "Failed to claim purchase " + purchase.id(), throwable);
                    this.runSync(() -> {
                        final Player onlinePlayer = this.getOnlinePlayer(player.getUniqueId());
                        if (onlinePlayer != null) {
                            this.send(onlinePlayer, "rcclaim.error.claim_failed", Map.of(
                                    "error", this.resolveThrowableMessage(throwable)
                            ));
                        }
                    });
                    return null;
                })
                .whenComplete((ignored, throwable) -> this.inFlightClaims.remove(claimKey));
    }

    public void handleCookieUse(final @NotNull Player player, final @NotNull ItemStack cookie) {
        final DropletCookieDefinition definition = this.getCookieDefinition(cookie);
        if (definition == null) {
            this.send(player, "rcclaim.error.unsupported_item", Map.of("item_code", this.readItemCode(cookie)));
            return;
        }

        if (definition.targetType() == DropletCookieTargetType.SKILL) {
            this.openSkillSelectionMenu(player, cookie, definition);
            return;
        }
        if (definition.targetType() == DropletCookieTargetType.JOB) {
            this.openJobSelectionMenu(player, cookie, definition);
            return;
        }

        this.handleDirectCookieUse(player, cookie, definition);
    }

    public void openSkillSelectionMenu(
            final @NotNull Player player,
            final @NotNull ItemStack cookie,
            final @NotNull DropletCookieDefinition definition
    ) {
        final List<SkillBridge.SkillDescriptor> skills = this.getAvailableSkillDescriptors(player);
        if (skills.isEmpty()) {
            this.send(player, "rcclaim.error.no_supported_skills");
            return;
        }
        if (this.plugin.getViewFrame() == null) {
            this.send(player, "rcclaim.error.menu_unavailable");
            return;
        }

        this.plugin.getViewFrame().open(
                DropletSkillSelectionView.class,
                player,
                Map.of("plugin", this.plugin, "cookie", cookie.clone(), "skills", List.copyOf(skills), "definition", definition)
        );
    }

    public void openJobSelectionMenu(
            final @NotNull Player player,
            final @NotNull ItemStack cookie,
            final @NotNull DropletCookieDefinition definition
    ) {
        final List<JobBridge.JobDescriptor> jobs = this.getAvailableJobDescriptors(player);
        if (jobs.isEmpty()) {
            this.send(player, "rcclaim.error.no_supported_jobs");
            return;
        }
        if (this.plugin.getViewFrame() == null) {
            this.send(player, "rcclaim.error.menu_unavailable");
            return;
        }

        this.plugin.getViewFrame().open(
                DropletJobSelectionView.class,
                player,
                Map.of("plugin", this.plugin, "cookie", cookie.clone(), "jobs", List.copyOf(jobs), "definition", definition)
        );
    }

    public void handleSkillSelection(
            final @NotNull Player player,
            final @NotNull SkillBridge.SkillDescriptor skillDescriptor,
            final @NotNull ItemStack cookie,
            final @NotNull DropletCookieDefinition definition
    ) {
        final SkillBridge bridge = SkillBridge.getBridge(skillDescriptor.integrationId());
        if (bridge == null || !bridge.isAvailable()) {
            this.send(player, "rcclaim.error.skill_grant_failed", Map.of("skill_name", skillDescriptor.displayName()));
            return;
        }
        if (!this.removeOneMatchingCookie(player, cookie)) {
            this.send(player, "rcclaim.error.cookie_missing");
            player.closeInventory();
            return;
        }

        switch (definition.effectType()) {
            case SKILL_LEVEL -> {
                if (!bridge.addSkillLevels(player, skillDescriptor.skillId(), 1)) {
                    this.restoreCookie(player.getUniqueId(), cookie.clone());
                    this.send(player, "rcclaim.error.skill_grant_failed", Map.of("skill_name", skillDescriptor.displayName()));
                    return;
                }

                player.closeInventory();
                this.send(player, "rcclaim.success.skill_granted", Map.of(
                        "skill_name", skillDescriptor.displayName(),
                        "plugin_name", skillDescriptor.pluginName()
                ));
            }
            case SKILL_XP_RATE -> {
                player.closeInventory();
                this.plugin.getActiveCookieBoostService()
                        .activateBoost(player, definition, skillDescriptor.integrationId(), skillDescriptor.skillId())
                        .thenAccept(result -> this.runSync(() ->
                                this.handleSkillBoostActivationResult(player.getUniqueId(), cookie, skillDescriptor, definition, result)
                        ));
            }
            default -> {
                this.restoreCookie(player.getUniqueId(), cookie.clone());
                this.send(player, "rcclaim.error.unsupported_item", Map.of("item_code", definition.itemCode()));
            }
        }
    }

    public void handleJobSelection(
            final @NotNull Player player,
            final @NotNull JobBridge.JobDescriptor jobDescriptor,
            final @NotNull ItemStack cookie,
            final @NotNull DropletCookieDefinition definition
    ) {
        final JobBridge bridge = JobBridge.getBridge(jobDescriptor.integrationId());
        if (bridge == null || !bridge.isAvailable()) {
            this.send(player, "rcclaim.error.job_grant_failed", Map.of("job_name", jobDescriptor.displayName()));
            return;
        }
        if (!this.removeOneMatchingCookie(player, cookie)) {
            this.send(player, "rcclaim.error.cookie_missing");
            player.closeInventory();
            return;
        }

        switch (definition.effectType()) {
            case JOB_LEVEL -> {
                if (!bridge.addJobLevels(player, jobDescriptor.jobId(), 1)) {
                    this.restoreCookie(player.getUniqueId(), cookie.clone());
                    this.send(player, "rcclaim.error.job_grant_failed", Map.of("job_name", jobDescriptor.displayName()));
                    return;
                }

                player.closeInventory();
                this.send(player, "rcclaim.success.job_granted", Map.of(
                        "job_name", jobDescriptor.displayName(),
                        "plugin_name", jobDescriptor.pluginName()
                ));
            }
            case JOB_XP_RATE, JOB_VAULT_RATE -> {
                player.closeInventory();
                this.plugin.getActiveCookieBoostService()
                        .activateBoost(player, definition, jobDescriptor.integrationId(), jobDescriptor.jobId())
                        .thenAccept(result -> this.runSync(() ->
                                this.handleJobBoostActivationResult(player.getUniqueId(), cookie, jobDescriptor, definition, result)
                        ));
            }
            default -> {
                this.restoreCookie(player.getUniqueId(), cookie.clone());
                this.send(player, "rcclaim.error.unsupported_item", Map.of("item_code", definition.itemCode()));
            }
        }
    }

    public void deliverPendingRewards(final @NotNull Player player) {
        final List<ItemStack> rewards = this.pendingRewards.remove(player.getUniqueId());
        if (rewards == null || rewards.isEmpty()) {
            return;
        }

        for (final ItemStack reward : rewards) {
            this.giveOrDrop(player, reward);
        }
        this.send(player, "rcclaim.success.pending_delivery", Map.of("count", rewards.size()));
    }

    public @NotNull List<SkillBridge.SkillDescriptor> getAvailableSkillDescriptors(final @NotNull Player player) {
        final Map<String, SkillBridge.SkillDescriptor> descriptors = new LinkedHashMap<>();
        for (final SkillBridge bridge : SkillBridge.getAvailableBridges()) {
            for (final SkillBridge.SkillDescriptor descriptor : bridge.getAvailableSkills(player)) {
                final String key = (descriptor.integrationId() + ":" + descriptor.skillId())
                        .toLowerCase()
                        .replace("-", "")
                        .replace("_", "")
                        .replace(" ", "");
                descriptors.putIfAbsent(key, descriptor);
            }
        }

        return descriptors.values().stream()
                .sorted(Comparator
                        .comparing(SkillBridge.SkillDescriptor::pluginName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(SkillBridge.SkillDescriptor::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public @NotNull List<JobBridge.JobDescriptor> getAvailableJobDescriptors(final @NotNull Player player) {
        final Map<String, JobBridge.JobDescriptor> descriptors = new LinkedHashMap<>();
        for (final JobBridge bridge : JobBridge.getAvailableBridges()) {
            for (final JobBridge.JobDescriptor descriptor : bridge.getAvailableJobs(player)) {
                final String key = (descriptor.integrationId() + ":" + descriptor.jobId())
                        .toLowerCase()
                        .replace("-", "")
                        .replace("_", "")
                        .replace(" ", "");
                descriptors.putIfAbsent(key, descriptor);
            }
        }

        return descriptors.values().stream()
                .sorted(Comparator
                        .comparing(JobBridge.JobDescriptor::pluginName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(JobBridge.JobDescriptor::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public double getCurrentSkillLevel(
            final @NotNull Player player,
            final @NotNull SkillBridge.SkillDescriptor descriptor
    ) {
        final SkillBridge bridge = SkillBridge.getBridge(descriptor.integrationId());
        return bridge == null ? 0.0D : bridge.getSkillLevel(player, descriptor.skillId());
    }

    public double getCurrentJobLevel(
            final @NotNull Player player,
            final @NotNull JobBridge.JobDescriptor descriptor
    ) {
        final JobBridge bridge = JobBridge.getBridge(descriptor.integrationId());
        return bridge == null ? 0.0D : bridge.getJobLevel(player, descriptor.jobId());
    }

    public @Nullable DropletCookieDefinition getCookieDefinition(final @Nullable ItemStack item) {
        return DropletCookieDefinitions.get(this.readItemCode(item));
    }

    public boolean isDropletStoreEnabled() {
        return this.centralService.isDropletStoreEnabled();
    }

    public boolean isDropletCookie(final @Nullable ItemStack item) {
        if (item == null || item.getType() != Material.COOKIE || !item.hasItemMeta()) {
            return false;
        }

        final PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        if (!data.has(this.itemCodeKey, PersistentDataType.STRING) || !data.has(this.itemVersionKey, PersistentDataType.BYTE)) {
            return false;
        }

        return this.getCookieDefinition(item) != null;
    }

    private void handleClaimsMenuResponse(
            final @NotNull UUID playerId,
            final @NotNull RCentralApiClient.ParsedApiResponse<List<RCentralApiClient.DropletStorePurchaseData>> response
    ) {
        final Player player = this.getOnlinePlayer(playerId);
        if (player == null) {
            return;
        }
        if (response.statusCode() == 404) {
            this.send(player, "rcclaim.error.not_registered");
            return;
        }
        if (!response.isSuccess()) {
            this.send(player, "rcclaim.error.api_unavailable", Map.of("error", this.resolveApiError(response)));
            return;
        }

        final List<RCentralApiClient.DropletStorePurchaseData> purchases = response.data() == null ? List.of() : response.data();
        if (purchases.isEmpty()) {
            this.send(player, "rcclaim.error.no_unclaimed");
            return;
        }
        if (this.plugin.getViewFrame() == null) {
            this.send(player, "rcclaim.error.menu_unavailable");
            return;
        }

        this.plugin.getViewFrame().open(
                DropletClaimsView.class,
                player,
                Map.of("plugin", this.plugin, "purchases", List.copyOf(purchases))
        );
    }

    private void handleClaimResponse(
            final @NotNull UUID playerId,
            final @NotNull RCentralApiClient.DropletStorePurchaseData requestedPurchase,
            final @NotNull RCentralApiClient.ParsedApiResponse<RCentralApiClient.DropletStorePurchaseData> response
    ) {
        if (!response.isSuccess()) {
            final Player player = this.getOnlinePlayer(playerId);
            if (player == null) {
                return;
            }
            if (response.statusCode() == 409) {
                this.send(player, "rcclaim.error.already_claimed");
                return;
            }
            if (response.statusCode() == 404) {
                this.send(player, "rcclaim.error.purchase_unavailable");
                return;
            }

            this.send(player, "rcclaim.error.claim_failed", Map.of("error", this.resolveApiError(response)));
            return;
        }

        final RCentralApiClient.DropletStorePurchaseData claimedPurchase = response.data() == null ? requestedPurchase : response.data();
        final ItemStack rewardCookie = this.createRewardCookie(claimedPurchase);
        final Player player = this.getOnlinePlayer(playerId);
        if (player == null) {
            this.queuePendingReward(playerId, rewardCookie.clone());
            return;
        }

        player.closeInventory();
        this.giveOrDrop(player, rewardCookie);
        this.send(player, this.resolveClaimedMessageKey(claimedPurchase), Map.of("item_name", claimedPurchase.itemNameOrCode()));
    }

    private @NotNull ItemStack createRewardCookie(final @NotNull RCentralApiClient.DropletStorePurchaseData purchase) {
        final DropletCookieDefinition definition = DropletCookieDefinitions.get(purchase.itemCodeOrBlank());
        final ItemStack itemStack = new ItemStack(Material.COOKIE);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(purchase.itemNameOrCode()));
        final PersistentDataContainer data = itemMeta.getPersistentDataContainer();
        data.set(this.itemCodeKey, PersistentDataType.STRING, purchase.itemCodeOrBlank());
        data.set(this.itemTypeKey, PersistentDataType.STRING, definition == null ? purchase.itemCodeOrBlank() : definition.itemType());
        data.set(this.itemVersionKey, PersistentDataType.BYTE, COOKIE_VERSION);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private void handleDirectCookieUse(
            final @NotNull Player player,
            final @NotNull ItemStack cookie,
            final @NotNull DropletCookieDefinition definition
    ) {
        if (!this.removeOneMatchingCookie(player, cookie)) {
            this.send(player, "rcclaim.error.cookie_missing");
            return;
        }

        if (definition.effectType() != DropletCookieEffectType.DOUBLE_DROP_RATE) {
            this.restoreCookie(player.getUniqueId(), cookie.clone());
            this.send(player, "rcclaim.error.unsupported_item", Map.of("item_code", definition.itemCode()));
            return;
        }

        this.plugin.getActiveCookieBoostService()
                .activateBoost(player, definition, null, null)
                .thenAccept(result -> this.runSync(() -> this.handleDoubleDropActivationResult(player.getUniqueId(), cookie, definition, result)));
    }

    private void handleSkillBoostActivationResult(
            final @NotNull UUID playerId,
            final @NotNull ItemStack cookie,
            final @NotNull SkillBridge.SkillDescriptor skillDescriptor,
            final @NotNull DropletCookieDefinition definition,
            final @NotNull com.raindropcentral.core.service.central.cookie.ActiveCookieBoostService.BoostActivationResult result
    ) {
        final Player player = this.getOnlinePlayer(playerId);
        if (!result.success()) {
            this.restoreCookie(playerId, cookie.clone());
            if (player != null) {
                this.send(player, "rcclaim.error.boost_activation_failed", Map.of("item_name", this.resolveDisplayName(cookie)));
            }
            return;
        }

        if (player == null) {
            return;
        }

        final String key = result.replaced()
                ? "rcclaim.success.skill_xp_boost_refreshed"
                : "rcclaim.success.skill_xp_boost_applied";
        this.send(player, key, Map.of(
                "skill_name", skillDescriptor.displayName(),
                "plugin_name", skillDescriptor.pluginName(),
                "rate_percent", definition.ratePercent(),
                "duration_minutes", definition.durationMinutes()
        ));
    }

    private void handleJobBoostActivationResult(
            final @NotNull UUID playerId,
            final @NotNull ItemStack cookie,
            final @NotNull JobBridge.JobDescriptor jobDescriptor,
            final @NotNull DropletCookieDefinition definition,
            final @NotNull com.raindropcentral.core.service.central.cookie.ActiveCookieBoostService.BoostActivationResult result
    ) {
        final Player player = this.getOnlinePlayer(playerId);
        if (!result.success()) {
            this.restoreCookie(playerId, cookie.clone());
            if (player != null) {
                this.send(player, "rcclaim.error.boost_activation_failed", Map.of("item_name", this.resolveDisplayName(cookie)));
            }
            return;
        }

        if (player == null) {
            return;
        }

        final String key;
        if (definition.effectType() == DropletCookieEffectType.JOB_VAULT_RATE) {
            key = result.replaced()
                    ? "rcclaim.success.job_vault_boost_refreshed"
                    : "rcclaim.success.job_vault_boost_applied";
        } else {
            key = result.replaced()
                    ? "rcclaim.success.job_xp_boost_refreshed"
                    : "rcclaim.success.job_xp_boost_applied";
        }

        this.send(player, key, Map.of(
                "job_name", jobDescriptor.displayName(),
                "plugin_name", jobDescriptor.pluginName(),
                "rate_percent", definition.ratePercent(),
                "duration_minutes", definition.durationMinutes()
        ));
    }

    private void handleDoubleDropActivationResult(
            final @NotNull UUID playerId,
            final @NotNull ItemStack cookie,
            final @NotNull DropletCookieDefinition definition,
            final @NotNull com.raindropcentral.core.service.central.cookie.ActiveCookieBoostService.BoostActivationResult result
    ) {
        final Player player = this.getOnlinePlayer(playerId);
        if (!result.success()) {
            this.restoreCookie(playerId, cookie.clone());
            if (player != null) {
                this.send(player, "rcclaim.error.boost_activation_failed", Map.of("item_name", this.resolveDisplayName(cookie)));
            }
            return;
        }

        if (player == null) {
            return;
        }

        final String key = result.replaced()
                ? "rcclaim.success.double_drop_boost_refreshed"
                : "rcclaim.success.double_drop_boost_applied";
        this.send(player, key, Map.of("duration_minutes", definition.durationMinutes()));
    }

    private void restoreCookie(final @NotNull UUID playerId, final @NotNull ItemStack cookie) {
        final Player player = this.getOnlinePlayer(playerId);
        if (player != null) {
            this.giveOrDrop(player, cookie);
            return;
        }

        this.queuePendingReward(playerId, cookie);
    }

    private void queuePendingReward(final @NotNull UUID playerId, final @NotNull ItemStack reward) {
        this.pendingRewards.compute(playerId, (ignored, existing) -> {
            final List<ItemStack> rewards = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            rewards.add(reward);
            return rewards;
        });
    }

    private void giveOrDrop(final @NotNull Player player, final @NotNull ItemStack reward) {
        final Map<Integer, ItemStack> leftovers = player.getInventory().addItem(reward);
        for (final ItemStack leftover : leftovers.values()) {
            final Item droppedItem = player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            droppedItem.setOwner(player.getUniqueId());
        }
    }

    private boolean removeOneMatchingCookie(final @NotNull Player player, final @NotNull ItemStack referenceCookie) {
        final PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            final ItemStack candidate = inventory.getItem(slot);
            if (!this.matchesCookie(candidate, referenceCookie)) {
                continue;
            }

            if (candidate.getAmount() > 1) {
                candidate.setAmount(candidate.getAmount() - 1);
            } else {
                inventory.setItem(slot, null);
            }
            return true;
        }
        return false;
    }

    private boolean matchesCookie(final @Nullable ItemStack candidate, final @NotNull ItemStack reference) {
        if (!this.isDropletCookie(candidate) || !this.isDropletCookie(reference)) {
            return false;
        }

        return this.readItemCode(candidate).equals(this.readItemCode(reference))
                && this.readItemType(candidate).equals(this.readItemType(reference))
                && this.resolveDisplayName(candidate).equals(this.resolveDisplayName(reference));
    }

    private @NotNull String readItemCode(final @Nullable ItemStack item) {
        return this.readTag(item, this.itemCodeKey);
    }

    private @NotNull String readItemType(final @Nullable ItemStack item) {
        return this.readTag(item, this.itemTypeKey);
    }

    private @NotNull String readTag(final @Nullable ItemStack item, final @NotNull NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) {
            return "";
        }
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(key, PersistentDataType.STRING, "");
    }

    private @NotNull String resolveDisplayName(final @Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "";
        }

        final Component displayName = item.getItemMeta().displayName();
        return displayName == null ? "" : PlainTextComponentSerializer.plainText().serialize(displayName);
    }

    private @NotNull String resolveClaimedMessageKey(final @NotNull RCentralApiClient.DropletStorePurchaseData purchase) {
        final DropletCookieDefinition definition = DropletCookieDefinitions.get(purchase.itemCodeOrBlank());
        if (definition == null) {
            return "rcclaim.success.claimed_activate";
        }
        return switch (definition.targetType()) {
            case SKILL -> "rcclaim.success.claimed_skill_targeted";
            case JOB -> "rcclaim.success.claimed_job_targeted";
            case NONE -> "rcclaim.success.claimed_activate";
        };
    }

    private @Nullable Player getOnlinePlayer(final @NotNull UUID playerId) {
        final Player player = Bukkit.getPlayer(playerId);
        return player != null && player.isOnline() ? player : null;
    }

    private void runSync(final @NotNull Runnable runnable) {
        this.plugin.getPlatform().getScheduler().runSync(runnable);
    }

    private void send(final @NotNull Player player, final @NotNull String key) {
        new I18n.Builder(key, player).includePrefix().build().sendMessage();
    }

    private void send(final @NotNull Player player, final @NotNull String key, final @NotNull Map<String, Object> placeholders) {
        new I18n.Builder(key, player).includePrefix().withPlaceholders(placeholders).build().sendMessage();
    }

    private @NotNull String resolveApiError(final @NotNull RCentralApiClient.ParsedApiResponse<?> response) {
        if (response.message() != null && !response.message().isBlank()) {
            return response.message();
        }
        if (response.error() != null && !response.error().isBlank()) {
            return response.error();
        }
        return "Status " + response.statusCode();
    }

    private @NotNull String resolveThrowableMessage(final @NotNull Throwable throwable) {
        final String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private long getRemainingClaimCommandCooldownMillis(final @NotNull UUID playerId) {
        final Long lastInvocation = this.claimCommandTimestamps.get(playerId);
        if (lastInvocation == null) {
            return 0L;
        }

        final long elapsedMillis = System.currentTimeMillis() - lastInvocation;
        return Math.max(0L, CLAIM_COMMAND_COOLDOWN_MILLIS - elapsedMillis);
    }

    private long toRemainingCooldownSeconds(final long remainingCooldownMillis) {
        return Math.max(1L, (remainingCooldownMillis + 999L) / 1000L);
    }

    public enum ClaimSupportStatus {
        SUPPORTED,
        ITEM_DISABLED,
        UNSUPPORTED_ITEM,
        NO_SKILLS_AVAILABLE,
        NO_JOBS_AVAILABLE
    }
}
