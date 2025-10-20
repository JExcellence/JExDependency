package com.raindropcentral.rdq.manager.quest;

/**
 * Central contract for managing the RaindropQuests lifecycle across editions.
 * <p>
 * Implementations coordinate the following responsibilities:
 * </p>
 * <ul>
 *     <li>Synchronizing quest definitions between persistent storage and in-memory caches.</li>
 *     <li>Tracking player progress, evaluating completion state, and dispatching reward logic.</li>
 *     <li>Exposing subscription hooks so UI controllers and listeners stay aligned with quest updates.</li>
 * </ul>
 * <p>
 * Thread-safety guarantees and data access strategies may differ between free and premium builds,
 * therefore callers should rely solely on the behaviours declared by this interface when
 * orchestrating quest flows.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public interface QuestManager {
}