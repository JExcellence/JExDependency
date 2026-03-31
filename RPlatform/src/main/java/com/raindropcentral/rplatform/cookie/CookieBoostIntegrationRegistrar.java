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

package com.raindropcentral.rplatform.cookie;

import com.raindropcentral.rplatform.job.JobBridge;
import com.raindropcentral.rplatform.skill.SkillBridge;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers reflection-based runtime hooks for droplet cookie boosts.
 */
public final class CookieBoostIntegrationRegistrar {

    private static final String JOBS_REBORN_ID = "jobsreborn";
    private static final String ECO_JOBS_ID = "ecojobs";
    private static final String AURA_SKILLS_ID = "auraskills";
    private static final String ECO_SKILLS_ID = "ecoskills";
    private static final String MCMMO_ID = "mcmmo";

    private static final String LIBREFORGE_HOLDER_PROVIDER_KT = "com.willfp.libreforge.HolderProviderKt";
    private static final String LIBREFORGE_SEPARATOR_CONFIG_KT = "com.willfp.libreforge.SeparatorAmbivalentConfigKt";
    private static final String LIBREFORGE_PROVIDED_HOLDER_CONFIG_KT = "com.willfp.libreforge.ProvidedHolderConfigKt";

    private final JavaPlugin plugin;
    private final CookieBoostLookup lookup;
    private final Logger logger;
    private final Listener bridgeListener = new Listener() {
    };

    private CookieBoostIntegrationRegistrar(
            final @NotNull JavaPlugin plugin,
            final @NotNull CookieBoostLookup lookup
    ) {
        this.plugin = plugin;
        this.lookup = lookup;
        this.logger = plugin.getLogger();
    }

    /**
     * Registers all boost hooks supported by the currently resolved skill and job bridges.
     *
     * @param plugin plugin owning the listeners
     * @param lookup active boost lookup service
     */
    public static void register(
            final @NotNull JavaPlugin plugin,
            final @NotNull CookieBoostLookup lookup
    ) {
        new CookieBoostIntegrationRegistrar(plugin, lookup).registerActiveHooks();
    }

    private void registerActiveHooks() {
        this.registerSkillHooks();
        this.registerJobHooks();
    }

    private void registerSkillHooks() {
        final SkillBridge bridge = SkillBridge.getBridge();
        if (bridge == null) {
            return;
        }

        switch (bridge.getIntegrationId().toLowerCase(Locale.ROOT)) {
            case AURA_SKILLS_ID -> this.registerAuraSkillsHooks();
            case ECO_SKILLS_ID -> this.registerEcoSkillsHooks();
            case MCMMO_ID -> this.registerMcMMOHooks();
            default -> {
            }
        }
    }

    private void registerJobHooks() {
        final JobBridge bridge = JobBridge.getBridge();
        if (bridge == null) {
            return;
        }

        switch (bridge.getIntegrationId().toLowerCase(Locale.ROOT)) {
            case JOBS_REBORN_ID -> this.registerJobsRebornHooks();
            case ECO_JOBS_ID -> this.registerEcoJobsHooks();
            default -> {
            }
        }
    }

    private void registerAuraSkillsHooks() {
        this.registerEvent(
                "AuraSkills",
                "dev.aurelium.auraskills.api.event.skill.XpGainEvent",
                EventPriority.HIGHEST,
                true,
                this::handleAuraSkillsXpGain
        );
    }

    private void registerEcoSkillsHooks() {
        this.registerEvent(
                "EcoSkills",
                "com.willfp.ecoskills.api.event.PlayerSkillXPGainEvent",
                EventPriority.HIGHEST,
                true,
                this::handleEcoSkillsXpGain
        );
    }

    private void registerMcMMOHooks() {
        this.registerEvent(
                "mcMMO",
                "com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent",
                EventPriority.HIGHEST,
                true,
                this::handleMcMMOXpGain
        );
    }

    private void registerJobsRebornHooks() {
        this.registerEvent(
                "Jobs",
                "com.gamingmesh.jobs.api.JobsExpGainEvent",
                EventPriority.HIGHEST,
                true,
                this::handleJobsRebornXpGain
        );

        // JobsPaymentEvent does not expose the originating job in the current JobsReborn API,
        // so the money bonus has to be applied at the pre-payment stage where job context exists.
        this.registerEvent(
                "Jobs",
                "com.gamingmesh.jobs.api.JobsPrePaymentEvent",
                EventPriority.HIGHEST,
                true,
                this::handleJobsRebornPrePayment
        );
    }

