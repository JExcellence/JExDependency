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

package com.raindropcentral.rplatform.requirement.config;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.impl.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for creating AbstractRequirement instances.
 *
 * <p>Provides a clean API for constructing requirements without manual JSON manipulation.
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * AbstractRequirement req = RequirementBuilder.item()
 *     .items(List.of(new ItemStack(Material.DIAMOND, 10)))
 *     .consumeOnComplete(true)
 *     .build();
 * }</pre>
 */
public final class RequirementBuilder {

    private RequirementBuilder() {}

    // ==================== Factory Methods ====================

    /**
     * Executes item.
     */
    public static ItemBuilder item() { return new ItemBuilder(); }
    /**
     * Executes currency.
     */
    public static CurrencyBuilder currency() { return new CurrencyBuilder(); }
    /**
     * Executes experience.
     */
    public static ExperienceBuilder experience() { return new ExperienceBuilder(); }
    /**
     * Executes permission.
     */
    public static PermissionBuilder permission() { return new PermissionBuilder(); }
    /**
     * Executes location.
     */
    public static LocationBuilder location() { return new LocationBuilder(); }
    /**
     * Executes playtime.
     */
    public static PlaytimeBuilder playtime() { return new PlaytimeBuilder(); }
    /**
     * Executes composite.
     */
    public static CompositeBuilder composite() { return new CompositeBuilder(); }
    /**
     * Executes choice.
     */
    public static ChoiceBuilder choice() { return new ChoiceBuilder(); }
    /**
     * Executes timed.
     */
    public static TimedBuilder timed() { return new TimedBuilder(); }
    /**
     * Executes plugin.
     */
    public static PluginBuilder plugin() { return new PluginBuilder(); }

    // ==================== Item Builder ====================

    /**
     * Represents the ItemBuilder API type.
     */
    public static final class ItemBuilder {
        private List<ItemStack> items = new ArrayList<>();
        private boolean consumeOnComplete = true;
        private boolean exactMatch = true;
        private String description;

        /**
         * Executes items.
         */
        public ItemBuilder items(@NotNull List<ItemStack> items) {
            this.items = new ArrayList<>(items);
            return this;
        }

        /**
         * Executes addItem.
         */
        public ItemBuilder addItem(@NotNull ItemStack item) {
            this.items.add(item);
            return this;
        }

        /**
         * Executes consumeOnComplete.
         */
        public ItemBuilder consumeOnComplete(boolean consume) {
            this.consumeOnComplete = consume;
            return this;
        }

        /**
         * Executes exactMatch.
         */
        public ItemBuilder exactMatch(boolean exact) {
            this.exactMatch = exact;
            return this;
        }

        /**
         * Executes description.
         */
        public ItemBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        /**
         * Executes build.
         */
        public ItemRequirement build() {
            if (items.isEmpty()) {
                throw new IllegalStateException("At least one item is required");
            }
            return new ItemRequirement(items, null, consumeOnComplete, description, exactMatch);
        }
    }

    // ==================== Currency Builder ====================

    /**
     * Represents the CurrencyBuilder API type.
     */
    public static final class CurrencyBuilder {
        private String currency;
        private double amount;
        private boolean consumable = false;

        /**
         * Executes currency.
         */
        public CurrencyBuilder currency(@NotNull String currency) {
            this.currency = currency;
            return this;
        }

        /**
         * Executes amount.
         */
        public CurrencyBuilder amount(double amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Executes consumable.
         */
        public CurrencyBuilder consumable(boolean consumable) {
            this.consumable = consumable;
            return this;
        }

        /**
         * Executes build.
         */
        public CurrencyRequirement build() {
            if (currency == null || currency.isEmpty()) {
                throw new IllegalStateException("Currency ID is required");
            }
            if (amount <= 0) {
                throw new IllegalStateException("Amount must be positive");
            }
            return new CurrencyRequirement(currency, amount, consumable);
        }
    }

    // ==================== Experience Builder ====================

