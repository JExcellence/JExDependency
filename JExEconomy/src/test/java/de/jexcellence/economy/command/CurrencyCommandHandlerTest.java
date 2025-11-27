package de.jexcellence.economy.command;

import com.raindropcentral.commands.BukkitCommand;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api .Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CurrencyCommandHandlerTest {

    private CommandRegistrar registrar;
    private CurrencyCommandHandler handler;
    private final List<CommandRegistration> registrations = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.registrar = mock(CommandRegistrar.class);
        this.handler = new CurrencyCommandHandler(this.registrar);
        when(this.registrar.registerCommand(any(), any())).thenAnswer(invocation -> {
            CommandRegistration registration = mock(CommandRegistration.class);
            this.registrations.add(registration);
            return registration;
        });
    }

    @Test
    void initializeRegistersEachCurrencyCommandExactlyOnce() {
        this.handler.initialize();
        this.handler.initialize();

        ArgumentCaptor<Class<? extends ACommandSection>> sectionCaptor = ArgumentCaptor.forClass(Class.class);
        ArgumentCaptor<Class<? extends BukkitCommand>> commandCaptor = ArgumentCaptor.forClass(Class.class);

        verify(this.registrar, times(5)).registerCommand(sectionCaptor.capture(), commandCaptor.capture());

        assertThat(sectionCaptor.getAllValues()).containsExactlyInAnyOrder(
                PCurrenciesSection.class,
                PCurrencySection.class,
                PCurrencyLogSection.class,
                CDepositSection.class,
                CWithdrawSection.class
        );

        assertThat(commandCaptor.getAllValues()).containsExactlyInAnyOrder(
                PCurrencies.class,
                PCurrency.class,
                PCurrencyLog.class,
                CDeposit.class,
                CWithdraw.class
        );
    }

    @Test
    void disposeReleasesRegistrationsAndAllowsReinitialization() {
        this.handler.dispose();
        verifyNoInteractions(this.registrar);

        this.handler.initialize();
        List<CommandRegistration> firstRegistrations = new ArrayList<>(this.registrations);

        this.handler.dispose();
        firstRegistrations.forEach(registration -> verify(registration, times(1)).dispose());
        this.registrations.clear();

        this.handler.initialize();
        List<CommandRegistration> secondRegistrations = new ArrayList<>(this.registrations);

        verify(this.registrar, times(10)).registerCommand(any(), any());
        secondRegistrations.forEach(registration -> verify(registration, never()).dispose());

        this.handler.dispose();
        secondRegistrations.forEach(registration -> verify(registration).dispose());
        firstRegistrations.forEach(registration -> verifyNoMoreInteractions(registration));
    }
}
