/**
 * Service layer for JExQuests. Each service wraps one or more
 * repositories, delegates reward/requirement handling to JExCore's
 * shared {@code RewardExecutor} / {@code RequirementEvaluator} via
 * {@code Bukkit.getServicesManager()}, and terminates every
 * {@code CompletableFuture} with a graceful {@code .exceptionally(...)}.
 */
package de.jexcellence.quests.service;
