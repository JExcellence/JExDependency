package com.raindropcentral.rdt.view.main;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.view.town.ServerTownsOverviewView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class MainOverviewView extends BaseView {

    private final State<RDT> rdt = initialState("plugin");

    @Override
    protected String getKey() {
        return "main_overview_ui";
    }

    @Override
    protected int getSize() {
        return 1;
    }

    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        this.initializeServerTownView(
                render,
                player
        );
    }

    private void initializeServerTownView(
            final @NotNull RenderContext context,
            final @NotNull Player player
    ) {
        context
                .slot(
                        1,
                        1
                )
                .withItem(
                        UnifiedBuilderFactory
                                .item(
                                        Material.DIAMOND
                                )
                                .setName(
                                        this.i18n(
                                                        "main_overview.name",
                                                        player
                                                )
                                                .build()
                                                .component()
                                )
                                .setLore(
                                        this.i18n(
                                                        "main_overview.lore",
                                                        player
                                                )
                                                .build()
                                                .children()
                                )
                                .build()
                )
                .displayIf(() -> player.hasPermission("raindroptowns.command.towns"))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ServerTownsOverviewView.class,
                        Map.of(
                                "plugin",
                                this.rdt.get(clickContext)
                        )
                ));
    }
}
