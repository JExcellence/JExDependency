/**
 * Statistics delivery engine for JExCore. Plugins push {@link de.jexcellence.core.stats.StatisticEntry}
 * records; the engine batches, signs, compresses, rate-limits, and delivers
 * over HTTP with exponential-backoff retries and a disk-backed offline
 * spool.
 */
package de.jexcellence.core.stats;
