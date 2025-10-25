package de.jexcellence.economy.command;

import com.raindropcentral.commands.BukkitCommand;
import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.ServerCommand;
import de.jexcellence.economy.command.console.deposit.CDeposit;
import de.jexcellence.economy.command.console.deposit.CDepositSection;
import de.jexcellence.economy.command.console.withdraw.CWithdraw;
import de.jexcellence.economy.command.console.withdraw.CWithdrawSection;
import de.jexcellence.economy.command.player.currency.PCurrency;
import de.jexcellence.economy.command.player.currency.PCurrencySection;
import de.jexcellence.economy.command.player.currencylog.PCurrencyLog;
import de.jexcellence.economy.command.player.currencylog.PCurrencyLogSection;
import de.jexcellence.economy.command.player.currencies.PCurrencies;
import de.jexcellence.economy.command.player.currencies.PCurrenciesSection;
import de.jexcellence.evaluable.section.ACommandSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates registration of all currency related commands exposed by the economy module.
 * <p>
 * The handler receives a {@link CommandRegistrar} abstraction so it can map each command section to its
 * implementation without depending on the concrete command framework. During plugin startup the handler's
 * {@link #initialize()} method is invoked to register every required command. The {@link #dispose()} method
 * releases those registrations when the plugin shuts down or reloads the currency subsystem.
 * </p>
 *
 * <p>
 * The handler registers both player commands ({@link PlayerCommand}) and console commands
 * ({@link ServerCommand}) to ensure all administration workflows remain available across currency editions.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public final class CurrencyCommandHandler {

    private static final List<CommandRegistrationRequest> REGISTRATION_REQUESTS = List.of(
            new CommandRegistrationRequest(PCurrenciesSection.class, PCurrencies.class),
            new CommandRegistrationRequest(PCurrencySection.class, PCurrency.class),
            new CommandRegistrationRequest(PCurrencyLogSection.class, PCurrencyLog.class),
            new CommandRegistrationRequest(CDepositSection.class, CDeposit.class),
            new CommandRegistrationRequest(CWithdrawSection.class, CWithdraw.class)
    );

    private final CommandRegistrar commandRegistrar;
    private final List<CommandRegistration> activeRegistrations = new ArrayList<>();
    private boolean initialized;

    /**
     * Creates a new handler instance that registers commands using the provided registrar.
     *
     * @param commandRegistrar abstraction that performs the underlying command registration, must not be null
     */
    public CurrencyCommandHandler(final @NotNull CommandRegistrar commandRegistrar) {
        this.commandRegistrar = Objects.requireNonNull(commandRegistrar, "commandRegistrar");
    }

    /**
     * Registers every currency command with the platform command framework.
     * <p>
     * Subsequent invocations after a successful initialization are ignored to prevent duplicate registrations.
     * </p>
     */
    public synchronized void initialize() {
        if (this.initialized) {
            return;
        }

        for (final CommandRegistrationRequest request : REGISTRATION_REQUESTS) {
            final CommandRegistration registration = this.commandRegistrar.registerCommand(
                    request.sectionClass,
                    request.commandClass
            );
            this.activeRegistrations.add(registration);
        }

        this.initialized = true;
    }

    /**
     * Disposes all active currency command registrations.
     * <p>
     * The handler can be re-initialized after disposal to register the commands again if required.
     * </p>
     */
    public synchronized void dispose() {
        if (!this.initialized) {
            return;
        }

        this.activeRegistrations.forEach(CommandRegistration::dispose);
        this.activeRegistrations.clear();
        this.initialized = false;
    }

    private static final class CommandRegistrationRequest {
        private final Class<? extends ACommandSection> sectionClass;
        private final Class<? extends BukkitCommand> commandClass;

        private CommandRegistrationRequest(
                final Class<? extends ACommandSection> sectionClass,
                final Class<? extends BukkitCommand> commandClass
        ) {
            this.sectionClass = sectionClass;
            this.commandClass = commandClass;
        }
    }
}
