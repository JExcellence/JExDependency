package com.raindropcentral.rdq.perk.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.api.PerkService;
import com.raindropcentral.rdq.perk.Perk;
import com.raindropcentral.rdq.perk.PlayerPerkState;
import com.raindropcentral.rdq.perk.repository.PerkRepository;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Paginated view for displaying available perks.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class PerkListView extends APaginatedView<PerkListView.PerkDisplayItem> {

    private static final Logger LOGGER = Logger.getLogger(PerkListView.class.getName());

    private final State<RDQCore> core = initialState("core");
    private final MutableState<String> selectedCategory = mutableState("all");

    public PerkListView() {
        super(PerkMainView.class);
    }

    @Override
    protected String getKey() {
        return "perk_list_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final String category = this.selectedCategory.get(openContext);
        return Map.of("category", category != null ? category : "all");
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "         ",
            "b  <p>   "
        };
    }

    @Override
    protected CompletableFuture<List<PerkDisplayItem>> getAsyncPaginationSource(final @NotNull Context context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final RDQCore rdqCore = this.core.get(context);
                final Player player = context.getPlayer();
                final PerkRepository perkRepository = rdqCore.getPerkRepository();
                final PerkService perkService = rdqCore.getPerkService();

                if (perkRepository == null || perkService == null) {
                    return List.of();
                }

                final String category = this.selectedCategory.get(context);
                final List<Perk> perks;
                
                if ("all".equals(category) || category == null) {
                    perks = perkRepository.findEnabled();
                } else {
                    perks = perkRepository.findEnabledByCategory(category);
                }

                final Map<String, PlayerPerkState> playerStates = perkService
                    .getPlayerPerks(player.getUniqueId())
                    .join()
                    .stream()
                    .collect(Collectors.toMap(PlayerPerkState::perkId, s -> s));

                return perks.stream()
                    .map(perk -> new PerkDisplayItem(perk, playerStates.get(perk.id())))
                    .sorted(this::compareItems)
                    .collect(Collectors.toList());

            } catch (final Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading perk data", e);
                return List.of();
            }
        });
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull PerkDisplayItem item
    ) {
        final Player player = context.getPlayer();
        final Material material = parseMaterial(item.perk.iconMaterial());

        builder
            .withItem(
                UnifiedBuilderFactory
                    .item(material)
                    .setName(
                        this.i18n("perk_entry.name", player)
                            .with("perk_name", item.perk.displayNameKey())
                            .with("state_color", getStateColor(item.state))
                            .build()
                            .component()
                    )
                    .setLore(
                        this.i18n("perk_entry.lore", player)
                            .withAll(Map.of(
                                "category", item.perk.category() != null ? item.perk.category() : "none",
                                "state", getStateDisplay(item.state),
                                "cooldown", formatCooldown(item.state)
                            ))
                            .build()
                            .splitLines()
                    )
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setGlowing(item.state != null && item.state.active())
                    .build()
            )
            .onClick(clickContext -> handlePerkClick(context, player, item));
    }

    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        // Additional rendering if needed
    }

    private void handlePerkClick(@NotNull Context context, @NotNull Player player, @NotNull PerkDisplayItem item) {
        final RDQCore rdqCore = this.core.get(context);
        player.closeInventory();
        rdqCore.getViewFrame().open(
            PerkDetailView.class,
            player,
            Map.of("core", rdqCore, "perkId", item.perk.id())
        );
    }

    private int compareItems(@NotNull PerkDisplayItem a, @NotNull PerkDisplayItem b) {
        int stateA = getStateOrder(a.state);
        int stateB = getStateOrder(b.state);
        if (stateA != stateB) return Integer.compare(stateA, stateB);
        return a.perk.id().compareTo(b.perk.id());
    }

    private int getStateOrder(PlayerPerkState state) {
        if (state == null) return 4;
        if (state.active()) return 0;
        if (state.unlocked() && !state.isOnCooldown()) return 1;
        if (state.isOnCooldown()) return 2;
        return 3;
    }

    private String getStateColor(PlayerPerkState state) {
        if (state == null) return "<gray>";
        if (state.active()) return "<aqua>";
        if (state.unlocked()) return "<green>";
        return "<red>";
    }

    private String getStateDisplay(PlayerPerkState state) {
        if (state == null) return "Locked";
        if (state.active()) return "Active";
        if (state.isOnCooldown()) return "Cooldown";
        if (state.unlocked()) return "Available";
        return "Locked";
    }

    private String formatCooldown(PlayerPerkState state) {
        if (state == null || !state.isOnCooldown()) return "";
        return state.remainingCooldown()
            .map(this::formatDuration)
            .orElse("");
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes < 60) return minutes + "m " + remainingSeconds + "s";
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "h " + remainingMinutes + "m";
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.NETHER_STAR;
        }
    }

    /**
     * Display item wrapper for perks.
     */
    protected record PerkDisplayItem(Perk perk, PlayerPerkState state) {}
}