    private void registerEcoJobsHooks() {
        this.registerEvent(
                "EcoJobs",
                "com.willfp.ecojobs.api.event.PlayerJobExpGainEvent",
                EventPriority.HIGHEST,
                true,
                this::handleEcoJobsXpGain
        );
        this.registerEvent(
                "EcoJobs",
                "com.willfp.libreforge.triggers.event.TriggerDispatchEvent",
                EventPriority.MONITOR,
                true,
                this::handleEcoJobsTriggerDispatch
        );
    }

    @SuppressWarnings("unchecked")
    private void registerEvent(
            final @NotNull String providerPluginName,
            final @NotNull String eventClassName,
            final @NotNull EventPriority priority,
            final boolean ignoreCancelled,
            final @NotNull Consumer<Object> handler
    ) {
        final Class<?> rawEventClass = this.loadClass(providerPluginName, eventClassName);
        if (rawEventClass == null || !Event.class.isAssignableFrom(rawEventClass)) {
            return;
        }

        final Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass;
        this.plugin.getServer().getPluginManager().registerEvent(
                eventClass,
                this.bridgeListener,
                priority,
                (listener, event) -> {
                    if (!eventClass.isInstance(event)) {
                        return;
                    }

                    try {
                        handler.accept(event);
                    } catch (Throwable throwable) {
                        this.logger.log(Level.WARNING, "Failed handling cookie boost hook for " + eventClassName, throwable);
                    }
                },
                this.plugin,
                ignoreCancelled
        );
    }

    private void handleAuraSkillsXpGain(final @NotNull Object event) {
        final Player player = this.resolveOnlinePlayer(this.invokeOptional(event, "getPlayer"));
        final Object skill = this.invokeOptional(event, "getSkill");
        final String skillId = this.resolveSkillIdentifier(skill);
        final Double amount = this.asDouble(this.invokeOptional(event, "getAmount"));
        if (player == null || skillId.isBlank() || amount == null || amount <= 0.0D) {
            return;
        }

        final double multiplier = this.lookup.getMultiplier(
                player.getUniqueId(),
                CookieBoostType.SKILL_XP,
                AURA_SKILLS_ID,
                skillId
        );
        if (multiplier > 1.0D) {
            this.invokeOptional(event, "setAmount", amount * multiplier);
        }
    }

    private void handleEcoSkillsXpGain(final @NotNull Object event) {
        final Player player = this.resolveOnlinePlayer(this.invokeOptional(event, "getPlayer"));
        final Object skill = this.invokeOptional(event, "getSkill");
        final String skillId = this.resolveSkillIdentifier(skill);
        final Double amount = this.asDouble(this.invokeOptional(event, "getGainedXP"));
        if (player == null || skillId.isBlank() || amount == null || amount <= 0.0D) {
            return;
        }

        final double multiplier = this.lookup.getMultiplier(
                player.getUniqueId(),
                CookieBoostType.SKILL_XP,
                ECO_SKILLS_ID,
                skillId
        );
        if (multiplier > 1.0D) {
            this.invokeOptional(event, "setGainedXP", amount * multiplier);
        }
    }

    private void handleMcMMOXpGain(final @NotNull Object event) {
        final Player player = this.resolveOnlinePlayer(this.invokeOptional(event, "getPlayer"));
        final Object skill = this.invokeOptional(event, "getSkill");
        final String skillId = skill == null ? "" : skill.toString().trim();
        final Double amount = this.asDouble(this.invokeOptional(event, "getRawXpGained"));
        if (player == null || skillId.isBlank() || amount == null || amount <= 0.0D) {
            return;
        }

        final double multiplier = this.lookup.getMultiplier(
                player.getUniqueId(),
                CookieBoostType.SKILL_XP,
                MCMMO_ID,
                skillId
        );
        if (multiplier > 1.0D) {
            this.invokeOptional(event, "setRawXpGained", (float) (amount * multiplier));
        }
    }

    private void handleJobsRebornXpGain(final @NotNull Object event) {
        final Player player = this.resolveOnlinePlayer(this.invokeOptional(event, "getPlayer"));
        final Object job = this.invokeOptional(event, "getJob");
        final String jobId = this.resolveJobIdentifier(job);
        final Double amount = this.asDouble(this.invokeOptional(event, "getExp"));
        if (player == null || jobId.isBlank() || amount == null || amount <= 0.0D) {
            return;
        }

        final double multiplier = this.lookup.getMultiplier(
                player.getUniqueId(),
                CookieBoostType.JOB_XP,
                JOBS_REBORN_ID,
                jobId
        );
        if (multiplier > 1.0D) {
            this.invokeOptional(event, "setExp", amount * multiplier);
        }
    }

