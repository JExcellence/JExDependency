/**
 * Cross-server player-state migration — JSON export / import of every
 * JExQuests-owned row for one UUID. Admin-only (the
 * {@code /jexquests export|import} commands are permission-gated);
 * intended for server migrations, backups, and manual reconciliation.
 */
package de.jexcellence.quests.migration;
