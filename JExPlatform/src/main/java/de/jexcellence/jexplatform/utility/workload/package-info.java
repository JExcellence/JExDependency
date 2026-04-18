/**
 * Tick-budget-aware workload distribution for spreading heavy operations
 * across multiple server ticks.
 *
 * <p>{@link de.jexcellence.jexplatform.utility.workload.WorkloadExecutor} drains
 * a queue of {@link de.jexcellence.jexplatform.utility.workload.Workload} tasks
 * within a configurable per-tick millisecond budget (default 2.5 ms).
 *
 * @since 1.0.0
 */
package de.jexcellence.jexplatform.utility.workload;
