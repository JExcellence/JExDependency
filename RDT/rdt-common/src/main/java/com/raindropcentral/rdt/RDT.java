/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdt;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rdt.configs.*;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRNation;
import com.raindropcentral.rdt.database.repository.RRNationInvite;
import com.raindropcentral.rdt.database.repository.RRServerBank;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.database.repository.RRTownChunk;
import com.raindropcentral.rdt.database.repository.RRTownInvite;
import com.raindropcentral.rdt.database.repository.RRTownRelationship;
import com.raindropcentral.rdt.requirement.RDTRequirementProvider;
import com.raindropcentral.rdt.service.*;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.view.main.TownHubView;
import com.raindropcentral.rdt.view.town.*;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.proxy.NoOpProxyService;
import com.raindropcentral.rplatform.proxy.ProxyService;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import jakarta.persistence.EntityManagerFactory;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared runtime bootstrap for RDT editions.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class RDT {

    private static final String CONFIG_FOLDER_PATH = "config";
    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final String NEXUS_CONFIG_FILE_NAME = "nexus.yml";
    private static final String NATION_CONFIG_FILE_NAME = "nation.yml";
    private static final String SECURITY_CONFIG_FILE_NAME = "security.yml";
    private static final String BANK_CONFIG_FILE_NAME = "bank.yml";
    private static final String TAX_CONFIG_FILE_NAME = "tax.yml";
    private static final String FARM_CONFIG_FILE_NAME = "farm.yml";
    private static final String FOB_CONFIG_FILE_NAME = "fob.yml";
    private static final String OUTPOST_CONFIG_FILE_NAME = "outpost.yml";
    private static final String MEDIC_CONFIG_FILE_NAME = "medic.yml";
    private static final String ARMORY_CONFIG_FILE_NAME = "armory.yml";
    public static final String TOWN_BOSS_BAR_PROVIDER_KEY = "rdt.town";

    private final JavaPlugin plugin;
    private final String edition;
    private final TownService townService;

    private ExecutorService executor;
    private RPlatform platform;
    private EntityManagerFactory entityManagerFactory;
    private ISchedulerAdapter scheduler;
    private PlatformType platformType;
    private ProxyService proxyService;
    private volatile CompletableFuture<Void> enableFuture;

    private Object economyInstance;
    private ViewFrame viewFrame;

    private RRDTPlayer playerRepository;
    private RRTown townRepository;
    private RRTownChunk townChunkRepository;
    private RRTownInvite townInviteRepository;
    private RRTownRelationship townRelationshipRepository;
    private RRNation nationRepository;
    private RRNationInvite nationInviteRepository;
    private RRServerBank serverBankRepository;

    private TownRuntimeService townRuntimeService;
    private TownSpawnService townSpawnService;
    private TownFobService townFobService;
    private TownBossBarService townBossBarService;
    private TownBossBarIntegration townBossBarIntegration;
    private TownBankService townBankService;
    private NationBankService nationBankService;
    private ServerBankService serverBankService;
    private TaxRuntimeService taxRuntimeService;
    private TownFarmService townFarmService;
    private TownFuelService townFuelService;
    private TownMedicService townMedicService;
    private TownArmoryService townArmoryService;
    private NexusAccessService nexusAccessService;
    private RDTRequirementProvider requirementProvider;

    /**
     * Creates a shared RDT runtime for a specific edition.
     *
     * @param plugin owning Bukkit plugin
     * @param edition edition label used in logs
     * @param townService edition-specific town rules
     */
    public RDT(
        final @NotNull JavaPlugin plugin,
        final @NotNull String edition,
        final @NotNull TownService townService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.edition = Objects.requireNonNull(edition, "edition");
        this.townService = Objects.requireNonNull(townService, "townService");
    }

    /**
     * Allocates shared platform state before plugin enable.
     */
    public void onLoad() {
        this.getLogger().info("Loading RPlatform for RDT (" + this.edition + ")");
        this.platform = new RPlatform(this.plugin);
        this.executor = Executors.newFixedThreadPool(4);
    }

    /**
     * Initializes repositories, services, commands, listeners, and views.
     */
    public void onEnable() {
        if (this.enableFuture != null && !this.enableFuture.isDone()) {
            this.getLogger().warning("Enable sequence already in progress for RDT (" + this.edition + ")");
            return;
        }

        this.getLogger().info("Enabling RDT (" + this.edition + ") Edition");
        this.enableFuture = this.platform.initialize()
            .thenCompose(ignored -> this.runSync(() -> {
                this.entityManagerFactory = this.requireEntityManagerFactory();
                this.scheduler = this.platform.getScheduler();
                this.platformType = this.platform.getPlatformType();
                this.proxyService = this.platform.getProxyService();
                this.ensureBundledConfigFiles();
                this.initializeRepositories();
                this.initializeServices();
                this.initializeRequirementProvider();
                this.initializePlugins();
                this.initializeViews();
                this.initializeCommands();
                this.startRuntimeServices();
                this.getLogger().info("RDT (" + this.edition + ") Edition enabled successfully");
            }))
            .exceptionally(throwable -> {
                this.runSync(() -> {
                    this.getLogger().log(Level.SEVERE, "Failed to initialize RDT (" + this.edition + ")", throwable);
                    this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                });
                return null;
            });
    }

    /**
     * Shuts down runtime services and releases resources.
     */
    public void onDisable() {
        this.getLogger().info("Disabling RDT (" + this.edition + ")");

        if (this.townBossBarService != null) {
            this.townBossBarService.shutdown();
        }
        if (this.townBossBarIntegration != null) {
            this.townBossBarIntegration.unregister();
            this.townBossBarIntegration = null;
        }
        if (this.townBankService != null) {
            this.townBankService.shutdown();
        }
        if (this.nationBankService != null) {
            this.nationBankService.shutdown();
        }
        if (this.serverBankService != null) {
            this.serverBankService.shutdown();
        }
        if (this.taxRuntimeService != null) {
            this.taxRuntimeService.shutdown();
        }
        if (this.townFuelService != null) {
            this.townFuelService.shutdown();
        }
        if (this.townMedicService != null) {
            this.townMedicService.shutdown();
        }
        if (this.requirementProvider != null) {
            this.requirementProvider.unregister();
            this.requirementProvider = null;
        }
        this.proxyService = NoOpProxyService.createDefault();

        if (this.executor != null) {
            this.executor.shutdownNow();
        }

        if (this.entityManagerFactory != null) {
            try {
                this.entityManagerFactory.close();
            } catch (final Exception ignored) {
            }
        }
    }

    /**
     * Loads the effective plugin configuration.
     *
     * @return parsed plugin configuration
     */
    public @NotNull ConfigSection getDefaultConfig() {
        this.ensureBundledConfigFiles();
        try {
            final ConfigSection config;
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + CONFIG_FILE_NAME);
                config = defaultStream == null ? ConfigSection.createDefault() : ConfigSection.fromInputStream(defaultStream);
            } else {
                config = ConfigSection.fromFile(this.getConfigFile(CONFIG_FILE_NAME));
            }
            if (config.alignNationUnlockLevelWithRelationships()) {
                this.getLogger().warning(
                    "Configured town.nation_unlock_level was lower than town.relationship_unlock_level. "
                        + "Nation unlock has been raised to match the relationship unlock level."
                );
            }
            return config;
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT config: " + exception.getMessage());
            return ConfigSection.createDefault();
        }
    }

    /**
     * Loads the effective Nexus level configuration.
     *
     * @return parsed Nexus level configuration
     */
    public @NotNull NexusConfigSection getNexusConfig() {
        this.ensureBundledConfigFiles();
        try {
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + NEXUS_CONFIG_FILE_NAME);
                return defaultStream == null ? NexusConfigSection.createDefault() : NexusConfigSection.fromInputStream(defaultStream);
            }
            return NexusConfigSection.fromFile(this.getConfigFile(NEXUS_CONFIG_FILE_NAME));
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT nexus config: " + exception.getMessage());
            return NexusConfigSection.createDefault();
        }
    }

    /**
     * Loads the effective nation creation configuration.
     *
     * @return parsed nation configuration
     */
    public @NotNull NationConfigSection getNationConfig() {
        this.ensureBundledConfigFiles();
        try {
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + NATION_CONFIG_FILE_NAME);
                return defaultStream == null ? NationConfigSection.createDefault() : NationConfigSection.fromInputStream(defaultStream);
            }
            return NationConfigSection.fromFile(this.getConfigFile(NATION_CONFIG_FILE_NAME));
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT nation config: " + exception.getMessage());
            return NationConfigSection.createDefault();
        }
    }

    /**
     * Loads the effective Security chunk level configuration.
     *
     * @return parsed Security level configuration
     */
    public @NotNull SecurityConfigSection getSecurityConfig() {
        this.ensureBundledConfigFiles();
        try {
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + SECURITY_CONFIG_FILE_NAME);
                return defaultStream == null
                    ? SecurityConfigSection.createDefault()
                    : SecurityConfigSection.fromInputStream(defaultStream);
            }
            return SecurityConfigSection.fromFile(this.getConfigFile(SECURITY_CONFIG_FILE_NAME));
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT security config: " + exception.getMessage());
            return SecurityConfigSection.createDefault();
        }
    }

    /**
     * Loads the effective Bank chunk level configuration.
     *
     * @return parsed Bank level configuration
     */
    public @NotNull BankConfigSection getBankConfig() {
        this.ensureBundledConfigFiles();
        try {
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + BANK_CONFIG_FILE_NAME);
                return defaultStream == null ? BankConfigSection.createDefault() : BankConfigSection.fromInputStream(defaultStream);
            }
            return BankConfigSection.fromFile(this.getConfigFile(BANK_CONFIG_FILE_NAME));
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT bank config: " + exception.getMessage());
            return BankConfigSection.createDefault();
        }
    }

    /**
     * Loads the effective scheduled tax configuration.
     *
     * @return parsed tax configuration
     */
    public @NotNull TaxConfigSection getTaxConfig() {
        this.ensureBundledConfigFiles();
        try {
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + TAX_CONFIG_FILE_NAME);
                return defaultStream == null ? TaxConfigSection.createDefault() : TaxConfigSection.fromInputStream(defaultStream);
            }
            return TaxConfigSection.fromFile(this.getConfigFile(TAX_CONFIG_FILE_NAME));
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT tax config: " + exception.getMessage());
            return TaxConfigSection.createDefault();
        }
    }

    /**
     * Loads the effective Farm chunk level configuration.
     *
     * @return parsed Farm level configuration
     */
    public @NotNull FarmConfigSection getFarmConfig() {
        this.ensureBundledConfigFiles();
        try {
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + FARM_CONFIG_FILE_NAME);
                return defaultStream == null ? FarmConfigSection.createDefault() : FarmConfigSection.fromInputStream(defaultStream);
            }
            return FarmConfigSection.fromFile(this.getConfigFile(FARM_CONFIG_FILE_NAME));
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT farm config: " + exception.getMessage());
            return FarmConfigSection.createDefault();
        }
    }

    /**
     * Loads the effective FOB chunk level configuration.
     *
     * @return parsed FOB level configuration
     */
    public @NotNull FobConfigSection getFobConfig() {
        this.ensureBundledConfigFiles();
        try {
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + FOB_CONFIG_FILE_NAME);
                return defaultStream == null ? FobConfigSection.createDefault() : FobConfigSection.fromInputStream(defaultStream);
            }
            return FobConfigSection.fromFile(this.getConfigFile(FOB_CONFIG_FILE_NAME));
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT fob config: " + exception.getMessage());
            return FobConfigSection.createDefault();
        }
    }

    /**
     * Loads the effective Outpost chunk level configuration.
     *
     * @return parsed Outpost level configuration
     */
    public @NotNull OutpostConfigSection getOutpostConfig() {
        this.ensureBundledConfigFiles();
        try {
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + OUTPOST_CONFIG_FILE_NAME);
                return defaultStream == null
                    ? OutpostConfigSection.createDefault()
                    : OutpostConfigSection.fromInputStream(defaultStream);
            }
            return OutpostConfigSection.fromFile(this.getConfigFile(OUTPOST_CONFIG_FILE_NAME));
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT outpost config: " + exception.getMessage());
            return OutpostConfigSection.createDefault();
        }
    }

    /**
     * Loads the effective Medic chunk level configuration.
     *
     * @return parsed Medic level configuration
     */
    public @NotNull MedicConfigSection getMedicConfig() {
        this.ensureBundledConfigFiles();
        try {
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + MEDIC_CONFIG_FILE_NAME);
                return defaultStream == null ? MedicConfigSection.createDefault() : MedicConfigSection.fromInputStream(defaultStream);
            }
            return MedicConfigSection.fromFile(this.getConfigFile(MEDIC_CONFIG_FILE_NAME));
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT medic config: " + exception.getMessage());
            return MedicConfigSection.createDefault();
        }
    }

    /**
     * Loads the effective Armory chunk level configuration.
     *
     * @return parsed Armory level configuration
     */
    public @NotNull ArmoryConfigSection getArmoryConfig() {
        this.ensureBundledConfigFiles();
        try {
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + ARMORY_CONFIG_FILE_NAME);
                return defaultStream == null
                    ? ArmoryConfigSection.createDefault()
                    : ArmoryConfigSection.fromInputStream(defaultStream);
            }
            return ArmoryConfigSection.fromFile(this.getConfigFile(ARMORY_CONFIG_FILE_NAME));
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT armory config: " + exception.getMessage());
            return ArmoryConfigSection.createDefault();
        }
    }

    /**
     * Resolves the display and marker-block material for one chunk type.
     *
     * @param chunkType chunk type to resolve
     * @return resolved material for views and marker syncing
     */
    public @NotNull Material getChunkTypeDisplayMaterial(final @Nullable ChunkType chunkType) {
        if (chunkType == null) {
            return this.getDefaultConfig().getDefaultChunkBlockMaterial();
        }
        return switch (chunkType) {
            case NEXUS -> this.getDefaultConfig().getChunkTypeIconMaterial(ChunkType.NEXUS);
            case DEFAULT -> this.getDefaultConfig().getDefaultChunkBlockMaterial();
            case FOB -> this.getFobConfig().getBlockMaterial();
            case CLAIM_PENDING -> this.getDefaultConfig().getChunkTypeIconMaterial(ChunkType.CLAIM_PENDING);
            case SECURITY -> this.getSecurityConfig().getBlockMaterial();
            case BANK -> this.getBankConfig().getBlockMaterial();
            case FARM -> this.getFarmConfig().getBlockMaterial();
            case OUTPOST -> this.getOutpostConfig().getBlockMaterial();
            case MEDIC -> this.getMedicConfig().getBlockMaterial();
            case ARMORY -> this.getArmoryConfig().getBlockMaterial();
        };
    }

    /**
     * Returns whether the current edition may change config-driven behavior at runtime.
     *
     * @return {@code true} when config changes are permitted
     */
    public boolean canChangeConfigs() {
        return this.townService.canChangeConfigs();
    }

    /**
     * Returns whether the current edition is premium.
     *
     * @return {@code true} when premium features are available
     */
    public boolean isPremium() {
        return this.townService.isPremium();
    }

    private void initializeRepositories() {
        this.playerRepository = new RRDTPlayer(this.executor, this.entityManagerFactory, RDTPlayer.class, RDTPlayer::getIdentifier);
        this.townRepository = new RRTown(this.executor, this.entityManagerFactory, RTown.class, RTown::getTownUUID);
        this.townChunkRepository = new RRTownChunk(this.executor, this.entityManagerFactory);
        this.townInviteRepository = new RRTownInvite(this.executor, this.entityManagerFactory);
        this.townRelationshipRepository = new RRTownRelationship(this.executor, this.entityManagerFactory);
        this.nationRepository = new RRNation(this.executor, this.entityManagerFactory);
        this.nationInviteRepository = new RRNationInvite(this.executor, this.entityManagerFactory);
        this.serverBankRepository = new RRServerBank(this.executor, this.entityManagerFactory);
    }

    private void initializeServices() {
        this.nexusAccessService = new NexusAccessService(this);
        this.townRuntimeService = new TownRuntimeService(this);
        this.townSpawnService = new TownSpawnService(this);
        this.townFobService = new TownFobService(this);
        this.townBossBarService = new TownBossBarService(this);
        this.townBankService = new TownBankService(this);
        this.nationBankService = new NationBankService(this);
        this.serverBankService = new ServerBankService(this);
        this.taxRuntimeService = new TaxRuntimeService(this);
        this.townFarmService = new TownFarmService(this);
        this.townFuelService = new TownFuelService(this);
        this.townMedicService = new TownMedicService(this);
        this.townArmoryService = new TownArmoryService(this);
    }

    private void initializeRequirementProvider() {
        this.requirementProvider = new RDTRequirementProvider(this);
        this.requirementProvider.register();
    }

    private void initializePlugins() {
        this.initializeBossBarIntegration();
        new com.raindropcentral.rplatform.service.ServiceRegistry().register(
            "net.milkbowl.vault.economy.Economy",
            "TheNewEconomy"
        ).optional().maxAttempts(30).retryDelay(1000).onSuccess(economy -> this.economyInstance = economy).load();
    }

    private void initializeBossBarIntegration() {
        if (!this.plugin.getServer().getPluginManager().isPluginEnabled("RCore")) {
            this.townBossBarIntegration = null;
            return;
        }

        try {
            final TownBossBarIntegration integration = new TownBossBarIntegration(this);
            if (integration.register()) {
                this.townBossBarIntegration = integration;
                this.getLogger().info("Registered RDT town boss bar with RCore");
                return;
            }
        } catch (final Throwable throwable) {
            this.getLogger().log(Level.WARNING, "Failed to initialize the optional RCore boss-bar bridge", throwable);
        }

        this.townBossBarIntegration = null;
    }

    private void initializeCommands() {
        final CommandFactory commandFactory = new CommandFactory(this.plugin, this);
        commandFactory.registerAllCommandsAndListeners();
    }

    private void initializeViews() {
        this.viewFrame = ViewFrame.create(this.plugin)
            .install(AnvilInputFeature.AnvilInput)
            .with(
                new TownHubView(),
                new TownCreationProgressView(),
                new TownCreationRequirementsView(),
                new TownCreationRewardsView(),
                new TownCreationCurrencyContributionAnvilView(),
                new CreateTownNameAnvilView(),
                new CreateTownConfirmView(),
                new TownDirectoryView(),
                new TownInvitesView(),
                new TownOverviewView(),
                new TownArchetypeView(),
                new TownBankView(),
                new TownFobClaimsView(),
                new TownBankRootView(),
                new TownBankStorageView(),
                new TownBankCurrencyInputView(),
                new NationBankRootView(),
                new NationBankStorageView(),
                new NationBankCurrencyInputView(),
                new ServerBankRootView(),
                new ServerBankStorageView(),
                new ServerBankCurrencyInputView(),
                new TownColorAnvilView(),
                new TownRenameAnvilView(),
                new CreateNationNameAnvilView(),
                new NationRenameAnvilView(),
                new TownLevelCurrencyContributionAnvilView(),
                new TownNationCurrencyContributionAnvilView(),
                new TownLevelProgressView(),
                new TownLevelRequirementsView(),
                new TownLevelRewardsView(),
                new TownLevelRoadmapView(),
                new ClaimsView(),
                new TownChunkView(),
                new TownChunkTypeView(),
                new TownNationView(),
                new TownNationRequirementsView(),
                new TownNationRewardsView(),
                new TownNationFormationSelectionView(),
                new TownNationInviteListView(),
                new TownNationMemberListView(),
                new TownNationInviteTownView(),
                new TownNationCapitalSelectionView(),
                new TownNationConfirmView(),
                new TownRelationshipsView(),
                new TownRelationshipDetailView(),
                new TownProtectionScopeView(),
                new TownProtectionsView(),
                new TownRoleProtectionsView(),
                new TownAlliedProtectionsView(),
                new TownAlliedItemUseProtectionsView(),
                new TownAlliedSwitchProtectionsView(),
                new TownItemUseProtectionsView(),
                new TownSwitchProtectionsView(),
                new TownToggleProtectionsView()
            )
            .disableMetrics()
            .register();
    }

    private void startRuntimeServices() {
        if (this.townBossBarService != null) {
            this.townBossBarService.start();
        }
        if (this.townBankService != null) {
            this.townBankService.start();
        }
        if (this.taxRuntimeService != null) {
            this.taxRuntimeService.start();
        }
        if (this.townFuelService != null) {
            this.townFuelService.start();
        }
        if (this.townMedicService != null) {
            this.townMedicService.start();
        }
    }

    private @NotNull CompletableFuture<Void> runSync(final @NotNull Runnable task) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.platform.getScheduler().runSync(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (final Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private @NotNull File getConfigFile(final @NotNull String fileName) {
        return new File(new File(this.plugin.getDataFolder(), CONFIG_FOLDER_PATH), fileName);
    }

    private void ensureBundledConfigFiles() {
        this.ensureBundledConfigFile(CONFIG_FILE_NAME);
        this.ensureBundledConfigFile(NEXUS_CONFIG_FILE_NAME);
        this.ensureBundledConfigFile(NATION_CONFIG_FILE_NAME);
        this.ensureBundledConfigFile(SECURITY_CONFIG_FILE_NAME);
        this.ensureBundledConfigFile(BANK_CONFIG_FILE_NAME);
        this.ensureBundledConfigFile(TAX_CONFIG_FILE_NAME);
        this.ensureBundledConfigFile(FARM_CONFIG_FILE_NAME);
        this.ensureBundledConfigFile(FOB_CONFIG_FILE_NAME);
        this.ensureBundledConfigFile(OUTPOST_CONFIG_FILE_NAME);
        this.ensureBundledConfigFile(MEDIC_CONFIG_FILE_NAME);
        this.ensureBundledConfigFile(ARMORY_CONFIG_FILE_NAME);
    }

    private void ensureBundledConfigFile(final @NotNull String fileName) {
        final File dataFolder = this.plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            this.getLogger().warning("Could not create plugin data folder for config extraction.");
            return;
        }

        final File configFolder = new File(dataFolder, CONFIG_FOLDER_PATH);
        if (!configFolder.exists() && !configFolder.mkdirs()) {
            this.getLogger().warning("Could not create config folder for default config extraction.");
            return;
        }

        final File configFile = new File(configFolder, fileName);
        if (configFile.exists()) {
            return;
        }

        try {
            this.plugin.saveResource(CONFIG_FOLDER_PATH + "/" + fileName, false);
        } catch (final IllegalArgumentException exception) {
            this.getLogger().warning("Bundled default config could not be extracted for " + fileName + ": " + exception.getMessage());
        }
    }

    private @NotNull EntityManagerFactory requireEntityManagerFactory() {
        final EntityManagerFactory factory = this.platform.getEntityManagerFactory();
        if (factory == null) {
            throw new IllegalStateException("RPlatform did not initialize the entity manager factory.");
        }
        return factory;
    }

    /**
     * Returns the active Vault economy instance when available.
     *
     * @return Vault economy, or {@code null} when unavailable
     */
    public @Nullable net.milkbowl.vault.economy.Economy getEco() {
        return this.economyInstance instanceof net.milkbowl.vault.economy.Economy economy ? economy : null;
    }

    /**
     * Returns the owning Bukkit plugin.
     *
     * @return owning Bukkit plugin
     */
    public @NotNull JavaPlugin getPlugin() {
        return this.plugin;
    }

    /**
     * Returns the plugin logger.
     *
     * @return plugin logger
     */
    public @NotNull Logger getLogger() {
        return this.plugin.getLogger();
    }

    /**
     * Returns the Bukkit server instance.
     *
     * @return Bukkit server
     */
    public @NotNull Server getServer() {
        return this.plugin.getServer();
    }

    /**
     * Returns the async executor.
     *
     * @return async executor
     */
    public @Nullable ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Returns the shared RPlatform runtime.
     *
     * @return shared platform runtime
     */
    public @Nullable RPlatform getPlatform() {
        return this.platform;
    }

    /**
     * Returns the entity manager factory.
     *
     * @return entity manager factory
     */
    public @Nullable EntityManagerFactory getEntityManagerFactory() {
        return this.entityManagerFactory;
    }

    /**
     * Returns the scheduler adapter.
     *
     * @return scheduler adapter
     */
    public @Nullable ISchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    /**
     * Returns the detected platform type.
     *
     * @return detected platform type
     */
    public @Nullable PlatformType getPlatformType() {
        return this.platformType;
    }

    /**
     * Returns the active edition label.
     *
     * @return edition label
     */
    public @NotNull String getEdition() {
        return this.edition;
    }

    /**
     * Returns the active view frame.
     *
     * @return registered view frame
     */
    public @Nullable ViewFrame getViewFrame() {
        return this.viewFrame;
    }

    /**
     * Returns the active proxy bridge.
     *
     * @return proxy bridge
     */
    public @NotNull ProxyService getProxyService() {
        return this.proxyService == null ? NoOpProxyService.createDefault() : this.proxyService;
    }

    /**
     * Returns the configured authoritative route ID for this Paper server.
     *
     * @return local server route identifier
     */
    public @NotNull String getServerRouteId() {
        final String configuredRouteId = this.getDefaultConfig().getProxyServerRouteId();
        if (!configuredRouteId.isBlank()) {
            return configuredRouteId;
        }
        final String serverName = this.plugin.getServer().getName();
        if (serverName == null || serverName.isBlank()) {
            return "server";
        }
        return serverName.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the player repository used by integrations and listeners.
     *
     * @return player repository
     */
    public @Nullable RRDTPlayer getPlayerRepository() {
        return this.playerRepository;
    }

    /**
     * Returns the town repository used by integrations and listeners.
     *
     * @return town repository
     */
    public @Nullable RRTown getTownRepository() {
        return this.townRepository;
    }

    /**
     * Returns the claimed-chunk repository.
     *
     * @return claimed-chunk repository
     */
    public @Nullable RRTownChunk getTownChunkRepository() {
        return this.townChunkRepository;
    }

    /**
     * Returns the town-invite repository.
     *
     * @return town-invite repository
     */
    public @Nullable RRTownInvite getTownInviteRepository() {
        return this.townInviteRepository;
    }

    /**
     * Returns the town-relationship repository.
     *
     * @return town-relationship repository
     */
    public @Nullable RRTownRelationship getTownRelationshipRepository() {
        return this.townRelationshipRepository;
    }

    /**
     * Returns the nation repository.
     *
     * @return nation repository
     */
    public @Nullable RRNation getNationRepository() {
        return this.nationRepository;
    }

    /**
     * Returns the nation-invite repository.
     *
     * @return nation-invite repository
     */
    public @Nullable RRNationInvite getNationInviteRepository() {
        return this.nationInviteRepository;
    }

    /**
     * Returns the singleton server-bank repository.
     *
     * @return server-bank repository
     */
    public @Nullable RRServerBank getServerBankRepository() {
        return this.serverBankRepository;
    }

    /**
     * Returns the central town runtime service.
     *
     * @return town runtime service
     */
    public @Nullable TownRuntimeService getTownRuntimeService() {
        return this.townRuntimeService;
    }

    /**
     * Returns the town-spawn service.
     *
     * @return town-spawn service
     */
    public @Nullable TownSpawnService getTownSpawnService() {
        return this.townSpawnService;
    }

    /**
     * Returns the town FOB travel service.
     *
     * @return town FOB travel service
     */
    public @Nullable TownFobService getTownFobService() {
        return this.townFobService;
    }

    /**
     * Returns the boss-bar service.
     *
     * @return boss-bar service
     */
    public @Nullable TownBossBarService getTownBossBarService() {
        return this.townBossBarService;
    }

    /**
     * Returns whether the town boss bar is enabled for the supplied player UUID.
     *
     * <p>When RCore is installed, the centralized RCore preference row is authoritative. Otherwise
     * RDT falls back to the legacy local player field, which defaults to enabled when no row
     * exists yet.</p>
     *
     * @param playerUuid player UUID to inspect
     * @return {@code true} when the town boss bar is enabled
     */
    public boolean isTownBossBarEnabled(final @NotNull UUID playerUuid) {
        if (this.townBossBarIntegration != null) {
            return this.townBossBarIntegration.isEnabled(playerUuid);
        }

        final RDTPlayer playerData = this.playerRepository == null ? null : this.playerRepository.findByPlayer(playerUuid);
        return playerData == null || playerData.isBossBarEnabled();
    }

    /**
     * Returns the town-bank service.
     *
     * @return town-bank service
     */
    public @Nullable TownBankService getTownBankService() {
        return this.townBankService;
    }

    /**
     * Returns the nation-bank service.
     *
     * @return nation-bank service
     */
    public @Nullable NationBankService getNationBankService() {
        return this.nationBankService;
    }

    /**
     * Returns the admin-only server-bank service.
     *
     * @return server-bank service
     */
    public @Nullable ServerBankService getServerBankService() {
        return this.serverBankService;
    }

    /**
     * Returns the scheduled tax runtime service.
     *
     * @return tax runtime service
     */
    public @Nullable TaxRuntimeService getTaxRuntimeService() {
        return this.taxRuntimeService;
    }

    /**
     * Returns the Farm enhancement service.
     *
     * @return Farm enhancement service
     */
    public @Nullable TownFarmService getTownFarmService() {
        return this.townFarmService;
    }

    /**
     * Returns the FE town fuel service.
     *
     * @return FE town fuel service
     */
    public @Nullable TownFuelService getTownFuelService() {
        return this.townFuelService;
    }

    /**
     * Returns the Medic chunk runtime service.
     *
     * @return Medic chunk runtime service
     */
    public @Nullable TownMedicService getTownMedicService() {
        return this.townMedicService;
    }

    /**
     * Returns the Armory chunk runtime service.
     *
     * @return Armory chunk runtime service
     */
    public @Nullable TownArmoryService getTownArmoryService() {
        return this.townArmoryService;
    }

    /**
     * Returns the nexus access validator.
     *
     * @return nexus access validator
     */
    public @Nullable NexusAccessService getNexusAccessService() {
        return this.nexusAccessService;
    }

    /**
     * Returns the edition-specific town service.
     *
     * @return edition-specific town service
     */
    public @NotNull TownService getTownService() {
        return this.townService;
    }
}
