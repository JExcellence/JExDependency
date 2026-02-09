package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TownOverviewView extends BaseView {
    @Override
    protected String getKey() {
        return "town_overview_view";
    }

    @Override
    protected int getSize() {
        return 1;
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        //Render town related buttons and info
    }
}
