package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShopOverviewView extends BaseView {

    private final State<RDS> rds = initialState("plugin");

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

    }
}