package de.jexcellence.economy.command.player.pay;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.adapter.CurrencyResponse;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Player command for transferring currency to other players.
 *
 * <p>This command allows players to pay other players using configured currencies.
 * Administrators can configure which currencies are allowed for payments and
 * set minimum/maximum transaction limits.
 *
 * <p><strong>Command Usage:</strong>
 * <pre>/pay &lt;currency&gt; &lt;player&gt; &lt;amount&gt;</pre>
 *
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Currency whitelist support - only allowed currencies can be paid</li>
 *   <li>Configurable min/max transaction amounts</li>
 *   <li>Tab completion for currencies and online players</li>
 *   <li>Asynchronous balance operations</li>
 *   <li>Comprehensive validation and error messages</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 * @see PlayerCommand
 * @see PPaySection
 */
@Command
public class PPay extends PlayerCommand {

    private final JExEconomy jexEconomyImpl;
    private final PPaySection paySection;

    /**
     * Constructs a new pay command handler.
     *
     * @param commandSectionConfiguration the command section configuration
     * @param jexEconomy the main plugin instance
     */
    public PPay(
            final @NotNull PPaySection commandSectionConfiguration,
            final @NotNull JExEconomy jexEconomy
    ) {
        super(commandSectionConfiguration);
        this.jexEconomyImpl = jexEconomy;
        this.paySection = commandSectionConfiguration;
    }

    @Override
    protected void onPlayerInvocation(
            final @NotNull Player commandExecutingPlayer,
            final @NotNull String commandLabel,
            final @NotNull String[] commandArguments
    ) {
        if (this.hasNoPermission(commandExecutingPlayer, EPayPermission.PAY)) {
            return;
        }

        if (!this.paySection.isEnabled()) {
            this.sendPayDisabledMessage(commandExecutingPlayer);
            return;
        }

        if (commandArguments.length < 3) {
            this.sendUsageMessage(commandExecutingPlayer);
            return;
        }

        final String currencyIdentifier = commandArguments[0];
        final String targetPlayerName = commandArguments[1];
        final String amountString = commandArguments[2];

        if (!this.paySection.isCurrencyAllowed(currencyIdentifier) 
                && !commandExecutingPlayer.hasPermission(EPayPermission.BYPASS.getPermission())) {
            this.sendCurrencyNotAllowedMessage(commandExecutingPlayer, currencyIdentifier);
            return;
        }

        final double amount;
        try {
            amount = Double.parseDouble(amountString);
        } catch (final NumberFormatException e) {
            this.sendInvalidAmountMessage(commandExecutingPlayer, amountString);
            return;
        }

        if (amount <= 0) {
            this.sendInvalidAmountMessage(commandExecutingPlayer, amountString);
            return;
        }

        if (!this.paySection.isAmountValid(amount)) {
            this.sendAmountOutOfRangeMessage(commandExecutingPlayer, amount);
            return;
        }

        if (targetPlayerName.equalsIgnoreCase(commandExecutingPlayer.getName())) {
            this.sendCannotPaySelfMessage(commandExecutingPlayer);
            return;
        }

        this.executePayment(commandExecutingPlayer, currencyIdentifier, targetPlayerName, amount);
    }

    @Override
    protected @NotNull List<String> onPlayerTabCompletion(
            final @NotNull Player tabCompletionRequestingPlayer,
            final @NotNull String commandLabel,
            final @NotNull String[] commandArguments
    ) {
        if (commandArguments.length == 1) {
            return this.getAvailableCurrencies(commandArguments[0]);
        }

        if (commandArguments.length == 2) {
            return this.getOnlinePlayerNames(commandArguments[1], tabCompletionRequestingPlayer);
        }

        if (commandArguments.length == 3) {
            return List.of("1", "10", "100", "1000");
        }

        return new ArrayList<>();
    }

