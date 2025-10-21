package de.jexcellence.economy.utility;

import com.raindropcentral.rplatform.view.ConfirmationView;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.view.currency.*;
import de.jexcellence.economy.view.currency.anvil.*;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class ViewFactory {

    private final JExEconomyImpl jexEconomyImpl;
    private       ViewFrame      viewFrame;

    public ViewFactory(
            final @NotNull JExEconomyImpl jexEconomyImpl
    ) {
        this.jexEconomyImpl = jexEconomyImpl;
        this.initializeUserInterfaceFramework();
    }

    private void initializeUserInterfaceFramework() {
        this.viewFrame = ViewFrame
                .create(this.jexEconomyImpl.getImpl())
                .install(AnvilInputFeature.AnvilInput)
                .defaultConfig(viewConfiguration -> {
                    viewConfiguration.cancelOnClick();
                    viewConfiguration.cancelOnDrag();
                    viewConfiguration.cancelOnDrop();
                    viewConfiguration.cancelOnPickup();
                    viewConfiguration.interactionDelay(Duration.ofMillis(100));
                })
                .with(
                        new ConfirmationView(),
                        new CurrenciesOverviewView(),
                        new CurrenciesCreatingView(),
                        new CurrencyIconAnvilView(),
                        new CurrencyDetailView(),
                        new CurrencyLeaderboardView(),
                        new CurrenciesActionOverviewView(),
                        new CurrencyIdentifierAnvilView(),
                        new CurrencySymbolAnvilView(),
                        new CurrencyPrefixAnvilView(),
                        new CurrencySuffixAnvilView(),
                        new CurrencyDeletionView(),
                        new CurrencyEditingView(),
                        new CurrencyPropertiesEditingView()
                )
                .disableMetrics()
                .register();
    }

    public ViewFrame getViewFrame() {
        return this.viewFrame;
    }
}
