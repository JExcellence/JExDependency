/**
 * Digit-specific head definitions used to render pagination counters and numeric badges.
 *
 * <p>The classes in this package expose immutable {@link com.raindropcentral.rplatform.utility.heads.RHead}
 * implementations that map digits {@code 0-15} to textured items.  Pagination controllers
 * compose these heads to display page numbers, counters, or progress bars inside inventory
 * UIs.</p>
 *
 * <p><strong>Usage patterns.</strong> Combine instances from this package with higher level
 * navigation controls from {@link com.raindropcentral.rplatform.utility.heads.view} to display
 * a consistent numeric footer or overlay.  When multiple digits are required, memoize the
 * resulting {@link org.bukkit.inventory.ItemStack} tuples per player and page to avoid
 * re-serializing texture data on every repaint.</p>
 *
 * <p><strong>Extension points.</strong> Custom glyphs can be registered by subclassing one of the
 * base digit classes or implementing a new {@link com.raindropcentral.rplatform.utility.heads.RHead}
 * that follows the same translation key contract (e.g., {@code head.number_16}).  Register the
 * custom digits alongside the defaults so pagination layout engines can pick them up from a
 * shared registry.</p>
 *
 * <p><strong>Performance.</strong> For large paginated lists, compute the {@code ItemStack}
 * combinations asynchronously after resolving translations, but schedule the actual item
 * mutation on the main thread.  The digit heads rely on shared builder factories and can be
 * safely reused across threads when treated as immutable templates.</p>
 */
package com.raindropcentral.rplatform.utility.heads.view.pagination;
