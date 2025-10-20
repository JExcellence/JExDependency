/**
 * Cooperative workload queues that cap the amount of synchronous work performed during a server tick.
 * <p>
 * {@link com.raindropcentral.rplatform.workload.Workload} is a functional interface describing an
 * executable unit, while {@link com.raindropcentral.rplatform.workload.WorkloadExecutor} buffers those
 * units behind a {@link java.util.Deque}. The executor protects its queue with a private lock, so
 * {@link com.raindropcentral.rplatform.workload.WorkloadExecutor#submit(Workload)} can be invoked from
 * any thread without racing the tick loop. {@link com.raindropcentral.rplatform.workload.WorkloadExecutor#run()}
 * drains as many workloads as possible until roughly 2.5&nbsp;ms of budget is consumed; schedule this
 * method on the main thread using {@link com.raindropcentral.rplatform.scheduler.ISchedulerAdapter#runRepeating(Runnable, long, long)}
 * once {@link com.raindropcentral.rplatform.RPlatform#initialize()} completes so translation managers,
 * command updates, and database resources are already in place.
 * </p>
 * <p>
 * Use {@link com.raindropcentral.rplatform.workload.WorkloadExecutor#submitAsync(Workload)} for callers
 * that need a completion signal. The returned {@link java.util.concurrent.CompletableFuture} resolves
 * after the workload has been drained by the tick loop. Because the future waits by polling the queue,
 * avoid submitting from the primary server thread; prefer the scheduler's asynchronous methods when
 * enqueueing bulk work from bootstrap flows. Clearing or interrogating the queue via
 * {@link com.raindropcentral.rplatform.workload.WorkloadExecutor#getPendingCount()} and
 * {@link com.raindropcentral.rplatform.workload.WorkloadExecutor#clear()} is also synchronised, making
 * the executor safe to share across services that coordinate during platform initialisation.
 * </p>
 */
package com.raindropcentral.rplatform.workload;