    private void handleJobsRebornPrePayment(final @NotNull Object event) {
        final Player player = this.resolveOnlinePlayer(this.invokeOptional(event, "getPlayer"));
        final Object job = this.invokeOptional(event, "getJob");
        final String jobId = this.resolveJobIdentifier(job);
        final Double amount = this.asDouble(this.invokeOptional(event, "getAmount"));
        if (player == null || jobId.isBlank() || amount == null || amount <= 0.0D) {
            return;
        }

        final double multiplier = this.lookup.getMultiplier(
                player.getUniqueId(),
                CookieBoostType.JOB_VAULT,
                JOBS_REBORN_ID,
                jobId
        );
        if (multiplier > 1.0D) {
            this.invokeOptional(event, "setAmount", amount * multiplier);
        }
    }

    private void handleEcoJobsXpGain(final @NotNull Object event) {
        final Player player = this.resolveOnlinePlayer(this.invokeOptional(event, "getPlayer"));
        final Object job = this.invokeOptional(event, "getJob");
        final String jobId = this.resolveJobIdentifier(job);
        final Double amount = this.asDouble(this.invokeOptional(event, "getAmount"));
        if (player == null || jobId.isBlank() || amount == null || amount <= 0.0D) {
            return;
        }

        final double multiplier = this.lookup.getMultiplier(
                player.getUniqueId(),
                CookieBoostType.JOB_XP,
                ECO_JOBS_ID,
                jobId
        );
        if (multiplier > 1.0D) {
            this.invokeOptional(event, "setAmount", amount * multiplier);
        }
    }

    private void handleEcoJobsTriggerDispatch(final @NotNull Object event) {
        final Object dispatchedTrigger = this.invokeOptional(event, "getTrigger");
        final Object triggerType = dispatchedTrigger == null ? null : this.invokeOptional(dispatchedTrigger, "getTrigger");
        final String triggerId = this.resolveTriggerIdentifier(triggerType);
        if (!"gain_job_xp".equalsIgnoreCase(triggerId)) {
            return;
        }

        final Object triggerData = this.invokeOptional(dispatchedTrigger, "getData");
        final Player player = this.resolveOnlinePlayer(triggerData == null ? null : this.invokeOptional(triggerData, "getPlayer"));
        final Object sourceEvent = triggerData == null ? null : this.invokeOptional(triggerData, "getEvent");
        final Object job = sourceEvent == null ? null : this.invokeOptional(sourceEvent, "getJob");
        final String jobId = this.resolveJobIdentifier(job);
        if (player == null || jobId.isBlank()) {
            return;
        }

        final double multiplier = this.lookup.getMultiplier(
                player.getUniqueId(),
                CookieBoostType.JOB_VAULT,
                ECO_JOBS_ID,
                jobId
        );
        if (multiplier <= 1.0D) {
            return;
        }

        final Object dispatcher = this.invokeOptional(event, "getDispatcher");
        if (dispatcher == null) {
            return;
        }

        final double basePayout = this.resolveEcoJobsBaseVaultPayout(dispatcher, dispatchedTrigger, jobId);
        final double bonus = basePayout * (multiplier - 1.0D);
        if (bonus > 0.0D) {
            this.depositVaultMoney(player, bonus);
        }
    }

