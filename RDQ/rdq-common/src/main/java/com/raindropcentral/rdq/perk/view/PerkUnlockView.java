package com.raindropcentral.rdq.perk.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.api.PerkService;
import com.raindropcentral.rdq.perk.Perk;
import com.raindropcentral.rdq.perk.PerkRequirement;
import com.raindropcentral.rdq.perk.repository.PerkRepository;
import com.raindropcentral.rdq.perk.service.PerkRequirementChecker;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * View for unlocking a perk, showing requirements and unlock button.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class PerkUnlockView extends BaseView {

    private static final Logger LOGGER = Logger.getLogger(PerkUnlockView.class.getName());

    private final State<RDQCore> core = initialState("core");
    private final State<String> perkId = initialState("perkId");

    public PerkUnlockView() {
        super(PerkDetailView.class);
    }

    @Override
    protected String getKey() {
        return "perk_unlock_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            "  I   R  ",
            "         ",
            "    U    ",
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
        if (perkRepository == null) {
            player.closeInventory();
            return;
        }

        final Optional<Perk> perkOpt = perkRepository.findById(id);
        if (perkOpt.isEmpty()) {
            player.closeInventory();
            return;
        }

        final Perk perk = perkOpt.get();

        renderInfoSlot(render, player, perk);
        renderRequirementsSlot(render, player, perk, rdqCore);
        renderUnlockSlot(render, player, perk, rdqCore);
    }

    private void renderInfoSlot(RenderContext render, Player player, Perk perk) {
        final Material material = parseMaterial(perk.iconMaterial());

        render.layoutSlot('I', UnifiedBuilderFactory
            .item(material)
            .setName(this.i18n("info.name", player)
                .with("perk_name", perk.displayNameKey())
                .build().component())
            .setLore(this.i18n("info.lore", player)
                .with("description", perk.descriptionKey())
                .with("category", perk.category() != null ? perk.category() : "none")
                .build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    private void renderRequirementsSlot(RenderContext render, Player player, Perk perk, RDQCore rdqCore) {
        final StringBuilder reqBuilder = new StringBuilder();

        if (perk.requirements().isEmpty()) {
            reqBuilder.append("<green>✓ No requirements");
        } else {
            for (final PerkRequirement req : perk.requirements()) {
                // Check if requirement is met (simplified - would need actual checker)
                final String prefix = "<gray>• ";
                reqBuilder.append(prefix).append(formatRequirement(req)).append("\n");
            }
        }

        render.layoutSlot('R', UnifiedBuilderFactory
            .item(Material.PAPER)
            .setName(this.i18n("requirements.name", player).build().component())
            .setLore(this.i18n("requirements.lore", player)
                .with("requirements", reqBuilder.toString().trim())
                .build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    private void renderUnlockSlot(RenderContext render, Player player, Perk perk, RDQCore rdqCore) {
        // Simplified - assume requirements are met for now
        final boolean canUnlock = perk.requirements().isEmpty();

        final Material material = canUnlock ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        final String actionKey = canUnlock ? "unlock.available" : "unlock.unavailable";

        render.layoutSlot('U', UnifiedBuilderFactory
            .item(material)
            .setName(this.i18n(actionKey + ".name", player).build().component())
            .setLore(this.i18n(actionKey + ".lore", player).build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(canUnlock)
            .build()
        ).onClick(ctx -> {
            if (!canUnlock) {
                this.i18n("message.requirements_not_met", player).send();
                return;
            }

            final PerkService perkService = rdqCore.getPerkService();
            perkService.unlockPerk(player.getUniqueId(), perk.id())
                .thenAccept(success -> {
                    if (success) {
                        this.i18n("message.unlocked", player)
                            .with("perk_name", perk.displayNameKey())
                            .send();
                        // Navigate back to detail view
                        rdqCore.getViewFrame().open(
                            PerkDetailView.class,
                            player,
                            Map.of("core", rdqCore, "perkId", perk.id())
                        );
                    } else {
                        this.i18n("message.unlock_failed", player).send();
                    }
                });
        });
    }

    private String formatRequirement(PerkRequirement req) {
        return switch (req) {
            case PerkRequirement.RankRequired(var rankId) -> "Rank: " + rankId;
            case PerkRequirement.PermissionRequired(var perm) -> "Permission: " + perm;
            case PerkRequirement.CurrencyRequired(var currency, var amount) -> currency + ": " + amount;
            case PerkRequirement.LevelRequired(var level) -> "Level: " + level;
        };
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.NETHER_STAR;
        }
    }
}
