package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Town level-up management view.
 *
 * <p>This view links to requirement and reward browsers and allows players with
 * {@link TownPermissions#TOWN_LEVEL_UP} to attempt town progression.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.1
 */
public final class TownLevelUpView extends BaseView {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the town level-up view with return navigation to town overview.
     */
    public TownLevelUpView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "town_level_up_ui";
    }

    /**
     * Returns the inventory size in rows.
     *
     * @return row count
     */
    @Override
    protected int getSize() {
        return 2;
    }

    /**
     * Cancels default inventory movement behavior.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Renders level-up controls for the current town.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(render);
        final RTown town = this.resolveTown(render, player);
        if (plugin == null || !this.verifyViewerAccess(render, player, town)) {
            player.closeInventory();
            return;
        }

        render.slot(1, 3).withItem(this.buildInfoItem(player, plugin, town));
        render.slot(1, 4)
                .withItem(this.buildRequirementsItem(player, plugin, town))
                .onClick(this::handleRequirementsClick);
        render.slot(1, 5)
                .withItem(this.buildRewardsItem(player, plugin, town))
                .onClick(this::handleRewardsClick);
        render.slot(1, 6)
                .withItem(this.buildAttemptItem(player, plugin, town))
                .onClick(this::handleAttemptLevelUpClick);
    }

    private @NotNull ItemStack buildInfoItem(
            final @NotNull Player player,
            final @Nullable RDT plugin,
            final @Nullable RTown town
    ) {
        if (town == null || plugin == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("info.unavailable.name", player).build().component())
                    .setLore(this.i18n("info.unavailable.lore", player).build().children())
                    .build();
        }

        final Integer nextLevel = TownLevelUpSupport.resolveNextLevel(plugin, town);
        final String nextLevelDisplay = nextLevel == null ? "-" : String.valueOf(nextLevel);
        return UnifiedBuilderFactory.item(Material.NETHER_STAR)
                .setName(this.i18n("info.name", player).build().component())
                .setLore(this.i18n("info.lore", player)
                        .withPlaceholders(Map.of(
                                "current_level", town.getTownLevel(),
                                "next_level", nextLevelDisplay
                        ))
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildRequirementsItem(
            final @NotNull Player player,
            final @Nullable RDT plugin,
            final @Nullable RTown town
    ) {
        if (town == null || plugin == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("requirements.unavailable.name", player).build().component())
                    .setLore(this.i18n("requirements.unavailable.lore", player).build().children())
                    .build();
        }

        final Integer nextLevel = TownLevelUpSupport.resolveNextLevel(plugin, town);
        final List<TownLevelUpSupport.ResolvedTownRequirement> requirements = nextLevel == null
                ? List.of()
                : TownLevelUpSupport.getConfiguredRequirements(plugin, player, town, nextLevel);
        return UnifiedBuilderFactory.item(Material.BOOK)
                .setName(this.i18n("requirements.name", player).build().component())
                .setLore(this.i18n("requirements.lore", player)
                        .withPlaceholders(Map.of(
                                "target_level", nextLevel == null ? "-" : nextLevel,
                                "requirement_count", requirements.size(),
                                "permission", TownPermissions.TOWN_LEVEL_UP.getPermissionKey()
                        ))
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildRewardsItem(
            final @NotNull Player player,
            final @Nullable RDT plugin,
            final @Nullable RTown town
    ) {
        if (town == null || plugin == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("rewards.unavailable.name", player).build().component())
                    .setLore(this.i18n("rewards.unavailable.lore", player).build().children())
                    .build();
        }

        final Integer nextLevel = TownLevelUpSupport.resolveNextLevel(plugin, town);
        final List<TownLevelUpSupport.ResolvedTownReward> rewards = nextLevel == null
                ? List.of()
                : TownLevelUpSupport.getConfiguredRewards(plugin, nextLevel);
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("rewards.name", player).build().component())
                .setLore(this.i18n("rewards.lore", player)
                        .withPlaceholders(Map.of(
                                "target_level", nextLevel == null ? "-" : nextLevel,
                                "reward_count", rewards.size()
                        ))
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildAttemptItem(
            final @NotNull Player player,
            final @Nullable RDT plugin,
            final @Nullable RTown town
    ) {
        if (town == null || plugin == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("level_up.unavailable.name", player).build().component())
                    .setLore(this.i18n("level_up.unavailable.lore", player).build().children())
                    .build();
        }

        final Integer nextLevel = TownLevelUpSupport.resolveNextLevel(plugin, town);
        if (nextLevel == null) {
            return UnifiedBuilderFactory.item(Material.LIME_STAINED_GLASS_PANE)
                    .setName(this.i18n("level_up.max_level.name", player).build().component())
                    .setLore(this.i18n("level_up.max_level.lore", player)
                            .withPlaceholder("current_level", town.getTownLevel())
                            .build()
                            .children())
                    .build();
        }

        final List<TownLevelUpSupport.ResolvedTownRequirement> requirements =
                TownLevelUpSupport.getConfiguredRequirements(plugin, player, town, nextLevel);
        final TownLevelUpSupport.RequirementAvailability availability =
                TownLevelUpSupport.resolveRequirementAvailability(plugin, player, town, requirements);
        final Material material = switch (availability) {
            case READY -> Material.EMERALD_BLOCK;
            case PENDING -> Material.CLOCK;
            case UNAVAILABLE -> Material.BARRIER;
        };
        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n("level_up.name", player).build().component())
                .setLore(this.i18n("level_up.lore", player)
                        .withPlaceholders(Map.of(
                                "current_level", town.getTownLevel(),
                                "next_level", nextLevel,
                                "requirement_status", this.resolveAvailabilityLabel(player, availability)
                        ))
                        .build()
                        .children())
                .build();
    }

    private boolean verifyViewerAccess(
            final @NotNull Context context,
            final @NotNull Player player,
            final @Nullable RTown town
    ) {
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(context, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_LEVEL_UP)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_LEVEL_UP.getPermissionKey())
                    .build()
                    .sendMessage();
            return false;
        }
        return true;
    }

    private void handleRequirementsClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        final RTown town = this.resolveTown(click, player);
        if (plugin == null || town == null || !this.verifyViewerAccess(click, player, town)) {
            return;
        }

        click.openForPlayer(
                TownLevelUpRequirementsView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private void handleRewardsClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        final RTown town = this.resolveTown(click, player);
        if (plugin == null || town == null || !this.verifyViewerAccess(click, player, town)) {
            return;
        }

        click.openForPlayer(
                TownLevelUpRewardsView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private void handleAttemptLevelUpClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        final RTown town = this.resolveTown(click, player);
        if (plugin == null || town == null || !this.verifyViewerAccess(click, player, town)) {
            return;
        }

        final TownLevelUpSupport.LevelUpResult result = TownLevelUpSupport.attemptLevelUp(plugin, player, town);
        switch (result.status()) {
            case SYSTEM_UNAVAILABLE -> this.i18n("level_up.error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            case MAX_LEVEL -> this.i18n("level_up.error.max_level", player)
                    .includePrefix()
                    .withPlaceholder("current_level", town.getTownLevel())
                    .build()
                    .sendMessage();
            case REQUIREMENT_UNAVAILABLE -> this.i18n("level_up.error.requirement_unavailable", player)
                    .includePrefix()
                    .withPlaceholder("requirement", result.failedRequirement())
                    .build()
                    .sendMessage();
            case REQUIREMENT_UNMET -> this.i18n("level_up.error.requirement_unmet", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "requirement", result.failedRequirement(),
                            "requirements", result.requirementSummary().isBlank() ? "-" : result.requirementSummary()
                    ))
                    .build()
                    .sendMessage();
            case CONSUME_FAILED -> this.i18n("level_up.error.consume_failed", player)
                    .includePrefix()
                    .withPlaceholder("requirement", result.failedRequirement())
                    .build()
                    .sendMessage();
            case SUCCESS -> this.i18n("level_up.message.success", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "old_level", result.previousLevel(),
                            "new_level", result.newLevel(),
                            "reward_count", result.grantedRewardCount(),
                            "requirements", result.requirementSummary().isBlank() ? "-" : result.requirementSummary(),
                            "rewards", result.rewardSummary().isBlank() ? "-" : result.rewardSummary()
                    ))
                    .build()
                    .sendMessage();
            case SUCCESS_WITH_REWARD_ERRORS -> this.i18n("level_up.message.success_with_reward_errors", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "old_level", result.previousLevel(),
                            "new_level", result.newLevel(),
                            "reward_count", result.grantedRewardCount(),
                            "failed_reward_count", result.failedRewardCount()
                    ))
                    .build()
                    .sendMessage();
        }

        click.openForPlayer(
                TownLevelUpView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private @Nullable RTown resolveTown(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return null;
        }

        final RRTown townRepository = plugin.getTownRepository();
        if (townRepository == null) {
            return null;
        }

        final UUID resolvedTownUuid = this.resolveTownUuid(context, player, plugin);
        if (resolvedTownUuid == null) {
            return null;
        }
        return townRepository.findByTownUUID(resolvedTownUuid);
    }

    private @Nullable UUID resolveTownUuid(
            final @NotNull Context context,
            final @NotNull Player player,
            final @NotNull RDT plugin
    ) {
        try {
            final UUID explicitTownUuid = this.townUuid.get(context);
            if (explicitTownUuid != null) {
                return explicitTownUuid;
            }
        } catch (final Exception ignored) {
        }

        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            return null;
        }

        final RDTPlayer rdtPlayer = playerRepository.findByPlayer(player.getUniqueId());
        return rdtPlayer == null ? null : rdtPlayer.getTownUUID();
    }

    private @Nullable RDTPlayer resolveTownPlayer(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getPlayerRepository() == null) {
            return null;
        }
        return plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @NotNull String resolveAvailabilityLabel(
            final @NotNull Player player,
            final @NotNull TownLevelUpSupport.RequirementAvailability availability
    ) {
        final String key = switch (availability) {
            case READY -> "level_up.state.ready";
            case PENDING -> "level_up.state.pending";
            case UNAVAILABLE -> "level_up.state.unavailable";
        };
        return PlainTextComponentSerializer.plainText().serialize(
                this.i18n(key, player).build().component()
        );
    }
}
