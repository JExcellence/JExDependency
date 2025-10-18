package com.raindropcentral.rplatform.workload;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Workload {

    void execute();

    static @NotNull Workload of(final @NotNull Runnable runnable) {
        return runnable::run;
    }

    default @NotNull Workload andThen(final @NotNull Workload after) {
        return () -> {
            this.execute();
            after.execute();
        };
    }
}
