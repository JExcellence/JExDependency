/**
 * Integration tests for RDQ plugin systems.
 *
 * <p>These tests verify the full flow of core functionality:
 * <ul>
 *   <li>Rank progression: tree selection, rank unlocking, progress tracking</li>
 *   <li>Perk activation: unlock, activate, deactivate, cooldown handling</li>
 *   <li>Bounty flow: creation, claiming, expiration, statistics</li>
 * </ul>
 *
 * <p>Tests use Mockito for repository mocking to isolate service logic
 * from database operations.
 */
package com.raindropcentral.rdq.integration;