    /**
     * Executes the payment transaction between two players.
     */
    private void executePayment(
            final @NotNull Player sender,
            final @NotNull String currencyIdentifier,
            final @NotNull String targetPlayerName,
            final double amount
    ) {
        final Currency currency = this.findCurrencyByIdentifier(currencyIdentifier);
        if (currency == null) {
            this.sendCurrencyNotFoundMessage(sender, currencyIdentifier);
            return;
        }

        this.findTargetUser(targetPlayerName).thenAcceptAsync(targetUser -> {
            if (targetUser == null) {
                this.sendPlayerNotFoundMessage(sender, targetPlayerName);
                return;
            }

            this.jexEconomyImpl.getCurrencyAdapter()
                    .getBalance(sender, currency)
                    .thenAcceptAsync(senderBalance -> {
                        if (senderBalance < amount) {
                            this.sendInsufficientFundsMessage(sender, currencyIdentifier, amount, senderBalance);
                            return;
                        }

                        this.performTransfer(sender, targetUser, currency, amount);
                    }, this.jexEconomyImpl.getExecutor());
        }, this.jexEconomyImpl.getExecutor());
    }

    /**
     * Finds a currency by its identifier from the cache.
     */
    private @Nullable Currency findCurrencyByIdentifier(final @NotNull String currencyIdentifier) {
        return this.jexEconomyImpl.getCurrencies().values().stream()
                .filter(c -> c.getIdentifier().equalsIgnoreCase(currencyIdentifier))
                .findFirst()
                .orElse(null);
    }

    /**
     * Performs the actual currency transfer between sender and target.
     */
    private void performTransfer(
            final @NotNull Player sender,
            final @NotNull User targetUser,
            final @NotNull Currency currency,
            final double amount
    ) {
        final OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(targetUser.getUniqueId());

        this.jexEconomyImpl.getCurrencyAdapter()
                .withdraw(sender, currency, amount)
                .thenComposeAsync(withdrawResponse -> {
                    if (!withdrawResponse.isTransactionSuccessful()) {
                        this.sendTransferFailedMessage(sender);
                        return CompletableFuture.completedFuture(
                                CurrencyResponse.createFailureResponse(amount, 0.0, "Withdrawal failed")
                        );
                    }

                    return this.jexEconomyImpl.getCurrencyAdapter()
                            .deposit(targetOfflinePlayer, currency, amount);
                }, this.jexEconomyImpl.getExecutor())
                .thenAcceptAsync(depositResponse -> {
                    if (depositResponse.isTransactionSuccessful()) {
                        this.sendPaymentSuccessMessage(sender, targetUser.getPlayerName(), amount, currency);
                        this.notifyTargetPlayer(targetUser.getUniqueId(), sender.getName(), amount, currency);
                    } else {
                        // Rollback: return money to sender
                        this.jexEconomyImpl.getCurrencyAdapter()
                                .deposit(sender, currency, amount);
                        this.sendTransferFailedMessage(sender);
                    }
                }, this.jexEconomyImpl.getExecutor());
    }

    /**
     * Finds a user by name (online or offline).
     */
    private @NotNull CompletableFuture<@Nullable User> findTargetUser(final @NotNull String playerName) {
        final Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return this.jexEconomyImpl.getUserRepository()
                    .findByUuidAsync(onlinePlayer.getUniqueId())
                    .thenApply(opt -> opt.orElse(null));
        }

