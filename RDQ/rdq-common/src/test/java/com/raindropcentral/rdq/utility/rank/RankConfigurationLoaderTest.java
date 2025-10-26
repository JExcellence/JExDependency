package com.raindropcentral.rdq.utility.rank;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.ranktree.RankTreeSection;
import com.raindropcentral.rdq.config.ranks.system.RankSystemSection;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.manager.RDQManager;
import com.raindropcentral.rdq.manager.bounty.BountyManager;
import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.manager.quest.QuestManager;
import com.raindropcentral.rdq.manager.rank.RankManager;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

final class RankConfigurationLoaderTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private RDQ rdq;
    private Path tempDirectory;

    @BeforeEach
    void setUp() throws Exception {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin("RankLoaderTest");
        this.tempDirectory = Files.createTempDirectory("rank-config-loader-test");
        assignDataFolder(this.plugin, this.tempDirectory);
        this.rdq = new TestRDQ(this.plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (this.server != null) {
                MockBukkit.unmock();
            }
        } finally {
            if (this.tempDirectory != null) {
                try (Stream<Path> paths = Files.walk(this.tempDirectory)) {
                    paths.sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                }
            }
        }
    }

    @Test
    void loadAllAsyncLoadsValidConfiguration() throws Exception {
        writeValidSystemConfig(this.tempDirectory);
        writeTreeConfig(this.tempDirectory, "cleric.yml");
        writeTreeConfigWithCustomName(this.tempDirectory, "Custom-Path.yml");

        final RankConfigurationLoader loader = new RankConfigurationLoader(this.rdq);
        final Executor directExecutor = Runnable::run;

        final RankSystemState state = loader.loadAllAsync(directExecutor).get(1, TimeUnit.SECONDS);

        final RankSystemSection systemSection = state.rankSystemSection();
        assertNotNull(systemSection);
        assertTrue(systemSection.getSettings().getEnableRankSystem());

        final Map<String, RankTreeSection> trees = state.rankTreeSections();
        assertEquals(2, trees.size());
        assertTrue(trees.containsKey("cleric"));
        assertTrue(trees.containsKey("custom_path"));

        final Map<String, Map<String, RankSection>> ranks = state.rankSections();
        assertEquals(2, ranks.size());
        assertTrue(ranks.containsKey("cleric"));
        assertTrue(ranks.containsKey("custom_path"));

        final RankSection novice = ranks.get("cleric").get("novice");
        assertNotNull(novice);
        assertEquals("rank.cleric.novice.name", novice.getDisplayNameKey());
        assertEquals("rank.cleric.novice.lore", novice.getDescriptionKey());
        assertEquals("cleric_novice", novice.getLuckPermsGroup());

        final RankSection explorer = ranks.get("custom_path").get("explorer");
        assertNotNull(explorer);
        assertEquals("rank.custom_path.explorer.name", explorer.getDisplayNameKey());
        assertEquals("rank.custom_path.explorer.lore", explorer.getDescriptionKey());
    }

    @Test
    void loadAllAsyncHandlesMissingAndMalformedConfigsGracefully() throws Exception {
        // Do not create any configuration files to trigger fallback behaviour.
        Files.createDirectories(this.tempDirectory.resolve("rank"));
        final RankConfigurationLoader loader = new RankConfigurationLoader(this.rdq);
        final RankSystemState emptyState = loader.loadAllAsync(Runnable::run).get(1, TimeUnit.SECONDS);

        assertNotNull(emptyState.rankSystemSection());
        assertTrue(emptyState.rankTreeSections().isEmpty());
        assertTrue(emptyState.rankSections().isEmpty());
        assertTrue(Files.exists(this.tempDirectory.resolve("rank").resolve("paths")));

        // Now add malformed files that should be skipped without throwing.
        Files.createDirectories(this.tempDirectory.resolve("rank").resolve("paths"));
        Files.writeString(
                this.tempDirectory.resolve("rank").resolve("rank-system.yml"),
                "invalid: [",
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        Files.writeString(
                this.tempDirectory.resolve("rank").resolve("paths").resolve("cleric.yml"),
                "invalid: [",
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        final RankSystemState malformedState = loader.loadAllAsync(Runnable::run).get(1, TimeUnit.SECONDS);
        assertNotNull(malformedState.rankSystemSection());
        assertTrue(malformedState.rankTreeSections().isEmpty());
        assertTrue(malformedState.rankSections().isEmpty());
    }

    @Test
    void normalizeProducesLowercaseIdentifiersWithoutSeparators() throws Exception {
        writeValidSystemConfig(this.tempDirectory);
        writeTreeConfigWithCustomName(this.tempDirectory, "Space Path.yml");

        final RankConfigurationLoader loader = new RankConfigurationLoader(this.rdq);
        final RankSystemState state = loader.loadAllAsync(Runnable::run).get(1, TimeUnit.SECONDS);

        final Map<String, RankTreeSection> trees = state.rankTreeSections();
        assertTrue(trees.containsKey("spacepath"));
        assertFalse(trees.containsKey("Space Path"));
    }

    private static void writeValidSystemConfig(final Path directory) throws IOException {
        final Path rankDirectory = directory.resolve("rank");
        Files.createDirectories(rankDirectory);
        final String yaml = String.join("\n",
                "settings:",
                "  enableRankSystem: true",
                "defaultRank:",
                "  luckPermsGroup: \"default\"",
                "progressionRule:",
                "  requireLinearRankProgression: true"
        );
        Files.writeString(rankDirectory.resolve("rank-system.yml"), yaml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void writeTreeConfig(final Path directory, final String fileName) throws IOException {
        final Path treeDirectory = directory.resolve("rank").resolve("paths");
        Files.createDirectories(treeDirectory);
        final String yaml = String.join("\n",
                "displayOrder: 1",
                "isEnabled: true",
                "ranks:",
                "  novice:",
                "    tier: 1",
                "    weight: 5",
                "    luckPermsGroup: \"cleric_novice\"",
                "    enabled: true",
                "    isInitialRank: true",
                "    nextRanks: [\"acolyte\"]",
                "  acolyte:",
                "    tier: 2",
                "    weight: 10",
                "    luckPermsGroup: \"cleric_acolyte\"",
                "    enabled: true",
                "    previousRanks: [\"novice\"]"
        );
        Files.writeString(treeDirectory.resolve(fileName), yaml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void writeTreeConfigWithCustomName(final Path directory, final String fileName) throws IOException {
        final Path treeDirectory = directory.resolve("rank").resolve("paths");
        Files.createDirectories(treeDirectory);
        final String yaml = String.join("\n",
                "displayOrder: 2",
                "isEnabled: true",
                "ranks:",
                "  explorer:",
                "    tier: 1",
                "    weight: 7",
                "    luckPermsGroup: \"custom_explorer\"",
                "    enabled: true",
                "    isInitialRank: true"
        );
        Files.writeString(treeDirectory.resolve(fileName), yaml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void assignDataFolder(final JavaPlugin plugin, final Path directory) throws Exception {
        final Field dataFolderField = JavaPlugin.class.getDeclaredField("dataFolder");
        dataFolderField.setAccessible(true);
        dataFolderField.set(plugin, directory.toFile());
    }

    private static final class TestRDQ extends RDQ {

        TestRDQ(final @NotNull JavaPlugin plugin) {
            super(plugin, "TEST");
        }

        @Override
        protected @NotNull String getStartupMessage() {
            return "";
        }

        @Override
        protected int getMetricsId() {
            return 0;
        }

        @Override
        protected @NotNull ViewFrame registerViews(@NotNull ViewFrame viewFrame) {
            return viewFrame;
        }

        @Override
        protected @NotNull RDQManager initializeManager() {
            return new NoOpRDQManager();
        }
    }

    private static final class NoOpRDQManager extends RDQManager {

        NoOpRDQManager() {
            super("TEST");
        }

        @Override
        public @NotNull BountyManager getBountyManager() {
            return new NoOpBountyManager();
        }

        @Override
        public @NotNull QuestManager getQuestManager() {
            return new QuestManager() {
            };
        }

        @Override
        public @NotNull RankManager getRankManager() {
            return new RankManager() {
            };
        }

        @Override
        public @NotNull PerkManager getPerkManager() {
            return new PerkManager() {
            };
        }

        @Override
        public boolean isPremium() {
            return false;
        }

        @Override
        public void initialize() {
        }

        @Override
        public void shutdown() {
        }
    }

    private static final class NoOpBountyManager implements BountyManager {

        @Override
        public void createBounty(@NotNull RDQPlayer targetPlayer, @NotNull Player commissioner, @NotNull Set<RewardItem> rewardItems, @NotNull Map<String, Double> rewardCurrencies) {
        }

        @Override
        public void removeBounty(@NotNull UUID targetUniqueId) {
        }

        @Override
        public void trackDamage(@NotNull UUID targetUniqueId, @NotNull UUID attackerUniqueId, double damage) {
        }

        @Override
        public void handleBountyKill(@NotNull Player killedPlayer) {
        }

        @Override
        public @NotNull RBounty addItemRewards(@NotNull RBounty bounty, @NotNull List<ItemStack> items) {
            return bounty;
        }

        @Override
        public @NotNull RBounty addCurrencyReward(@NotNull RBounty bounty, @NotNull String currencyName, double amount) {
            return bounty;
        }

        @Override
        public void updateBountyPlayerDisplay(@NotNull UUID playerUniqueId) {
        }

        @Override
        public boolean hasActiveBounty(@NotNull UUID playerUniqueId) {
            return false;
        }

        @Override
        public @Nullable RBounty getBounty(@NotNull UUID playerUniqueId) {
            return null;
        }

        @Override
        public void giveRewardItemsToPlayer(@NotNull Player player, @NotNull Set<RewardItem> rewardItems) {
        }
    }
}
