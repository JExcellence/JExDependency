package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ShopOverviewView extends BaseView {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> location = initialState("location");
    private final State<String>  owner = initialState("owner");

    @Override
    protected String getKey() {
        return "shop_overview_ui";
    }

    @Override
    protected int getSize() {
        return 1;
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {

        return Map.of(
                "location",
                "(" +
                        location.get(context).getX() + ", " +
                        location.get(context).getY() + ", " +
                        location.get(context).getZ() + ")",
                "owner",
                owner.get(context)
        );
    }

    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {

    }
}