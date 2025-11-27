package com.raindropcentral.rdq.perk.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.api.PerkService;
import com.raindropcentral.rdq.perk.Perk;
import com.raindropcentral.rdq.perk.PerkEffect;
import com.raindropcentral.rdq.perk.PerkRequirement;
import com.raindropcentral.rdq.perk.PlayerPerkState;
import com.raindropcentral.rdq.perk.repository.PerkRepository;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Detail view for a single perk showing info, requirements, and actions.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class PerkDetailView extends BaseView {

    private static final Logger LOGGER = Logger.getLogger(PerkDetailView.class.getName());

    private final State<RDQCore> core = initialState("core");
    private final State<String> perkId = initialState("perkId");

    public PerkDetailView() {
        super(PerkListView.class);
    }

    @Override
    protected String getKey() {
        return "perk_detail_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            "  I R E  ",
            "         ",
            "    A    ",
            "         ",
            "b        "
        };
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final String id = this.perkId.get(openContext);
        return Map.of("perk_id", id != null ? id : "unknown");
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDQCore rdqCore = this.core.get(render);
        final String id = this.perkId.get(render);

        if (rdqCore == null || id == null) {
            player.closeInventory();
            return;
        }

        final PerkRepository perkRepository = rdqCore.getPerkRepository();
        final PerkService perkService = rdqCore.getPerkService();

        if (perkRepository == null || perkService == null) {
            player.closeInventory();
            return;
        }

        final Optional<Perk> perkOpt = perkRepository.findById(id);
        if (perkOpt.isEmpty()) {
            player.closeInventory();
            return;
        }

        final Perk perk = perkOpt.get();
        final PlayerPerkState state = perkService.getPlayerPerkState(player.getUniqueId(), id).join().orElse(null);

        renderInfoSlot(render, player, perk);
        renderRequirementsSlot(render, player, perk);
        renderEffectSlot(render, player, perk);
        renderActionSlot(render, player, perk, state, rdqCore, perkService);
    }

    private void renderInfoSlot(RenderContext render, Player player, Perk perk) {
        final Material material = parseMaterial(perk.iconMaterial());

        render.layoutSlot('I', UnifiedBuilderFactory
            .item(material)
            .setName(this.i18n("info.name", player)
                .with("perk_name", perk.displayNameKey())
                .build().component())
            .setLore(this.i18n("info.lore", player)
                .withAll(Map.of(
                    "description", perk.descriptionKey(),
                    "category", perk.category() != null ? perk.category() : "none",
                    "type", formatType(perk),
                    "cooldown", perk.cooldownSeconds() + "s",
                    "duration", perk.durationSeconds() + "s"
                ))
                .build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    private void renderRequirementsSlot(RenderContext render, Player player, Perk perk) {
        render.layoutSlot('R', UnifiedBuilderFactory
            .item(Material.BOOK)
            .setName(this.i18n("requirements.name", player).build().component())
            .setLore(this.i18n("requirements.lore", player)
                .with("requirements", formatRequirements(perk))
                .build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    private void renderEffectSlot(RenderContext render, Player player, Perk perk) {
        render.layoutSlot('E', UnifiedBuilderFactory
            .item(Material.BLAZE_POWDER)
            .setName(this.i18n("effect.name", player).build().component())
            .setLore(this.i18n("effect.lore", player)
                .with("effect", formatEffect(perk.effect()))
                .build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    private void renderActionSlot(RenderContext render, Player player, Perk perk, 
                                   PlayerPerkState state, RDQCore rdqCore, PerkService perkService) {
        final Material material;
        final String actionKey;

        if (state == null || !state.unlocked()) {
            material = Material.GRAY_DYE;
            actionKey = "action.unlock";
        } else if (state.active()) {
            material = Material.LIME_DYE;
            actionKey = "action.deactivate";
        } else if (state.isOnCooldown()) {
            material = Material.ORANGE_DYE;
            actionKey = "action.cooldown";
        } else {
            material = Material.GREEN_DYE;
            actionKey = "action.activate";
        }

        render.layoutSlot('A', UnifiedBuilderFactory
            .item(material)
            .setName(this.i18n(actionKey + ".name", player).build().component())
            .setLore(this.i18n(actionKey + ".lore", player)
                .with("remaining", state != null && state.isOnCooldown() 
                    ? formatDuration(state.remainingCooldown().orElse(Duration.ZERO)) 
                    : "")
                .build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(state != null && state.active())
            .build()
        ).onClick(ctx -> handleAction(ctx.getPlayer(), perk, state, perkService, render));
    }

    private void handleAction(Player player, Perk perk, PlayerPerkState state, 
                               PerkService perkService, RenderContext render) {
        if (state == null || !state.unlocked()) {
            perkService.unlockPerk(player.getUniqueId(), perk.id())
                .thenAccept(success -> {
                    if (success) {
                        this.i18n("message.unlocked", player)
                            .with("perk_name", perk.displayNameKey())
                            .send();
                    } else {
                        this.i18n("message.unlock_failed", player).send();
                    }
                    render.update();
                });
        } else if (state.active()) {
            perkService.deactivatePerk(player.getUniqueId(), perk.id())
                .thenAccept(success -> {
                    if (success) {
                        this.i18n("message.deactivated", player)
                            .with("perk_name", perk.displayNameKey())
                            .send();
                    }
                    render.update();
                });
        } else if (!state.isOnCooldown()) {
            perkService.activatePerk(player.getUniqueId(), perk.id())
                .thenAccept(success -> {
                    if (success) {
                        this.i18n("message.activated", player)
                            .with("perk_name", perk.displayNameKey())
                            .send();
                    } else {
                        this.i18n("message.activate_failed", player).send();
                    }
                    render.update();
                });
        }
    }

    private String formatType(Perk perk) {
        if (perk.isToggleable()) return "Toggleable";
        if (perk.isEventBased()) return "Event-Based";
        if (perk.isPassive()) return "Passive";
        return "Unknown";
    }

    private String formatRequirements(Perk perk) {
        if (perk.requirements().isEmpty()) return "None";
        return perk.requirements().stream()
            .map(this::formatRequirement)
            .reduce((a, b) -> a + "\n" + b)
            .orElse("None");
    }

    private String formatRequirement(PerkRequirement req) {
        return switch (req) {
            case PerkRequirement.RankRequired(var rankId) -> "Rank: " + rankId;
            case PerkRequirement.PermissionRequired(var perm) -> "Permission: " + perm;
            case PerkRequirement.CurrencyRequired(var currency, var amount) -> currency + ": " + amount;
            case PerkRequirement.LevelRequired(var level) -> "Level: " + level;
        };
    }

    private String formatEffect(PerkEffect effect) {
        return switch (effect) {
            case PerkEffect.PotionEffect(var type, var amp) -> "Potion: " + type + " " + (amp + 1);
            case PerkEffect.AttributeModifier(var attr, var val, var op) -> "Attribute: " + attr + " " + op + " " + val;
            case PerkEffect.Flight(var combat) -> "Flight" + (combat ? " (combat allowed)" : "");
            case PerkEffect.ExperienceMultiplier(var mult) -> "XP x" + mult;
            case PerkEffect.DeathPrevention(var health) -> "Death Prevention (restore " + health + " HP)";
            case PerkEffect.Custom(var handler, var config) -> "Custom: " + handler;
        };
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return minutes + "m " + remainingSeconds + "s";
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.NETHER_STAR;
        }
    }
}
