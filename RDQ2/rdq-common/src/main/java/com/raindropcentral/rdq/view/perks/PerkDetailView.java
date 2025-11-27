package com.raindropcentral.rdq.view.perks;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.perk.RPerkUnlockRequirement;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.perk.runtime.LoadedPerk;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq.type.EPerkState;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.raindropcentral.rdq.type.EPerkState.*;

public class PerkDetailView extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkDetailView.class.getName());

    private final State<RDQ> rdq = initialState("plugin");
    private final State<String> perkId = initialState("perkId");
    private final State<RDQPlayer> rdqPlayer = initialState("player");

    @Override
    protected String getKey() {
        return "perk.detail_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "GGGGGGGGG",
            "G   p   G",
            "G       G",
            "G s i a G",
            "G r u t G",
            "bGGGGGGGn"
        };
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(@NotNull OpenContext openContext) {
        try {
            var id = this.perkId.get(openContext);
            var plugin = this.rdq.get(openContext);
            var perk = plugin.getPerkRegistry().get(id);

            if (perk == null) {
                return Map.of("perk_name", id);
            }

            var perkName = this.i18n(perk.config().displayName(), openContext.getPlayer())
                    .build()
                    .toString();

            return Map.of("perk_name", perkName);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting title placeholders", e);
            return Map.of("perk_name", "Unknown Perk");
        }
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        try {
            var plugin = this.rdq.get(render);
            var id = this.perkId.get(render);
            var registry = plugin.getPerkRegistry();
            var perkManager = plugin.getPerkInitializationManager().getPerkManager();

            var perk = registry.get(id);
            if (perk == null) {
                this.i18n("perk.error.not_found", player)
                        .with("perk", id)
                        .withPrefix()
                        .send();
                player.closeInventory();
                return;
            }

            this.renderDecorations(render, player);

            var state = calculateState(plugin, perk, player);
            var isOwned = (state != LOCKED);
            var isActive = (state == ACTIVE);

            // Main perk info display (center top)
            render.layoutSlot('p', createPerkInfoItem(player, perk, state));

            // Status indicator (left)
            render.layoutSlot('s', createStatusItem(player, state));

            // Info/Details button (center)
            render.layoutSlot('i', createDetailsItem(player, perk, state));

            // Activate/Deactivate button (right)
            if (isOwned && !isActive && state == AVAILABLE) {
                render.layoutSlot('a', createActivateButton(player))
                        .onClick(click -> {
                            perk.type().activate(player, perk);
                            this.i18n("perk.messages.activated", player)
                                    .with("perk_name", perk.config().displayName())
                                    .withPrefix()
                                    .send();
                            render.update();
                        });
            } else if (isActive) {
                render.layoutSlot('a', createDeactivateButton(player))
                        .onClick(click -> {
                            perk.type().deactivate(player, perk);
                            this.i18n("perk.messages.deactivated", player)
                                    .with("perk_name", perk.config().displayName())
                                    .withPrefix()
                                    .send();
                            render.update();
                        });
            }

            if (!isOwned) {
                var rPerk = plugin.getPerkRepository().findByAttributes(Map.of("identifier", id));
                var progressPercent = rPerk != null ? calculateOverallProgress(player, rPerk) : 0;
                var requirementCounts = rPerk != null ? getRequirementCounts(player, rPerk) : new int[]{0, 0};

                // View requirements button
                render.layoutSlot('r', createViewRequirementsButton(player, progressPercent, requirementCounts))
                        .onClick(click -> {
                            click.closeForPlayer();
                            plugin.getViewFrame().open(
                                    PerkRequirementView.class,
                                    player,
                                    Map.of(
                                            "plugin", plugin,
                                            "perkId", id,
                                            "player", this.rdqPlayer.get(render)
                                    )
                            );
                        });

                // Unlock button
                render.layoutSlot('u', createUnlockButton(player, requirementCounts[0], requirementCounts[1]))
                        .onClick(click -> {
                            click.closeForPlayer();
                            plugin.getViewFrame().open(
                                    PerkUnlockView.class,
                                    player,
                                    Map.of(
                                            "plugin", plugin,
                                            "perkId", id,
                                            "player", this.rdqPlayer.get(render)
                                    )
                            );
                        });
            }

            // Toggle button (if owned)
            if (isOwned) {
                render.layoutSlot('t', createToggleInfoButton(player, perk, state));
            }

            // Navigate to perks list
            render.layoutSlot('n', createNavigationButton(player))
                    .onClick(click -> {
                        click.closeForPlayer();
                        plugin.getViewFrame().open(
                                PerkListViewFrame.class,
                                player,
                                Map.of(
                                        "plugin", plugin,
                                        "player", this.rdqPlayer.get(render)
                                )
                        );
                    });

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error rendering perk detail view", e);
        }
    }

    private @NotNull ItemStack createPerkInfoItem(
            @NotNull Player player,
            @NotNull LoadedPerk perk,
            @NotNull EPerkState state
    ) {
        try {
            var lore = new ArrayList<Component>();

            var desc = perk.config().description();
            if (desc != null && !desc.isEmpty()) {
                lore.addAll(this.i18n(desc, player).build().splitLines());
                lore.add(Component.empty());
            }

            lore.add(this.i18n("perk.detail.state", player)
                    .with("state", state.name())
                    .build().component());
            lore.add(Component.empty());

            lore.add(this.i18n("perk.detail.category", player)
                    .with("category", perk.config().category().name())
                    .build().component());

            lore.add(this.i18n("perk.detail.priority", player)
                    .with("priority", String.valueOf(perk.config().priority()))
                    .build().component());

            var material = Material.valueOf(perk.config().iconMaterial());
            return UnifiedBuilderFactory.item(material)
                    .setName(this.i18n(perk.config().displayName(), player).build().component())
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                    .setGlowing(state == ACTIVE)
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create perk info item", e);
            return UnifiedBuilderFactory.item(Material.PAPER)
                    .setName(this.i18n("perk.detail.error", player).build().component())
                    .build();
        }
    }

    private @NotNull ItemStack createStatusItem(
            @NotNull Player player,
            @NotNull EPerkState state
    ) {
        var material = switch (state) {
            case ACTIVE -> Material.LIME_CONCRETE;
            case AVAILABLE -> Material.YELLOW_CONCRETE;
            case LOCKED -> Material.RED_CONCRETE;
            case COOLDOWN -> Material.BLUE_CONCRETE;
            case DISABLED -> Material.BARRIER;
        };

        var lore = new ArrayList<Component>();
        lore.addAll(this.i18n("perk.status." + state.name().toLowerCase() + ".lore", player)
                .build()
                .splitLines());

        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n("perk.status.name", player)
                        .with("status", state.name())
                        .build().component())
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(state == ACTIVE)
                .build();
    }

    private @NotNull ItemStack createDetailsItem(
            final @NotNull Player player,
            final @NotNull LoadedPerk perk,
            final @NotNull EPerkState state
    ) {
        final List<Component> lore = new ArrayList<>();

        lore.add(this.i18n("perk.details.type", player)
                .with("type", perk.type().getClass().getSimpleName())
                .build().component());
        lore.add(Component.empty());

        if (state == ACTIVE) {
            lore.addAll(this.i18n("perk.details.active_effects", player)
                    .build()
                    .splitLines());
        } else if (state == AVAILABLE) {
            lore.addAll(this.i18n("perk.details.available_info", player)
                    .build()
                    .splitLines());
        } else {
            lore.addAll(this.i18n("perk.details.locked_info", player)
                    .build()
                    .splitLines());
        }

        return UnifiedBuilderFactory.item(Material.BOOK)
                .setName(this.i18n("perk.details.name", player).build().component())
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createActivateButton(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.EMERALD)
                .setName(this.i18n("perk.button.activate.name", player).build().component())
                .setLore(this.i18n("perk.button.activate.lore", player).build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(true)
                .build();
    }

    private @NotNull ItemStack createDeactivateButton(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.REDSTONE)
                .setName(this.i18n("perk.button.deactivate.name", player).build().component())
                .setLore(this.i18n("perk.button.deactivate.lore", player).build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createViewRequirementsButton(
            final @NotNull Player player,
            final int progressPercent,
            final int[] requirementCounts
    ) {
        final List<Component> lore = new ArrayList<>();
        lore.add(this.i18n("perk.button.requirements.progress", player)
                .with("progress", String.valueOf(progressPercent))
                .build().component());
        lore.add(this.i18n("perk.button.requirements.count", player)
                .with("completed", String.valueOf(requirementCounts[0]))
                .with("total", String.valueOf(requirementCounts[1]))
                .build().component());
        lore.add(Component.empty());
        lore.addAll(this.i18n("perk.button.requirements.lore", player).build().splitLines());

        // Progress bar
        lore.add(Component.empty());
        lore.add(createProgressBar(progressPercent / 100.0, 15));

        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("perk.button.requirements.name", player).build().component())
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createUnlockButton(
            final @NotNull Player player,
            final int completed,
            final int total
    ) {
        final boolean canUnlock = completed >= total && total > 0;

        final List<Component> lore = new ArrayList<>();
        lore.add(this.i18n("perk.button.unlock.requirements", player)
                .with("completed", String.valueOf(completed))
                .with("total", String.valueOf(total))
                .build().component());
        lore.add(Component.empty());

        if (canUnlock) {
            lore.addAll(this.i18n("perk.button.unlock.ready_lore", player).build().splitLines());
        } else {
            lore.addAll(this.i18n("perk.button.unlock.locked_lore", player).build().splitLines());
        }

        final Material material = canUnlock ? Material.DIAMOND : Material.IRON_INGOT;
        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n("perk.button.unlock.name", player).build().component())
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(canUnlock)
                .build();
    }

    private @NotNull ItemStack createToggleInfoButton(
            final @NotNull Player player,
            final @NotNull LoadedPerk perk,
            final @NotNull EPerkState state
    ) {
        final List<Component> lore = new ArrayList<>();
        lore.add(this.i18n("perk.button.toggle.current_state", player)
                .with("state", state.name())
                .build().component());
        lore.add(Component.empty());
        lore.addAll(this.i18n("perk.button.toggle.lore", player).build().splitLines());

        return UnifiedBuilderFactory.item(Material.LEVER)
                .setName(this.i18n("perk.button.toggle.name", player).build().component())
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createNavigationButton(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.COMPASS)
                .setName(this.i18n("perk.button.navigate.name", player).build().component())
                .setLore(this.i18n("perk.button.navigate.lore", player).build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull Component createProgressBar(final double progress, final int length) {
        final int filled = (int) (progress * length);
        return Component.text("")
                .append(MiniMessage.miniMessage().deserialize("<green>â–ˆ</green>".repeat(Math.max(0, filled))))
                .append(MiniMessage.miniMessage().deserialize("<gray>â–ˆ</gray>".repeat(Math.max(0, length - filled))));
    }

    private @NotNull EPerkState calculateState(
            @NotNull RDQ plugin,
            @NotNull LoadedPerk perk,
            @NotNull Player player
    ) {
        try {
            var perkManager = plugin.getPerkInitializationManager().getPerkManager();
            if (perkManager.isActive(player, perk.getId())) {
                return ACTIVE;
            }
            return LOCKED;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to calculate perk state", e);
            return LOCKED;
        }
    }

    private int calculateOverallProgress(@NotNull Player player, @NotNull RPerk rPerk) {
        try {
            var requirements = rPerk.getUnlockRequirementsOrdered();
            if (requirements.isEmpty()) {
                return 100;
            }

            var totalProgress = requirements.stream()
                    .mapToDouble(req -> req.calculateProgress(player))
                    .sum();

            return (int) Math.min(100, (totalProgress / requirements.size()) * 100);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to calculate perk progress", e);
            return 0;
        }
    }

    private int[] getRequirementCounts(@NotNull Player player, @NotNull RPerk rPerk) {
        try {
            var requirements = rPerk.getUnlockRequirementsOrdered();
            var total = requirements.size();
            var completed = (int) requirements.stream()
                    .filter(req -> req.isMet(player))
                    .count();

            return new int[]{completed, total};
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to count perk requirements", e);
            return new int[]{0, 0};
        }
    }

    private void renderDecorations(
            @NotNull RenderContext render,
            @NotNull Player player
    ) {
        render.layoutSlot(
                'G',
                UnifiedBuilderFactory
                        .item(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                        .setName(this.i18n("decoration.name", player).build().component())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        );
    }
}