/**
 * Permission-aware configuration sections that expose per-player overrides for platform behaviour.
 *
 * <h2>Runtime behaviour mapping</h2>
 * <p>{@link APermissionBasedSection} extracts and normalises {@link org.bukkit.entity.Player} permissions
 * before delegating to specialised implementations. {@link PermissionCooldownSection} maps permission keys
 * to cooldown windows so quest and perk handlers can honour shortened or extended delays, while
 * {@link PermissionDurationSection} reuses {@link com.raindropcentral.rplatform.config.DurationSection}
 * parsing to produce second- and millisecond-accurate effect windows.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/config/permission/APermissionBasedSection.java†L21-L139】【F:RPlatform/src/main/java/com/raindropcentral/rplatform/config/permission/PermissionCooldownSection.java†L21-L122】</p>
 * <p>The resulting values feed directly into RDQ runtime checks—callers use
 * {@code getEffectiveValue(Player)} to gate abilities and rewards without duplicating permission parsing
 * logic. Maintaining these sections keeps free and premium editions aligned because they resolve through
 * the shared platform registry.</p>
 *
 * <h2>Maintaining permission ladders</h2>
 * <p>When adding new permission namespaces, document the precedence rules ({@link APermissionBasedSection#getUseBestValue()})
 * so operations teams can grant consistent upgrades. Guard rails like {@link PermissionDurationSection#getMaxDurationSeconds()}
 * ensure configuration mistakes cannot silently grant unbounded perks; update their limits alongside
 * gameplay balancing changes.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/config/permission/PermissionDurationSection.java†L70-L208】</p>
 *
 * <h2>Update coordination</h2>
 * <p>Because permission maps are evaluated lazily from YAML, updates become visible on the next reload. When
 * reorganising keys, publish migration snippets so old permissions are removed from player groups before the
 * new entries go live, preventing unexpected fallthrough to defaults.</p>
 */
package com.raindropcentral.rplatform.config.permission;
