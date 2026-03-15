package com.raindropcentral.rplatform.requirement.async;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.RequirementService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Async extension of RequirementService for high-performance batch operations.
 *
 * <p>Provides parallel execution for multiple requirements with proper error handling.
 */
public final class AsyncRequirementService {

    private static final AsyncRequirementService INSTANCE = new AsyncRequirementService();
    private final RequirementService syncService = RequirementService.getInstance();
    private final Executor executor = ForkJoinPool.commonPool();

    private AsyncRequirementService() {}

    /**
     * Gets instance.
     */
    @NotNull
    public static AsyncRequirementService getInstance() {
        return INSTANCE;
    }

    // ==================== Single Requirement Async ====================

    /**
     * Returns whether metAsync.
     */
    @NotNull
    public CompletableFuture<Boolean> isMetAsync(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        if (requirement instanceof AsyncRequirement async) {
            return async.isMetAsync(player);
        }
        return CompletableFuture.supplyAsync(() -> syncService.isMet(player, requirement), executor);
    }

    /**
     * Executes calculateProgressAsync.
     */
    @NotNull
    public CompletableFuture<Double> calculateProgressAsync(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        if (requirement instanceof AsyncRequirement async) {
            return async.calculateProgressAsync(player);
        }
        return CompletableFuture.supplyAsync(() -> syncService.calculateProgress(player, requirement), executor);
    }

    /**
     * Executes consumeAsync.
     */
    @NotNull
    public CompletableFuture<Void> consumeAsync(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        if (requirement instanceof AsyncRequirement async) {
            return async.consumeAsync(player);
        }
        return CompletableFuture.runAsync(() -> syncService.consume(player, requirement), executor);
    }

    // ==================== Batch Operations (Parallel) ====================

    /**
     * Check all requirements in parallel.
     */
    @NotNull
    public CompletableFuture<Boolean> areAllMetAsync(@NotNull Player player, @NotNull List<AbstractRequirement> requirements) {
        if (requirements.isEmpty()) return CompletableFuture.completedFuture(true);

        return CompletableFuture.allOf(
            requirements.stream()
                .map(req -> isMetAsync(player, req))
                .toArray(CompletableFuture[]::new)
        ).thenApply(v -> requirements.stream()
            .map(req -> isMetAsync(player, req).join())
            .allMatch(Boolean::booleanValue));
    }

    /**
     * Calculate overall progress across all requirements in parallel.
     */
    @NotNull
    public CompletableFuture<Double> calculateOverallProgressAsync(@NotNull Player player, @NotNull List<AbstractRequirement> requirements) {
        if (requirements.isEmpty()) return CompletableFuture.completedFuture(1.0);

        return CompletableFuture.allOf(
            requirements.stream()
                .map(req -> calculateProgressAsync(player, req))
                .toArray(CompletableFuture[]::new)
        ).thenApply(v -> requirements.stream()
            .mapToDouble(req -> calculateProgressAsync(player, req).join())
            .average()
            .orElse(0.0));
    }

    /**
     * Consume all requirements in parallel.
     */
    @NotNull
    public CompletableFuture<Void> consumeAllAsync(@NotNull Player player, @NotNull List<AbstractRequirement> requirements) {
        if (requirements.isEmpty()) return CompletableFuture.completedFuture(null);

        return CompletableFuture.allOf(
            requirements.stream()
                .map(req -> consumeAsync(player, req))
                .toArray(CompletableFuture[]::new)
        );
    }

    /**
     * Get detailed results for all requirements in parallel.
     */
    @NotNull
    public CompletableFuture<List<RequirementResult>> checkAllAsync(@NotNull Player player, @NotNull List<AbstractRequirement> requirements) {
        return CompletableFuture.allOf(
            requirements.stream()
                .map(req -> checkSingleAsync(player, req))
                .toArray(CompletableFuture[]::new)
        ).thenApply(v -> requirements.stream()
            .map(req -> checkSingleAsync(player, req).join())
            .toList());
    }

    private CompletableFuture<RequirementResult> checkSingleAsync(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        return isMetAsync(player, requirement)
            .thenCombine(calculateProgressAsync(player, requirement),
                (met, progress) -> new RequirementResult(requirement, met, progress));
    }

    /**
     * Represents the RequirementResult API type.
     */
    public record RequirementResult(
        @NotNull AbstractRequirement requirement,
        boolean met,
        double progress
    ) {}
}