    private double resolveEcoJobsBaseVaultPayout(
            final @NotNull Object dispatcher,
            final @NotNull Object dispatchedTrigger,
            final @NotNull String targetJobId
    ) {
        final Class<?> holderProviderKt = this.loadClass("EcoJobs", LIBREFORGE_HOLDER_PROVIDER_KT);
        final Class<?> separatorConfigKt = this.loadClass("EcoJobs", LIBREFORGE_SEPARATOR_CONFIG_KT);
        final Class<?> providedHolderConfigKt = this.loadClass("EcoJobs", LIBREFORGE_PROVIDED_HOLDER_CONFIG_KT);
        if (holderProviderKt == null || separatorConfigKt == null || providedHolderConfigKt == null) {
            return 0.0D;
        }

        final Object providedEffects = this.invokeStaticOptional(holderProviderKt, "getProvidedActiveEffects", dispatcher);
        final Iterable<?> effectBlocks = this.asIterable(providedEffects);
        final Object triggerType = this.invokeOptional(dispatchedTrigger, "getTrigger");
        final Object triggerData = this.invokeOptional(dispatchedTrigger, "getData");
        double total = 0.0D;

        for (final Object providedEffectBlock : effectBlocks) {
            final Object providedHolder = this.invokeOptional(providedEffectBlock, "getHolder");
            final Object rawHolder = providedHolder == null ? null : this.invokeOptional(providedHolder, "getHolder");
            if (rawHolder == null || !this.isEcoJobsJobHolder(rawHolder, targetJobId)) {
                continue;
            }

            final Object effectBlock = this.invokeOptional(providedEffectBlock, "getEffect");
            if (effectBlock == null) {
                continue;
            }

            final Boolean triggerable = this.asBoolean(this.invokeOptional(effectBlock, "canBeTriggeredBy", triggerType));
            if (Boolean.FALSE.equals(triggerable)) {
                continue;
            }

            final Object chain = this.invokeOptional(effectBlock, "getEffects");
            for (final Object chainElement : this.asIterable(chain)) {
                final Object effect = this.invokeOptional(chainElement, "getEffect");
                final String effectId = this.resolveEffectIdentifier(effect);
                if (!"give_money".equalsIgnoreCase(effectId)) {
                    continue;
                }

                final Object config = this.invokeOptional(chainElement, "getConfig");
                if (config == null) {
                    continue;
                }

                final Object withHolder = this.invokeStaticOptional(providedHolderConfigKt, "applyHolder", config, providedHolder, dispatcher);
                final Object evaluated = this.invokeStaticOptional(
                        separatorConfigKt,
                        "getDoubleFromExpression",
                        withHolder == null ? config : withHolder,
                        "amount",
                        triggerData
                );
                final Double amount = this.asDouble(evaluated);
                if (amount != null && amount > 0.0D) {
                    total += amount;
                }
            }
        }

        return total;
    }

    private boolean isEcoJobsJobHolder(final @NotNull Object holder, final @NotNull String targetJobId) {
        if (!holder.getClass().getName().equals("com.willfp.ecojobs.jobs.JobLevel")) {
            return false;
        }

        final Object job = this.invokeOptional(holder, "getJob");
        final String holderJobId = this.resolveJobIdentifier(job);
        return !holderJobId.isBlank() && this.normalizeLookupKey(holderJobId).equals(this.normalizeLookupKey(targetJobId));
    }

    private boolean depositVaultMoney(final @NotNull OfflinePlayer player, final double amount) {
        if (amount <= 0.0D) {
            return false;
        }

        try {
            final Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            final RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(economyClass);
            if (provider == null) {
                return false;
            }

            final Object economy = provider.getProvider();
            final Method method = this.findMethod(economy.getClass(), "depositPlayer", player, amount);
            if (method != null) {
                method.setAccessible(true);
                method.invoke(economy, player, amount);
                return true;
            }
        } catch (Exception exception) {
            this.logger.log(Level.FINE, "Failed to deposit Vault bonus payout", exception);
        }
        return false;
    }

    private @Nullable Player resolveOnlinePlayer(final @Nullable Object rawPlayer) {
        if (rawPlayer instanceof Player player) {
            return player.isOnline() ? player : null;
        }
        if (rawPlayer instanceof OfflinePlayer offlinePlayer) {
            final Player player = offlinePlayer.getPlayer();
            return player != null && player.isOnline() ? player : null;
        }
        if (rawPlayer instanceof UUID uniqueId) {
            final Player player = Bukkit.getPlayer(uniqueId);
            return player != null && player.isOnline() ? player : null;
        }
        return null;
    }

    private @NotNull String resolveSkillIdentifier(final @Nullable Object skill) {
        if (skill == null) {
            return "";
        }

        final Object resolved = this.firstNonNull(
                this.invokeOptional(skill, "getId"),
                this.invokeOptional(skill, "getID"),
                this.invokeOptional(skill, "getKey"),
                this.invokeOptional(skill, "getIdentifier"),
                this.invokeOptional(skill, "getName"),
                this.invokeOptional(skill, "name")
        );
        return (resolved == null ? skill : resolved).toString().trim();
    }

    private @NotNull String resolveJobIdentifier(final @Nullable Object job) {
        if (job == null) {
            return "";
        }

        final Object resolved = this.firstNonNull(
                this.invokeOptional(job, "getName"),
                this.invokeOptional(job, "getJobName"),
                this.invokeOptional(job, "getId"),
                this.invokeOptional(job, "getID"),
                this.invokeOptional(job, "getIdentifier"),
                this.invokeOptional(job, "getKey")
        );
        return (resolved == null ? job : resolved).toString().trim();
    }

