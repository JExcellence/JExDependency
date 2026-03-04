/*
 * ShopMaterialSearchAnvilView.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.view.shop.anvil;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.view.shop.ShopBrowserSupport;
import com.raindropcentral.rds.view.shop.ShopSearchView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Renders the anvil-based material search workflow for the public shop browser.
 */
public class ShopMaterialSearchAnvilView extends AbstractAnvilView {

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the material search anvil view.
     */
    public ShopMaterialSearchAnvilView() {
        super(ShopSearchView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_material_search_anvil_ui";
    }

    @Override
    protected @NotNull String getTitleKey() {
        return "title";
    }

    @Override
    protected @NotNull Object processInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final Material material = ShopBrowserSupport.resolveMaterialQuery(input);
        if (material == null) {
            throw new IllegalArgumentException("Invalid material query: " + input);
        }

        return material;
    }

    @Override
    protected @NotNull String getInitialInputText(
            final @NotNull OpenContext context
    ) {
        return "diamond_block";
    }

    @Override
    protected boolean isValidInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        return ShopBrowserSupport.resolveMaterialQuery(input) != null;
    }

    @Override
    protected void setupFirstSlot(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final ItemStack inputSlotItem = UnifiedBuilderFactory.item(Material.NAME_TAG)
                .setName(Component.text("diamond_block"))
                .setLore(this.i18n("input.lore", player).build().children())
                .build();

        render.firstSlot(inputSlotItem);
    }

    @Override
    protected void onValidationFailed(
            final @Nullable String input,
            final @NotNull Context context
    ) {
        this.i18n("error.invalid_material", context.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                        "input", input == null ? "" : input
                ))
                .build()
                .sendMessage();
    }

    @Override
    protected @NotNull Map<String, Object> prepareResultData(
            final @Nullable Object processingResult,
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final Map<String, Object> resultData = super.prepareResultData(processingResult, input, context);
        resultData.put("plugin", this.rds.get(context));
        resultData.put("searchMaterial", processingResult);
        return resultData;
    }
}
