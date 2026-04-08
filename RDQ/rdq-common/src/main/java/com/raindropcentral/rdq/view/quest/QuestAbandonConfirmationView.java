package com.raindropcentral.rdq.view.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.service.quest.QuestService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Confirmation dialog for quest abandonment.
 * <p>
 * This view provides a confirmation interface before abandoning a quest,
 * helping prevent accidental quest abandonment and providing clear information
 * about what will be lost.
 *
 * <h2>Features:</h2>
 * <ul>
 *     <li>Clear confirmation interface</li>
 *     <li>Quest information display</li>
 *     <li>Progress loss warning</li>
 *     <li>Confirm/Cancel options</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public class QuestAbandonConfirmationView extends BaseView {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    private final State<RDQ> rdq = initialState("plugin");
    private final State<Quest> quest = initialState("quest");
    
    private static final int QUEST_INFO_SLOT = 4;
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;
    
    public QuestAbandonConfirmationView() {
        super(QuestDetailView.class);
    }
    
    @Override
    protected String getKey() {
        return "view.quest.abandon_confirmation";
    }
    
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        final Quest q = quest.get(open);
        return Map.of("quest", q != null ? q.getIdentifier() : "Unknown");
    }
    
    @Override
    protected int getSize() {
        return 3; // Small confirmation dialog
    }
    
    @Override
    protected @NotNull String[] getLayout() {
        return new String[]{
                "XXXXXXXXX",
                "X C   C X",
                "XXXXXXXXX"
        };
    }
    
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final Quest q = quest.get(render);
        final RDQ plugin = rdq.get(render);
        
        if (q == null) {
            renderErrorState(render, player);
            return;
        }
        
        renderQuestInfo(render, player, q);
        renderConfirmButton(render, player, q, plugin);
        renderCancelButton(render, player);
    }
    
    private void renderErrorState(final @NotNull RenderContext render, final @NotNull Player player) {
        render.slot(QUEST_INFO_SLOT).renderWith(() -> {
            final Component errorName = new I18n.Builder("quest.general.error", player).build().component();
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(errorName)
                    .build();
        });
    }
    
    private void renderQuestInfo(final @NotNull RenderContext render, final @NotNull Player player, final @NotNull Quest q) {
        render.slot(QUEST_INFO_SLOT).renderWith(() -> {
            final Component name = new I18n.Builder(q.getIcon().getDisplayNameKey(), player).build().component();
            final List<Component> lore = new I18n.Builder("view.quest.abandon_confirmation.quest_info.lore", player)
                    .withPlaceholder("quest", q.getIdentifier())
                    .build().children();
            
            // Use a default material since Quest doesn't have a material field
            return UnifiedBuilderFactory.item(Material.PAPER)
                    .setName(name)
                    .setLore(lore)
                    .build();
        });
    }
    
    private void renderConfirmButton(final @NotNull RenderContext render, final @NotNull Player player, 
                                   final @NotNull Quest q, final @NotNull RDQ plugin) {
        render.layoutSlot('C').renderWith(() -> {
            final Component name = new I18n.Builder("view.quest.abandon_confirmation.confirm.name", player).build().component();
            final List<Component> lore = new I18n.Builder("view.quest.abandon_confirmation.confirm.lore", player)
                    .withPlaceholder("quest", q.getIdentifier())
                    .build().children();
            
            return UnifiedBuilderFactory.item(Material.GREEN_WOOL)
                    .setName(name)
                    .setLore(lore)
                    .build();
        }).onClick(click -> handleConfirmAbandon(click, q, plugin));
    }
    
    private void renderCancelButton(final @NotNull RenderContext render, final @NotNull Player player) {
        render.layoutSlot('C').renderWith(() -> {
            final Component name = new I18n.Builder("view.quest.abandon_confirmation.cancel.name", player).build().component();
            final List<Component> lore = new I18n.Builder("view.quest.abandon_confirmation.cancel.lore", player).build().children();
            
            return UnifiedBuilderFactory.item(Material.RED_WOOL)
                    .setName(name)
                    .setLore(lore)
                    .build();
        }).onClick(this::handleCancel);
    }
    
    private void handleConfirmAbandon(final @NotNull SlotClickContext click, final @NotNull Quest q, final @NotNull RDQ plugin) {
        final Player player = click.getPlayer();
        final QuestService questService = plugin.getQuestService();
        
        // Close confirmation dialog
        click.closeForPlayer();
        
        // Perform the abandonment
        questService.abandonQuest(player.getUniqueId(), q.getIdentifier())
                .thenAccept(result -> {
                    if (result.success()) {
                        new I18n.Builder("quest.notification.abandoned", player)
                                .withPlaceholder("quest", q.getIdentifier())
                                .build()
                                .sendMessage();
                    } else {
                        new I18n.Builder("quest.command.abandon.failed", player)
                                .withPlaceholder("reason", result.getMessage())
                                .build()
                                .sendMessage();
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error abandoning quest", ex);
                    new I18n.Builder("quest.general.error", player).build().sendMessage();
                    return null;
                });
    }
    
    private void handleCancel(final @NotNull SlotClickContext click) {
        // Just close the confirmation dialog, returning to the quest detail view
        click.closeForPlayer();
    }
}