    /**
     * Represents the ExperienceBuilder API type.
     */
    public static final class ExperienceBuilder {
        private int level = 1;
        private ExperienceLevelRequirement.ExperienceType type = ExperienceLevelRequirement.ExperienceType.LEVEL;
        private boolean consumeOnComplete = true;
        private String description;

        /**
         * Executes level.
         */
        public ExperienceBuilder level(int level) {
            this.level = level;
            return this;
        }

        /**
         * Executes type.
         */
        public ExperienceBuilder type(@NotNull ExperienceLevelRequirement.ExperienceType type) {
            this.type = type;
            return this;
        }

        /**
         * Executes points.
         */
        public ExperienceBuilder points() {
            this.type = ExperienceLevelRequirement.ExperienceType.POINTS;
            return this;
        }

        /**
         * Executes consumeOnComplete.
         */
        public ExperienceBuilder consumeOnComplete(boolean consume) {
            this.consumeOnComplete = consume;
            return this;
        }

        /**
         * Executes description.
         */
        public ExperienceBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        /**
         * Executes build.
         */
        public ExperienceLevelRequirement build() {
            return new ExperienceLevelRequirement(level, type, consumeOnComplete, description);
        }
    }

    // ==================== Permission Builder ====================

    /**
     * Represents the PermissionBuilder API type.
     */
    public static final class PermissionBuilder {
        private final List<String> permissions = new ArrayList<>();
        private PermissionRequirement.PermissionMode mode = PermissionRequirement.PermissionMode.ALL;
        private int minimumRequired = 1;
        private boolean checkNegated = false;
        private String description;

        /**
         * Executes permission.
         */
        public PermissionBuilder permission(@NotNull String permission) {
            this.permissions.add(permission);
            return this;
        }

        /**
         * Executes permissions.
         */
        public PermissionBuilder permissions(@NotNull List<String> permissions) {
            this.permissions.addAll(permissions);
            return this;
        }

        /**
         * Executes mode.
         */
        public PermissionBuilder mode(@NotNull PermissionRequirement.PermissionMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Executes any.
         */
        public PermissionBuilder any() {
            this.mode = PermissionRequirement.PermissionMode.ANY;
            return this;
        }

        /**
         * Executes minimum.
         */
        public PermissionBuilder minimum(int min) {
            this.mode = PermissionRequirement.PermissionMode.MINIMUM;
            this.minimumRequired = min;
            return this;
        }

        /**
         * Executes negated.
         */
        public PermissionBuilder negated(boolean negated) {
            this.checkNegated = negated;
            return this;
        }

        /**
         * Executes description.
         */
        public PermissionBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        /**
         * Executes build.
         */
        public PermissionRequirement build() {
            if (permissions.isEmpty()) {
                throw new IllegalStateException("At least one permission is required");
            }
            return new PermissionRequirement(permissions, mode, minimumRequired, description, checkNegated);
        }
    }

    // ==================== Location Builder ====================

    /**
     * Represents the LocationBuilder API type.
     */
    public static final class LocationBuilder {
        private String world;
        private String region;
        private LocationRequirement.Coordinates coordinates;
        private double distance = 0;
        private String description;

        /**
         * Executes world.
         */
        public LocationBuilder world(@NotNull String world) {
            this.world = world;
            return this;
        }

        /**
         * Executes region.
         */
        public LocationBuilder region(@NotNull String region) {
            this.region = region;
            return this;
        }

        /**
         * Executes coordinates.
         */
        public LocationBuilder coordinates(double x, double y, double z) {
            this.coordinates = new LocationRequirement.Coordinates(x, y, z);
            return this;
        }

        /**
         * Executes distance.
         */
        public LocationBuilder distance(double distance) {
            this.distance = distance;
            return this;
        }

        /**
         * Executes description.
         */
        public LocationBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        /**
         * Executes build.
         */
        public LocationRequirement build() {
            return new LocationRequirement(world, region, coordinates, distance, description);
        }
    }

    // ==================== Playtime Builder ====================

