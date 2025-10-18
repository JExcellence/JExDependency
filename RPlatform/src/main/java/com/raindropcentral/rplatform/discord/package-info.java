/**
 * Discord webhook helper utilities.
 * <p>
 * Webhook calls are performed asynchronously via a cached thread pool so they
 * do not depend on the platform scheduler. Nevertheless, prefer creating
 * webhook instances only after {@link com.raindropcentral.rplatform.RPlatform#initialize()}
 * so logging, metrics, and translation services have been configured. Metrics
 * emitted around webhook success/failure should be routed through
 * {@link com.raindropcentral.rplatform.metrics.MetricsManager} once initialized
 * by {@link com.raindropcentral.rplatform.RPlatform#initializeMetrics(int)}.
 * </p>
 * <p>
 * When integrating with translated content, build payloads after resolving
 * locale-specific strings from the translation service and avoid blocking the
 * returned {@link java.util.concurrent.CompletableFuture}. Chain callbacks via
 * {@link java.util.concurrent.CompletableFuture#thenAccept(java.util.function.Consumer)}
 * or similar helpers to keep the async flow non-blocking.
 * </p>
 */
package com.raindropcentral.rplatform.discord;
