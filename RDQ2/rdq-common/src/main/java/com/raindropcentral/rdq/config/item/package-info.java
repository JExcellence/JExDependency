/**
 * Shared GUI and item presentation configuration.
 * <p>
 * The {@link com.raindropcentral.rdq.config.item.IconSection} bridges YAML icon definitions with
 * Bukkit {@link org.bukkit.inventory.ItemStack} metadata so rank and perk menus render consistently.
 * Icon blocks appear throughout {@code rank/paths/*.yml} and {@code perks/*.yml}; updating the
 * section requires updating those resources and rerunning {@code ./gradlew :RDQ:rdq-common:check}
 * to confirm the JEConfig mapping remains aligned.
 * </p>
 */
package com.raindropcentral.rdq.config.item;