    /**
     * Represents the PlaytimeBuilder API type.
     */
    public static final class PlaytimeBuilder {
        private long seconds = 0;
        private final Map<String, Long> worldRequirements = new HashMap<>();
        private boolean useTotalPlaytime = true;
        private String description;

        /**
         * Executes seconds.
         */
        public PlaytimeBuilder seconds(long seconds) {
            this.seconds = seconds;
            return this;
        }

        /**
         * Executes minutes.
         */
        public PlaytimeBuilder minutes(long minutes) {
            this.seconds = minutes * 60;
            return this;
        }

        /**
         * Executes hours.
         */
        public PlaytimeBuilder hours(long hours) {
            this.seconds = hours * 3600;
            return this;
        }

        /**
         * Executes worldPlaytime.
         */
        public PlaytimeBuilder worldPlaytime(@NotNull String world, long seconds) {
            this.worldRequirements.put(world, seconds);
            this.useTotalPlaytime = false;
            return this;
        }

        /**
         * Executes description.
         */
        public PlaytimeBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        /**
         * Executes build.
         */
        public PlaytimeRequirement build() {
            return new PlaytimeRequirement(seconds, worldRequirements.isEmpty() ? null : worldRequirements, useTotalPlaytime, description);
        }
    }

    // ==================== Composite Builder ====================

    /**
     * Represents the CompositeBuilder API type.
     */
    public static final class CompositeBuilder {
        private final List<AbstractRequirement> requirements = new ArrayList<>();
        private CompositeRequirement.Operator operator = CompositeRequirement.Operator.AND;
        private int minimumRequired = 1;
        private Integer maximumRequired;
        private boolean allowPartialProgress = true;
        private String description;

        /**
         * Executes add.
         */
        public CompositeBuilder add(@NotNull AbstractRequirement requirement) {
            this.requirements.add(requirement);
            return this;
        }

        /**
         * Executes addAll.
         */
        public CompositeBuilder addAll(@NotNull List<AbstractRequirement> requirements) {
            this.requirements.addAll(requirements);
            return this;
        }

        /**
         * Executes and.
         */
        public CompositeBuilder and() {
            this.operator = CompositeRequirement.Operator.AND;
            return this;
        }

        /**
         * Executes or.
         */
        public CompositeBuilder or() {
            this.operator = CompositeRequirement.Operator.OR;
            return this;
        }

        /**
         * Executes minimum.
         */
        public CompositeBuilder minimum(int min) {
            this.operator = CompositeRequirement.Operator.MINIMUM;
            this.minimumRequired = min;
            return this;
        }

        /**
         * Executes maximum.
         */
        public CompositeBuilder maximum(int max) {
            this.maximumRequired = max;
            return this;
        }

        /**
         * Executes allowPartialProgress.
         */
        public CompositeBuilder allowPartialProgress(boolean allow) {
            this.allowPartialProgress = allow;
            return this;
        }

        /**
         * Executes description.
         */
        public CompositeBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        /**
         * Executes build.
         */
        public CompositeRequirement build() {
            if (requirements.isEmpty()) {
                throw new IllegalStateException("At least one requirement is needed");
            }
            return new CompositeRequirement(requirements, operator, minimumRequired, maximumRequired, description, allowPartialProgress);
        }
    }

    // ==================== Choice Builder ====================

    /**
     * Represents the ChoiceBuilder API type.
     */
    public static final class ChoiceBuilder {
        private final List<AbstractRequirement> choices = new ArrayList<>();
        private int minimumRequired = 1;
        private Integer maximumRequired;
        private boolean allowPartialProgress = true;
        private boolean mutuallyExclusive = false;
        private boolean allowChoiceChange = true;
        private String description;

        /**
         * Executes add.
         */
        public ChoiceBuilder add(@NotNull AbstractRequirement choice) {
            this.choices.add(choice);
            return this;
        }

        /**
         * Executes addAll.
         */
        public ChoiceBuilder addAll(@NotNull List<AbstractRequirement> choices) {
            this.choices.addAll(choices);
            return this;
        }

