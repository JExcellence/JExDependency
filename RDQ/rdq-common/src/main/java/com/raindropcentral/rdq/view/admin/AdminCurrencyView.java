package com.raindropcentral.rdq.view.admin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;

/**
 * Administrative currency overview view.
 * <p>
 * This view lists currencies RDQ can detect from installed economy integrations.
 * It includes Vault when available and provides a dedicated control for creating
 * the default RDQ currency {@code raindrops} through JExEconomy.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class AdminCurrencyView extends APaginatedView<AdminCurrencyView.DetectedCurrency> {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");

    private static final String JEX_ADAPTER_CLASS = "de.jexcellence.economy.adapter.CurrencyAdapter";
    private static final String JEX_CURRENCY_CLASS = "de.jexcellence.economy.database.entity.Currency";
    private static final String VAULT_ECONOMY_CLASS = "net.milkbowl.vault.economy.Economy";

    private static final String DEFAULT_CURRENCY_IDENTIFIER = "raindrops";
    private static final String DEFAULT_CURRENCY_PREFIX = "";
    private static final String DEFAULT_CURRENCY_SUFFIX = " raindrops";
    private static final String DEFAULT_CURRENCY_SYMBOL = "R";
    private static final Material DEFAULT_CURRENCY_ICON = Material.PRISMARINE_CRYSTALS;

    private static final String PROVIDER_JEX = "JExEconomy";
    private static final String PROVIDER_VAULT = "Vault";
    private static final String FALLBACK_VALUE = "None";

    private final State<RDQ> rdq = initialState("plugin");

    /**
     * Creates the admin currency view with {@link PluginIntegrationManagementView} as its parent view.
     */
    public AdminCurrencyView() {
        super(PluginIntegrationManagementView.class);
    }

    @Override
    protected String getKey() {
        return "admin_currency_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXCXXXXX",
            " OOOOOOO ",
            " OOOOOOO ",
            "XXXXXXXXX",
            "   <p>   "
        };
    }

    @Override
    protected CompletableFuture<List<DetectedCurrency>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        return CompletableFuture.completedFuture(this.detectCurrencies());
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull DetectedCurrency entry
    ) {
        final Player player = context.getPlayer();
        final ItemStack item = UnifiedBuilderFactory.item(entry.icon())
            .setName(this.i18n("currency_entry.name", player)
                .withPlaceholder("currency_id", entry.identifier())
                .build().component())
            .setLore(this.i18n("currency_entry.lore", player)
                .withPlaceholders(Map.of(
                    "provider", entry.provider(),
                    "symbol", this.normalizeDisplayValue(entry.symbol()),
                    "prefix", this.normalizeDisplayValue(entry.prefix()),
                    "suffix", this.normalizeDisplayValue(entry.suffix())
                ))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();

        builder.withItem(item);
    }

    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        render.layoutSlot('X', this.createDecorationItem(player));

        final Object jexAdapter = this.resolveServiceProvider(JEX_ADAPTER_CLASS);
        final boolean hasJExEconomy = jexAdapter != null;
        final boolean hasDefaultCurrency = hasJExEconomy && this.hasCurrency(jexAdapter, DEFAULT_CURRENCY_IDENTIFIER);

        render.layoutSlot('C', this.createDefaultCurrencyButton(player, hasJExEconomy, hasDefaultCurrency))
            .onClick(this::handleCreateDefaultCurrencyClick);

        final Pagination pagination = this.getPagination(render);
        if (pagination.source() == null || pagination.source().isEmpty()) {
            render.slot(22, this.createEmptyStateItem(player));
        }
    }

    private @NotNull List<DetectedCurrency> detectCurrencies() {
        final List<DetectedCurrency> detectedCurrencies = new ArrayList<>();

        final Object jexAdapter = this.resolveServiceProvider(JEX_ADAPTER_CLASS);
        if (jexAdapter != null) {
            this.collectJExCurrencies(jexAdapter, detectedCurrencies);
        }

        final Object vaultEconomy = this.resolveServiceProvider(VAULT_ECONOMY_CLASS);
        if (vaultEconomy != null) {
            final String vaultSingularName = this.normalizeDisplayValue(this.invokeStringMethod(vaultEconomy, "currencyNameSingular"));
            final String vaultPluralName = this.normalizeDisplayValue(this.invokeStringMethod(vaultEconomy, "currencyNamePlural"));

            detectedCurrencies.add(new DetectedCurrency(
                "vault",
                PROVIDER_VAULT,
                "$",
                vaultSingularName,
                vaultPluralName,
                Material.EMERALD
            ));
        }

        detectedCurrencies.sort(Comparator.comparing(DetectedCurrency::identifier, String.CASE_INSENSITIVE_ORDER));
        return detectedCurrencies;
    }

    private void collectJExCurrencies(
        final @NotNull Object jexAdapter,
        final @NotNull List<DetectedCurrency> detectedCurrencies
    ) {
        try {
            final Method getAllCurrenciesMethod = jexAdapter.getClass().getMethod("getAllCurrencies");
            final Object result = getAllCurrenciesMethod.invoke(jexAdapter);

            if (!(result instanceof Map<?, ?> currenciesMap)) {
                return;
            }

            for (final Object currencyEntity : currenciesMap.values()) {
                final DetectedCurrency detectedCurrency = this.extractJExCurrency(currencyEntity);
                if (detectedCurrency != null) {
                    detectedCurrencies.add(detectedCurrency);
                }
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.FINE, "Failed to collect JExEconomy currencies.", exception);
        }
    }

    private @Nullable DetectedCurrency extractJExCurrency(final @Nullable Object currencyEntity) {
        if (currencyEntity == null) {
            return null;
        }

        try {
            final Class<?> currencyClass = currencyEntity.getClass();

            final String identifier = this.invokeStringMethod(currencyEntity, currencyClass, "getIdentifier");
            if (identifier == null || identifier.isBlank()) {
                return null;
            }

            final String symbol = this.invokeStringMethod(currencyEntity, currencyClass, "getSymbol");
            final String prefix = this.invokeStringMethod(currencyEntity, currencyClass, "getPrefix");
            final String suffix = this.invokeStringMethod(currencyEntity, currencyClass, "getSuffix");

            final Method getIconMethod = currencyClass.getMethod("getIcon");
            final Object iconResult = getIconMethod.invoke(currencyEntity);
            final Material icon = iconResult instanceof Material material ? material : Material.GOLD_INGOT;

            return new DetectedCurrency(
                identifier,
                PROVIDER_JEX,
                symbol,
                prefix,
                suffix,
                icon
            );
        } catch (final Exception exception) {
            LOGGER.log(Level.FINE, "Failed to extract JExEconomy currency entry.", exception);
            return null;
        }
    }

    private void handleCreateDefaultCurrencyClick(final @NotNull SlotClickContext clickContext) {
        final Player player = clickContext.getPlayer();
        final Object jexAdapter = this.resolveServiceProvider(JEX_ADAPTER_CLASS);

        if (jexAdapter == null) {
            this.i18n("default_currency.messages.jexeconomy_required", player)
                .includePrefix()
                .build().sendMessage();
            return;
        }

        if (this.hasCurrency(jexAdapter, DEFAULT_CURRENCY_IDENTIFIER)) {
            this.i18n("default_currency.messages.already_exists", player)
                .withPlaceholder("currency_id", DEFAULT_CURRENCY_IDENTIFIER)
                .includePrefix()
                .build().sendMessage();
            return;
        }

        if (!this.createDefaultCurrency(jexAdapter, player)) {
            this.i18n("default_currency.messages.failed", player)
                .withPlaceholder("currency_id", DEFAULT_CURRENCY_IDENTIFIER)
                .includePrefix()
                .build().sendMessage();
            return;
        }

        this.i18n("default_currency.messages.created", player)
            .withPlaceholder("currency_id", DEFAULT_CURRENCY_IDENTIFIER)
            .includePrefix()
            .build().sendMessage();

        clickContext.openForPlayer(
            AdminCurrencyView.class,
            Map.of("plugin", this.rdq.get(clickContext))
        );
    }

    private boolean createDefaultCurrency(
        final @NotNull Object jexAdapter,
        final @NotNull Player creator
    ) {
        try {
            final Class<?> currencyClass = Class.forName(JEX_CURRENCY_CLASS);
            final Constructor<?> constructor = currencyClass.getConstructor(
                String.class,
                String.class,
                String.class,
                String.class,
                Material.class
            );

            final Object currencyInstance = constructor.newInstance(
                DEFAULT_CURRENCY_PREFIX,
                DEFAULT_CURRENCY_SUFFIX,
                DEFAULT_CURRENCY_IDENTIFIER,
                DEFAULT_CURRENCY_SYMBOL,
                DEFAULT_CURRENCY_ICON
            );

            final CompletableFuture<Boolean> creationFuture = this.invokeCurrencyCreation(jexAdapter, currencyClass, currencyInstance, creator);
            return Boolean.TRUE.equals(creationFuture.join());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to create default RDQ currency.", exception);
            return false;
        }
    }

    private @NotNull CompletableFuture<Boolean> invokeCurrencyCreation(
        final @NotNull Object jexAdapter,
        final @NotNull Class<?> currencyClass,
        final @NotNull Object currencyInstance,
        final @NotNull Player creator
    ) throws ReflectiveOperationException {
        try {
            final Method createCurrencyWithPlayerMethod = jexAdapter.getClass().getMethod(
                "createCurrency",
                currencyClass,
                Player.class
            );
            final Object future = createCurrencyWithPlayerMethod.invoke(jexAdapter, currencyInstance, creator);
            return this.asBooleanFuture(future);
        } catch (final NoSuchMethodException ignored) {
            final Method createCurrencyMethod = jexAdapter.getClass().getMethod(
                "createCurrency",
                currencyClass
            );
            final Object future = createCurrencyMethod.invoke(jexAdapter, currencyInstance);
            return this.asBooleanFuture(future);
        }
    }

    private boolean hasCurrency(
        final @NotNull Object jexAdapter,
        final @NotNull String currencyIdentifier
    ) {
        try {
            final Method hasGivenCurrencyMethod = jexAdapter.getClass().getMethod("hasGivenCurrency", String.class);
            final Object future = hasGivenCurrencyMethod.invoke(jexAdapter, currencyIdentifier);
            return Boolean.TRUE.equals(this.asBooleanFuture(future).join());
        } catch (final Exception exception) {
            LOGGER.log(Level.FINE, "Failed to check JExEconomy currency existence.", exception);
            return false;
        }
    }

    private @NotNull CompletableFuture<Boolean> asBooleanFuture(final @Nullable Object futureObject) {
        if (!(futureObject instanceof CompletableFuture<?> future)) {
            return CompletableFuture.completedFuture(false);
        }
        return future.thenApply(result -> result instanceof Boolean bool && bool);
    }

    private @Nullable Object resolveServiceProvider(final @NotNull String serviceClassName) {
        try {
            final Class<?> serviceClass = Class.forName(serviceClassName);
            final RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(serviceClass);
            return registration != null ? registration.getProvider() : null;
        } catch (final ClassNotFoundException ignored) {
            return null;
        } catch (final Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve service provider: " + serviceClassName, exception);
            return null;
        }
    }

    private @Nullable String invokeStringMethod(
        final @NotNull Object target,
        final @NotNull String methodName
    ) {
        return this.invokeStringMethod(target, target.getClass(), methodName);
    }

    private @Nullable String invokeStringMethod(
        final @NotNull Object target,
        final @NotNull Class<?> targetClass,
        final @NotNull String methodName
    ) {
        try {
            final Method method = targetClass.getMethod(methodName);
            final Object result = method.invoke(target);
            return result instanceof String value ? value : null;
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @NotNull String normalizeDisplayValue(final @Nullable String value) {
        return value == null || value.isBlank() ? FALLBACK_VALUE : value;
    }

    private @NotNull ItemStack createDefaultCurrencyButton(
        final @NotNull Player player,
        final boolean hasJExEconomy,
        final boolean hasDefaultCurrency
    ) {
        if (!hasJExEconomy) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("default_currency.unavailable.name", player).build().component())
                .setLore(this.i18n("default_currency.unavailable.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        }

        if (hasDefaultCurrency) {
            return UnifiedBuilderFactory.item(Material.LIME_DYE)
                .setName(this.i18n("default_currency.already_exists.name", player)
                    .withPlaceholder("currency_id", DEFAULT_CURRENCY_IDENTIFIER)
                    .build().component())
                .setLore(this.i18n("default_currency.already_exists.lore", player)
                    .withPlaceholder("currency_id", DEFAULT_CURRENCY_IDENTIFIER)
                    .build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        }

        return UnifiedBuilderFactory.item(DEFAULT_CURRENCY_ICON)
            .setName(this.i18n("default_currency.create.name", player)
                .withPlaceholder("currency_id", DEFAULT_CURRENCY_IDENTIFIER)
                .build().component())
            .setLore(this.i18n("default_currency.create.lore", player)
                .withPlaceholder("currency_id", DEFAULT_CURRENCY_IDENTIFIER)
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createDecorationItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(this.i18n("decoration.border.name", player).build().component())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyStateItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.PAPER)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    static record DetectedCurrency(
        @NotNull String identifier,
        @NotNull String provider,
        @Nullable String symbol,
        @Nullable String prefix,
        @Nullable String suffix,
        @NotNull Material icon
    ) {
        DetectedCurrency {
            Objects.requireNonNull(identifier, "identifier");
            Objects.requireNonNull(provider, "provider");
            Objects.requireNonNull(icon, "icon");
        }
    }
}