        return CompletableFuture.supplyAsync(
                () -> this.jexEconomyImpl.getUserRepository()
                        .findByAttributes(Map.of("playerName", playerName))
                        .orElse(null),
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Gets available currencies for tab completion.
     */
    private @NotNull List<String> getAvailableCurrencies(final @NotNull String currentInput) {
        final String normalizedInput = currentInput.toLowerCase(Locale.ROOT);
        
        final List<String> allowedCurrencies = this.paySection.getAllowedCurrencies();
        
        if (allowedCurrencies.isEmpty()) {
            try {
                return this.jexEconomyImpl.getCurrencyRepository()
                        .findAll(0, 100)
                        .stream()
                        .map(Currency::getIdentifier)
                        .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(normalizedInput))
                        .collect(Collectors.toList());
            } catch (final Exception e) {
                return new ArrayList<>();
            }
        }

        return allowedCurrencies.stream()
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(normalizedInput))
                .collect(Collectors.toList());
    }

    /**
     * Gets online player names for tab completion.
     */
    private @NotNull List<String> getOnlinePlayerNames(
            final @NotNull String currentInput,
            final @NotNull Player excludePlayer
    ) {
        final String normalizedInput = currentInput.toLowerCase(Locale.ROOT);
        
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.equals(excludePlayer))
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(normalizedInput))
                .collect(Collectors.toList());
    }

    /**
     * Notifies the target player if they are online.
     */
    private void notifyTargetPlayer(
            final @NotNull UUID targetUuid,
            final @NotNull String senderName,
            final double amount,
            final @NotNull Currency currency
    ) {
        final Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            new I18n.Builder("pay.received", targetPlayer)
                    .includePrefix()
                    .withPlaceholder("sender", senderName)
                    .withPlaceholder("amount", String.format("%.2f", amount))
                    .withPlaceholder("currency", currency.getIdentifier())
                    .withPlaceholder("symbol", currency.getSymbol())
                    .build().sendMessage();
        }
    }


    private void sendUsageMessage(final @NotNull Player player) {
        new I18n.Builder("pay.usage", player)
                .includePrefix()
                .build().sendMessage();
    }

    private void sendPayDisabledMessage(final @NotNull Player player) {
        new I18n.Builder("pay.disabled", player)
                .includePrefix()
                .build().sendMessage();
    }

    private void sendCurrencyNotAllowedMessage(final @NotNull Player player, final @NotNull String currency) {
        new I18n.Builder("pay.currency_not_allowed", player)
                .includePrefix()
                .withPlaceholder("currency", currency)
                .build().sendMessage();
    }

    private void sendCurrencyNotFoundMessage(final @NotNull Player player, final @NotNull String currency) {
        new I18n.Builder("pay.currency_not_found", player)
                .includePrefix()
                .withPlaceholder("currency", currency)
                .build().sendMessage();
    }

    private void sendInvalidAmountMessage(final @NotNull Player player, final @NotNull String amount) {
        new I18n.Builder("pay.invalid_amount", player)
                .includePrefix()
                .withPlaceholder("amount", amount)
                .build().sendMessage();
    }

    private void sendAmountOutOfRangeMessage(final @NotNull Player player, final double amount) {
        new I18n.Builder("pay.amount_out_of_range", player)
                .includePrefix()
                .withPlaceholder("amount", String.format("%.2f", amount))
                .withPlaceholder("min", String.format("%.2f", this.paySection.getMinAmount()))
                .withPlaceholder("max", this.paySection.getMaxAmount() > 0 
                        ? String.format("%.2f", this.paySection.getMaxAmount()) 
                        : "unlimited")
                .build().sendMessage();
    }

    private void sendCannotPaySelfMessage(final @NotNull Player player) {
        new I18n.Builder("pay.cannot_pay_self", player)
                .includePrefix()
                .build().sendMessage();
    }

    private void sendPlayerNotFoundMessage(final @NotNull Player player, final @NotNull String targetName) {
        new I18n.Builder("pay.player_not_found", player)
                .includePrefix()
                .withPlaceholder("player", targetName)
                .build().sendMessage();
    }

    private void sendInsufficientFundsMessage(
            final @NotNull Player player,
            final @NotNull String currency,
            final double required,
            final double available
    ) {
        new I18n.Builder("pay.insufficient_funds", player)
                .includePrefix()
                .withPlaceholder("currency", currency)
                .withPlaceholder("required", String.format("%.2f", required))
                .withPlaceholder("available", String.format("%.2f", available))
                .build().sendMessage();
    }

    private void sendTransferFailedMessage(final @NotNull Player player) {
        new I18n.Builder("pay.transfer_failed", player)
                .includePrefix()
                .build().sendMessage();
    }

    private void sendPaymentSuccessMessage(
            final @NotNull Player player,
            final @NotNull String targetName,
            final double amount,
            final @NotNull Currency currency
    ) {
        new I18n.Builder("pay.success", player)
                .includePrefix()
                .withPlaceholder("target", targetName)
                .withPlaceholder("amount", String.format("%.2f", amount))
                .withPlaceholder("currency", currency.getIdentifier())
                .withPlaceholder("symbol", currency.getSymbol())
                .build().sendMessage();
    }
}