        /**
         * Executes minimumRequired.
         */
        public ChoiceBuilder minimumRequired(int min) {
            this.minimumRequired = min;
            return this;
        }

        /**
         * Executes maximumRequired.
         */
        public ChoiceBuilder maximumRequired(int max) {
            this.maximumRequired = max;
            return this;
        }

        /**
         * Executes mutuallyExclusive.
         */
        public ChoiceBuilder mutuallyExclusive(boolean exclusive) {
            this.mutuallyExclusive = exclusive;
            return this;
        }

        /**
         * Executes allowChoiceChange.
         */
        public ChoiceBuilder allowChoiceChange(boolean allow) {
            this.allowChoiceChange = allow;
            return this;
        }

        /**
         * Executes description.
         */
        public ChoiceBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        /**
         * Executes build.
         */
        public ChoiceRequirement build() {
            if (choices.isEmpty()) {
                throw new IllegalStateException("At least one choice is needed");
            }
            return new ChoiceRequirement(choices, minimumRequired, minimumRequired, maximumRequired, description, allowPartialProgress, mutuallyExclusive, allowChoiceChange);
        }
    }

    // ==================== Timed Builder ====================

    /**
     * Represents the TimedBuilder API type.
     */
    public static final class TimedBuilder {
        private AbstractRequirement delegate;
        private long timeLimitMillis = 60000;
        private boolean autoStart = true;
        private String description;

        /**
         * Executes delegate.
         */
        public TimedBuilder delegate(@NotNull AbstractRequirement delegate) {
            this.delegate = delegate;
            return this;
        }

        /**
         * Executes seconds.
         */
        public TimedBuilder seconds(long seconds) {
            this.timeLimitMillis = seconds * 1000;
            return this;
        }

        /**
         * Executes minutes.
         */
        public TimedBuilder minutes(long minutes) {
            this.timeLimitMillis = minutes * 60 * 1000;
            return this;
        }

        /**
         * Executes hours.
         */
        public TimedBuilder hours(long hours) {
            this.timeLimitMillis = hours * 60 * 60 * 1000;
            return this;
        }

        /**
         * Executes autoStart.
         */
        public TimedBuilder autoStart(boolean auto) {
            this.autoStart = auto;
            return this;
        }

        /**
         * Executes description.
         */
        public TimedBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        /**
         * Executes build.
         */
        public TimedRequirement build() {
            if (delegate == null) {
                throw new IllegalStateException("Delegate requirement is required");
            }
            return new TimedRequirement(delegate, timeLimitMillis, autoStart, description);
        }
    }

    // ==================== Plugin Builder ====================

    /**
     * Represents the PluginBuilder API type.
     */
    public static final class PluginBuilder {
        private String plugin;
        private String category;
        private final Map<String, Double> values = new HashMap<>();
        private boolean consumable = false;
        private String description;

        /**
         * Executes plugin.
         */
        public PluginBuilder plugin(@NotNull String plugin) {
            this.plugin = plugin;
            return this;
        }

        /**
         * Executes category.
         */
        public PluginBuilder category(@NotNull String category) {
            this.category = category;
            return this;
        }

        /**
         * Executes value.
         */
        public PluginBuilder value(@NotNull String key, double value) {
            this.values.put(key, value);
            return this;
        }

        /**
         * Executes values.
         */
        public PluginBuilder values(@NotNull Map<String, Double> values) {
            this.values.putAll(values);
            return this;
        }

        /**
         * Executes consumable.
         */
        public PluginBuilder consumable(boolean consumable) {
            this.consumable = consumable;
            return this;
        }

        /**
         * Executes description.
         */
        public PluginBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        /**
         * Executes build.
         */
        public PluginRequirement build() {
            if (plugin == null || plugin.isEmpty()) {
                throw new IllegalStateException("Plugin integration ID is required");
            }
            if (values.isEmpty()) {
                throw new IllegalStateException("At least one value is required");
            }
            return new PluginRequirement(plugin, category, values, consumable, description);
        }
    }
}
