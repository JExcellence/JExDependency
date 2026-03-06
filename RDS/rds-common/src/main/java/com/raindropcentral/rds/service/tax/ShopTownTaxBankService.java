package com.raindropcentral.rds.service.tax;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.RTownShopBank;
import com.raindropcentral.rds.database.repository.RRTownShopBank;
import com.raindropcentral.rplatform.protection.RProtectionBridge;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;

/**
 * Provides support utilities for persisting and transferring town shop-tax ledger balances.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ShopTownTaxBankService {

    private static final double EPSILON = 1.0E-6D;

    private ShopTownTaxBankService() {
    }

    /**
     * Persists a successfully collected protection tax amount into the active town ledger scope.
     *
     * @param plugin active RDS runtime
     * @param payer owner who paid the tax
     * @param currencyType charged currency identifier
     * @param amount charged amount
     * @throws NullPointerException if {@code plugin}, {@code payer}, or {@code currencyType} is {@code null}
     */
    public static void recordCollectedTax(
            final @NotNull RDS plugin,
            final @NotNull OfflinePlayer payer,
            final @NotNull String currencyType,
            final double amount
    ) {
        if (amount <= EPSILON) {
            return;
        }

        final RRTownShopBank townShopBankRepository = plugin.getTownShopBankRepository();
        if (townShopBankRepository == null) {
            return;
        }

        final TownScope townScope = resolveTownScope(payer);
        if (townScope == null) {
            return;
        }

        townShopBankRepository.deposit(
                townScope.protectionPlugin(),
                townScope.townIdentifier(),
                townScope.townDisplayName(),
                normalizeToken(currencyType, "vault"),
                amount
        );
    }

    /**
     * Resolves whether the player can transfer ledgered shop taxes into their external town bank.
     *
     * @param player player to inspect
     * @return {@code true} when the player is mayor for the active town scope
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public static boolean canTransferToTownBank(final @NotNull Player player) {
        final TownScope townScope = resolveTownScope(player);
        return townScope != null && invokeBridgeBoolean(townScope.protectionBridge(), "isPlayerTownMayor", player);
    }

    /**
     * Resolves a town scope from the currently active protection bridge for the supplied player.
     *
     * @param player player to resolve
     * @return resolved town scope, or {@code null} when no supported scope is available
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public static @Nullable TownScope resolveTownScope(final @NotNull Player player) {
        final Player validatedPlayer = Objects.requireNonNull(player, "player");
        final RProtectionBridge protectionBridge = RProtectionBridge.getBridge();
        if (protectionBridge == null || !protectionBridge.isAvailable() || !protectionBridge.isPlayerInTown(validatedPlayer)) {
            return null;
        }

        final String townIdentifier = invokeBridgeString(protectionBridge, "getPlayerTownIdentifier", validatedPlayer);
        if (townIdentifier == null || townIdentifier.isBlank()) {
            return null;
        }

        final String normalizedTownIdentifier = normalizeToken(townIdentifier, "unknown");
        final String townDisplayName = normalizeDisplayName(
                invokeBridgeString(protectionBridge, "getPlayerTownDisplayName", validatedPlayer),
                normalizedTownIdentifier
        );
        return new TownScope(
                protectionBridge,
                normalizeToken(protectionBridge.getPluginName(), "unknown"),
                normalizedTownIdentifier,
                townDisplayName
        );
    }

    /**
     * Resolves a town scope from the currently active protection bridge for the supplied offline player.
     *
     * @param player player to resolve
     * @return resolved town scope, or {@code null} when unavailable
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public static @Nullable TownScope resolveTownScope(final @NotNull OfflinePlayer player) {
        final OfflinePlayer validatedPlayer = Objects.requireNonNull(player, "player");
        final Player onlinePlayer = validatedPlayer.getPlayer();
        if (onlinePlayer == null || !onlinePlayer.isOnline()) {
            return null;
        }

        return resolveTownScope(onlinePlayer);
    }

    /**
     * Transfers one currency from the plugin-managed town tax ledger into the external town bank.
     *
     * <p>Town-bank transfer currently supports {@code vault} currency because supported protection
     * plugins expose a single bank-balance API path.</p>
     *
     * @param plugin active RDS runtime
     * @param player mayor executing the transfer
     * @param currencyType currency to transfer
     * @return amount that was transferred successfully
     * @throws NullPointerException if {@code plugin}, {@code player}, or {@code currencyType} is {@code null}
     */
    public static double transferToTownBank(
            final @NotNull RDS plugin,
            final @NotNull Player player,
            final @NotNull String currencyType
    ) {
        final String normalizedCurrencyType = normalizeToken(currencyType, "vault");
        if (!"vault".equals(normalizedCurrencyType)) {
            return 0.0D;
        }

        final RRTownShopBank townShopBankRepository = plugin.getTownShopBankRepository();
        if (townShopBankRepository == null) {
            return 0.0D;
        }

        final TownScope townScope = resolveTownScope(player);
        if (townScope == null || !invokeBridgeBoolean(townScope.protectionBridge(), "isPlayerTownMayor", player)) {
            return 0.0D;
        }

        final RTownShopBank entry = townShopBankRepository.findByScope(
                townScope.protectionPlugin(),
                townScope.townIdentifier(),
                normalizedCurrencyType
        );
        if (entry == null || entry.getAmount() <= EPSILON) {
            return 0.0D;
        }

        final double transferAmount = entry.getAmount();
        if (!townScope.protectionBridge().depositToTownBank(player, transferAmount)) {
            return 0.0D;
        }

        return townShopBankRepository.withdraw(
                townScope.protectionPlugin(),
                townScope.townIdentifier(),
                normalizedCurrencyType,
                transferAmount
        );
    }

    private static @NotNull String normalizeToken(
            final @NotNull String rawToken,
            final @NotNull String fallback
    ) {
        final String normalized = Objects.requireNonNull(rawToken, "rawToken").trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static @NotNull String normalizeDisplayName(
            final @Nullable String rawDisplayName,
            final @NotNull String fallback
    ) {
        if (rawDisplayName == null || rawDisplayName.isBlank()) {
            return fallback;
        }
        return rawDisplayName.trim();
    }

    private static boolean invokeBridgeBoolean(
            final @NotNull RProtectionBridge bridge,
            final @NotNull String methodName,
            final @NotNull Player player
    ) {
        try {
            final Method method = bridge.getClass().getMethod(methodName, Player.class);
            final Object result = method.invoke(bridge, player);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static @Nullable String invokeBridgeString(
            final @NotNull RProtectionBridge bridge,
            final @NotNull String methodName,
            final @NotNull Player player
    ) {
        try {
            final Method method = bridge.getClass().getMethod(methodName, Player.class);
            final Object result = method.invoke(bridge, player);
            return result instanceof String value ? value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * Resolved protection-plugin town scope metadata.
     *
     * @param protectionBridge active protection bridge
     * @param protectionPlugin normalized plugin scope key
     * @param townIdentifier normalized town identifier
     * @param townDisplayName resolved town display name
     */
    public record TownScope(
            @NotNull RProtectionBridge protectionBridge,
            @NotNull String protectionPlugin,
            @NotNull String townIdentifier,
            @NotNull String townDisplayName
    ) {
    }
}
