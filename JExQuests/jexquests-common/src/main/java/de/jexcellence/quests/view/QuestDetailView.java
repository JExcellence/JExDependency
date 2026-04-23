package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.BaseView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.Quest;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Single-quest detail view with accept / abandon / track buttons.
 * Back-navigates to {@link QuestOverviewView} via
 * {@link BaseView#BaseView(Class)}.
 */
public class QuestDetailView extends BaseView {

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<Quest> quest = initialState("quest");

    public QuestDetailView() {
        super(QuestOverviewView.class);
    }

    @Override
    protected String translationKey() {
        return "quest_detail_ui";
    }

    @Override
    protected String[] layout() {
        // q = summary, t = track, O = objectives (tasks + live progress),
        // W = rewards drill-down, R = requirements drill-down,
        // a = accept, b = abandon, r = back button.
        return new String[]{
                "         ",
                " q   t   ",
                "  O W R  ",
                " a     b ",
                "        r"
        };
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        final Quest q = this.quest.get(render);
        final JExQuests quests = this.plugin.get(render);

        renderSummary(render, player, q);
        renderTrack(render, player, q);
        renderObjectivesButton(render, player, q, quests);
        renderRewardsButton(render, player, q, quests);
        renderRequirementsButton(render, player, q, quests);
        renderAccept(render, player, q, quests);
        renderAbandon(render, player, q, quests);
    }

    private void renderObjectivesButton(@NotNull RenderContext render, @NotNull Player player,
                                         @NotNull Quest q, @NotNull JExQuests quests) {
        render.layoutSlot('O', createItem(
                org.bukkit.Material.WRITABLE_BOOK,
                i18n("objectives.name", player).build().component(),
                i18n("objectives.lore", player).build().children()
        )).onClick(click -> click.openForPlayer(QuestObjectiveListView.class, Map.of(
                "plugin", quests,
                "quest", q
        )));
    }

    private void renderRewardsButton(@NotNull RenderContext render, @NotNull Player player,
                                      @NotNull Quest q, @NotNull JExQuests quests) {
        render.layoutSlot('W', createItem(
                org.bukkit.Material.CHEST,
                i18n("rewards.name", player).build().component(),
                i18n("rewards.lore", player)
                        .withPlaceholder("rewards", RewardDescriber.describe(q.getRewardData()))
                        .build().children()
        )).onClick(click -> {
            if (q.getRewardData() == null || q.getRewardData().isBlank()) return;
            click.openForPlayer(RewardListView.class, Map.of(
                    "plugin", quests,
                    "rewardData", q.getRewardData(),
                    "titleContext", q.getDisplayName()
            ));
        });
    }

    private void renderRequirementsButton(@NotNull RenderContext render, @NotNull Player player,
                                           @NotNull Quest q, @NotNull JExQuests quests) {
        render.layoutSlot('R', createItem(
                org.bukkit.Material.BOOK,
                i18n("requirements.name", player).build().component(),
                i18n("requirements.lore", player)
                        .withPlaceholder("requirements", RequirementDescriber.describe(q.getRequirementData()))
                        .build().children()
        )).onClick(click -> {
            if (q.getRequirementData() == null || q.getRequirementData().isBlank()) return;
            click.openForPlayer(RequirementListView.class, Map.of(
                    "plugin", quests,
                    "requirementData", q.getRequirementData(),
                    "titleContext", q.getDisplayName()
            ));
        });
    }

    private void renderSummary(@NotNull RenderContext render, @NotNull Player player, @NotNull Quest q) {
        render.layoutSlot('q', createItem(
                Material.WRITABLE_BOOK,
                i18n("summary.name", player)
                        .withPlaceholder("quest_display_name", q.getDisplayName())
                        .build().component(),
                i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "quest_identifier", q.getIdentifier(),
                                "quest_category", q.getCategory(),
                                "quest_difficulty", q.getDifficulty().name(),
                                "quest_repeatable", Boolean.toString(q.isRepeatable()),
                                "quest_rewards", RewardDescriber.describe(q.getRewardData())
                        ))
                        .build().children()
        ));
    }

    private void renderTrack(@NotNull RenderContext render, @NotNull Player player, @NotNull Quest q) {
        // The track button previously had no click handler at all —
        // players saw a compass, clicked it, nothing happened. Now it
        // writes the quest's identifier into QuestsPlayer.trackedQuestIdentifier
        // and confirms in chat so the sidebar can pick it up.
        final JExQuests quests = this.plugin.get(render);
        render.layoutSlot('t', createItem(
                Material.COMPASS,
                i18n("track.name", player).build().component(),
                i18n("track.lore", player)
                        .withPlaceholder("quest_identifier", q.getIdentifier())
                        .build().children()
        )).onClick(click -> {
            quests.questsPlayerService().findAsync(player.getUniqueId()).thenAccept(opt -> {
                opt.ifPresent(profile -> {
                    profile.setTrackedQuestIdentifier(q.getIdentifier());
                    quests.questsPlayerService().repository().update(profile);
                });
                org.bukkit.Bukkit.getScheduler().runTask(quests.getPlugin(), () ->
                        de.jexcellence.jextranslate.R18nManager.getInstance()
                                .msg("quest.tracking-set").prefix()
                                .with("quest", q.getIdentifier())
                                .send(player));
            });
            click.closeForPlayer();
        });
    }

    private void renderAccept(
            @NotNull RenderContext render, @NotNull Player player,
            @NotNull Quest q, @NotNull JExQuests quests
    ) {
        render.layoutSlot('a', createItem(
                Material.LIME_DYE,
                i18n("accept.name", player).build().component(),
                i18n("accept.lore", player)
                        .withPlaceholder("quest_identifier", q.getIdentifier())
                        .build().children()
        )).onClick(click -> {
            quests.questService().acceptAsync(player.getUniqueId(), q.getIdentifier())
                    .thenAccept(result -> handleAcceptResult(quests, player, q, result));
            click.closeForPlayer();
        });
    }

    private void renderAbandon(
            @NotNull RenderContext render, @NotNull Player player,
            @NotNull Quest q, @NotNull JExQuests quests
    ) {
        render.layoutSlot('b', createItem(
                Material.RED_DYE,
                i18n("abandon.name", player).build().component(),
                i18n("abandon.lore", player)
                        .withPlaceholder("quest_identifier", q.getIdentifier())
                        .build().children()
        )).onClick(click -> {
            quests.questService().abandonAsync(player.getUniqueId(), q.getIdentifier())
                    .thenAccept(success -> org.bukkit.Bukkit.getScheduler().runTask(quests.getPlugin(), () -> {
                        final var r18n = de.jexcellence.jextranslate.R18nManager.getInstance();
                        if (Boolean.TRUE.equals(success)) {
                            r18n.msg("quest.abandoned").prefix().with("quest", q.getIdentifier()).send(player);
                        } else {
                            r18n.msg("quest.not-found").prefix().with("quest", q.getIdentifier()).send(player);
                        }
                    }));
            click.closeForPlayer();
        });
    }

    /**
     * Route every {@link de.jexcellence.quests.service.QuestService.AcceptResult}
     * back to the player — a successful accept also writes the quest id
     * into {@code trackedQuestIdentifier} so the sidebar starts showing
     * progress immediately without a separate /quest track command.
     */
    private static void handleAcceptResult(
            @NotNull JExQuests quests, @NotNull Player player, @NotNull Quest q,
            @NotNull de.jexcellence.quests.service.QuestService.AcceptResult result
    ) {
        final var r18n = de.jexcellence.jextranslate.R18nManager.getInstance();
        org.bukkit.Bukkit.getScheduler().runTask(quests.getPlugin(), () -> {
            switch (result) {
                case ACCEPTED -> {
                    r18n.msg("quest.accepted").prefix().with("quest", q.getIdentifier()).send(player);
                    // Auto-track the freshly-accepted quest so the sidebar
                    // lights up immediately — saves the player a second click.
                    quests.questsPlayerService().findAsync(player.getUniqueId()).thenAccept(opt ->
                            opt.ifPresent(profile -> {
                                profile.setTrackedQuestIdentifier(q.getIdentifier());
                                quests.questsPlayerService().repository().update(profile);
                            }));
                }
                case ALREADY_ACTIVE -> r18n.msg("quest.already-active").prefix()
                        .with("quest", q.getIdentifier()).send(player);
                case ALREADY_COMPLETED -> r18n.msg("quest.already-completed").prefix()
                        .with("quest", q.getIdentifier()).send(player);
                case REQUIREMENTS_NOT_MET -> r18n.msg("quest.locked").prefix()
                        .with("quest", q.getIdentifier())
                        .with("requirements", "see /quest info").send(player);
                case DISABLED, NOT_FOUND -> r18n.msg("quest.not-found").prefix()
                        .with("quest", q.getIdentifier()).send(player);
                case ERROR -> r18n.msg("error.unknown").prefix()
                        .with("error", "accept failed").send(player);
            }
        });
    }
}