    private @NotNull String resolveTriggerIdentifier(final @Nullable Object trigger) {
        if (trigger == null) {
            return "";
        }

        final Object resolved = this.firstNonNull(
                this.invokeOptional(trigger, "getID"),
                this.invokeOptional(trigger, "getId"),
                this.invokeOptional(trigger, "getIdentifier"),
                this.invokeOptional(trigger, "getName"),
                this.invokeOptional(trigger, "name")
        );
        return (resolved == null ? trigger : resolved).toString().trim();
    }

    private @NotNull String resolveEffectIdentifier(final @Nullable Object effect) {
        if (effect == null) {
            return "";
        }

        final Object resolved = this.firstNonNull(
                this.invokeOptional(effect, "getId"),
                this.invokeOptional(effect, "getID"),
                this.invokeOptional(effect, "getIdentifier"),
                this.invokeOptional(effect, "getName"),
                this.invokeOptional(effect, "name")
        );
        return (resolved == null ? effect : resolved).toString().trim();
    }

    private @Nullable Object invokeOptional(
            final @Nullable Object target,
            final @NotNull String methodName,
            final Object... arguments
    ) {
        if (target == null) {
            return null;
        }

        try {
            final Method method = this.findMethod(target.getClass(), methodName, arguments);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return this.unwrapOptional(method.invoke(target, arguments));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private @Nullable Object invokeStaticOptional(
            final @Nullable Class<?> owner,
            final @NotNull String methodName,
            final Object... arguments
    ) {
        if (owner == null) {
            return null;
        }

        try {
            final Method method = this.findMethod(owner, methodName, arguments);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return this.unwrapOptional(method.invoke(null, arguments));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private @Nullable Object readFieldOptional(
            final @Nullable Object target,
            final @NotNull String fieldName
    ) {
        if (target == null) {
            return null;
        }

        try {
            final Field field = this.findField(target.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private @Nullable Class<?> loadClass(
            final @NotNull String providerPluginName,
            final @NotNull String className
    ) {
        final Plugin providerPlugin = Bukkit.getPluginManager().getPlugin(providerPluginName);
        if (providerPlugin != null) {
            try {
                return Class.forName(className, true, providerPlugin.getClass().getClassLoader());
            } catch (ClassNotFoundException ignored) {
            }
        }

        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private @Nullable Object firstNonNull(final @Nullable Object... values) {
        for (final Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private @Nullable Double asDouble(final @Nullable Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private @Nullable Boolean asBoolean(final @Nullable Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return null;
    }

    private @NotNull Iterable<?> asIterable(final @Nullable Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Iterable<?> iterable) {
            return iterable;
        }
        if (value instanceof Object[] array) {
            return Arrays.asList(array);
        }
        if (value instanceof Collection<?> collection) {
            return collection;
        }

        final List<Object> single = new ArrayList<>(1);
        single.add(value);
        return single;
    }

    private @NotNull String normalizeLookupKey(final @NotNull String input) {
        return input.trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");
    }

    private @Nullable Method findMethod(
            final @NotNull Class<?> owner,
            final @NotNull String methodName,
            final Object... arguments
    ) {
        for (final Method method : owner.getMethods()) {
            if (method.getName().equals(methodName) && this.parametersMatch(method.getParameterTypes(), arguments)) {
                return method;
            }
        }

        Class<?> current = owner;
        while (current != null) {
            for (final Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && this.parametersMatch(method.getParameterTypes(), arguments)) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private boolean parametersMatch(final @NotNull Class<?>[] parameterTypes, final @Nullable Object[] arguments) {
        final int argumentCount = arguments == null ? 0 : arguments.length;
        if (parameterTypes.length != argumentCount) {
            return false;
        }

        for (int index = 0; index < parameterTypes.length; index++) {
            final Class<?> parameterType = this.wrapPrimitive(parameterTypes[index]);
            final Object argument = arguments[index];

            if (argument == null) {
                if (parameterTypes[index].isPrimitive()) {
                    return false;
                }
                continue;
            }

            if (!parameterType.isInstance(argument)) {
                return false;
            }
        }

        return true;
    }

    private @NotNull Class<?> wrapPrimitive(final @NotNull Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }

        return switch (type.getName()) {
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "char" -> Character.class;
            case "short" -> Short.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            default -> type;
        };
    }

    private @Nullable Field findField(final @NotNull Class<?> owner, final @NotNull String fieldName) {
        Class<?> current = owner;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private @Nullable Object unwrapOptional(final @Nullable Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }
}
