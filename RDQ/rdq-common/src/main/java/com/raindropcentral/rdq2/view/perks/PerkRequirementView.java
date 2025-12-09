/*
package com.raindropcentral.rdq2.view.perks;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.database.entity.perk.RPerk;
import com.raindropcentral.rdq2.database.entity.perk.RPerkUnlockRequirement;
import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq2.perk.runtime.LoadedPerk;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

*/
/**
 * Paginated view for displaying perk unlock requirements with progress tracking.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 1.0.0
 *//*

public class PerkRequirementView extends APaginatedView<RPerkUnlockRequirement> {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkRequirementView.class.getName());

    private final State<RDQ> rdq = initialState("plugin");
    private final State<String> perkId = initialState("perkId");
    private final State<RDQPlayer> rdqPlayer = initialState("player");

    public PerkRequirementView() {
        super(PerkDetailView.class);
    }

    @Override
    protected String getKey() {
        return "perk.requirement_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            "    i    ",
            "  OOOOO  ",
            "  OOOOO  ",
            "  OOOOO  ",
            "b  <p>   "
        };
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        try {
            final String id = this.perkId.get(openContext);
            final RDQ plugin = this.rdq.get(openContext);
            final LoadedPerk perk = plugin.getPerkRegistry().get(id);

            if (perk == null) {
                return Map.of("perk_name", id);
            }

            final String perkName = this.i18n(perk.config().displayName(), openContext.getPlayer())
                    .build()
                    .toString();

            return Map.of("perk_name", perkName);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error getting title placeholders", e);
            return Map.of("perk_name", "Unknown Perk");
        }
    }

    @Override
    protected CompletableFuture<List<RPerkUnlockRequirement>> getAsyncPaginationSource(final @NotNull Context context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final RDQ plugin = this.rdq.get(context);
                final String id = this.perkId.get(context);
                final RPerk rPerk = plugin.getPerkRepository().findByAttributes(Map.of("identifier", id));

                if (rPerk == null || rPerk.getUnlockRequirements().isEmpty()) {
                    return List.of();
                }

                return rPerk.getUnlockRequirementsOrdered();
            } catch (final Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading perk requirements", e);
                return List.of();
            }
        });
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull RPerkUnlockRequirement requirement
    ) {
        try {
            final Player player = context.getPlayer();
            final ItemStack item = createRequirementItem(player, requirement);
            builder.withItem(item)
                    .updateOnClick()
                    .onClick(click -> handleRequirementClick(context, requirement));
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error rendering requirement entry", e);
        }
    }

    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        try {
            final RDQ plugin = this.rdq.get(render);
            final String id = this.perkId.get(render);
            final LoadedPerk perk = plugin.getPerkRegistry().get(id);

            if (perk == null) {
                renderErrorState(render, player);
                return;
            }

            // Render perk info at top
            render.layoutSlot('i', createPerkInfoItem(player, perk));
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error rendering perk requirement view", e);
            renderErrorState(render, player);
        }
    }

    private @NotNull ItemStack createPerkInfoItem(final @NotNull Player player, final @NotNull LoadedPerk perk) {
        try {
            final List<Component> lore = new ArrayList<>();
            
            // Description
            final String desc = perk.config().description();
            if (desc != null && !desc.isEmpty()) {
                lore.addAll(this.i18n(desc, player).build().splitLines());
                lore.add(Component.empty());
            }

            // Category and Priority
            lore.add(this.i18n("perk.info.category", player)
                    .with("category", perk.config().category().name())
                    .build().component());
            lore.add(this.i18n("perk.info.priority", player)
                    .with("priority", String.valueOf(perk.config().priority()))
                    .build().component());
            lore.add(Component.empty());
            lore.add(this.i18n("perk.info.requirements_below", player).build().component());

            return UnifiedBuilderFactory.item(Material.valueOf(perk.config().iconMaterial()))
                    .setName(this.i18n(perk.config().displayName(), player).build().component())
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                    .build();
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create perk info item", e);
            return UnifiedBuilderFactory.item(Material.PAPER)
                    .setName(this.i18n("perk.info.error", player).build().component())
                    .build();
        }
    }

    private @NotNull ItemStack createRequirementItem(
            final @NotNull Player player,
            final @NotNull RPerkUnlockRequirement requirement
    ) {
        try {
            final double progress = requirement.calculateProgress(player);
            final boolean isMet = requirement.isMet(player);
            final String descKey = requirement.getRequirement().getRequirement().getDescriptionKey();
            final String requirementType = requirement.getRequirement().getRequirement().getType().name();

            final List<Component> lore = new ArrayList<>();

            // Type
            lore.add(this.i18n("perk.requirement.type", player)
                    .with("type", requirementType)
                    .build().component());
            lore.add(Component.empty());

            // Description
            lore.addAll(this.i18n(descKey, player).build().splitLines());
            lore.add(Component.empty());

            // Progress bar
            lore.add(this.i18n("perk.requirement.progress", player).build().component());
            lore.add(createProgressBar(progress, 20));
            lore.add(this.i18n("perk.requirement.progress_percent", player)
                    .with("progress", String.format("%.1f", progress * 100))
                    .build().component());
            lore.add(Component.empty());

            // Status
            if (isMet) {
                lore.add(this.i18n("perk.requirement.status.completed", player).build().component());
            } else if (progress > 0) {
                lore.add(this.i18n("perk.requirement.status.in_progress", player).build().component());
            } else {
                lore.add(this.i18n("perk.requirement.status.not_started", player).build().component());
            }

            final Material material = isMet ? Material.LIME_DYE : (progress > 0 ? Material.YELLOW_DYE : Material.RED_DYE);
            return UnifiedBuilderFactory.item(material)
                    .setName(this.i18n("perk.requirement.name", player)
                            .with("requirement_type", requirementType)
                            .build().component())
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setGlowing(isMet)
                    .build();
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create requirement item", e);
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("perk.requirement.error", player).build().component())
                    .build();
        }
    }

    private @NotNull Component createProgressBar(final double progress, final int length) {
        final int filled = (int) (progress * length);
        return Component.text("")
                .append(MiniMessage.miniMessage().deserialize("<green>â–ˆ</green>".repeat(Math.max(0, filled))))
                .append(MiniMessage.miniMessage().deserialize("<gray>â–ˆ</gray>".repeat(Math.max(0, length - filled))));
    }

    private void handleRequirementClick(final @NotNull Context context, final @NotNull RPerkUnlockRequirement requirement) {
        final Player player = context.getPlayer();
        try {
            final double progress = requirement.calculateProgress(player);
            final boolean isMet = requirement.isMet(player);

            this.i18n("perk.requirement.click_info", player)
                    .with("progress", String.format("%.1f", progress * 100))
                    .with("status", isMet ? "COMPLETED" : "IN PROGRESS")
                    .withPrefix()
                    .send();
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to handle requirement click", e);
        }
    }

    private void renderErrorState(final @NotNull RenderContext render, final @NotNull Player player) {
        try {
            render.slot(22,
                    UnifiedBuilderFactory.item(Material.BARRIER)
                            .setName(this.i18n("perk.requirement.error.name", player).build().component())
                            .setLore(this.i18n("perk.requirement.error.lore", player).build().splitLines())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build()
            );
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to render error state", e);
        }
    }
}*/
