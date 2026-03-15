package com.raindropcentral.rplatform.view;

import com.raindropcentral.rplatform.utility.heads.view.Return;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.component.ComponentFactory;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotRenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.internal.LayoutSlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Template-method implementation for Inventory Framework views that centralizes.
 * common layout, translation, and navigation behaviour.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class BaseView extends View {

        private static final String PROMOTION_URL = "https://raindropcentral.com";
        private static final String INITIAL_DATA_PLUGIN_KEY = "plugin";
        private static final String RDS_PACKAGE_PREFIX = "com.raindropcentral.rds.";
        private static final String RDR_PACKAGE_PREFIX = "com.raindropcentral.rdr.";
        private static final String RDQ_PACKAGE_PREFIX = "com.raindropcentral.rdq.";
        private static final @Nullable Field ITEM_BUILDER_ITEM_FIELD = resolveItemBuilderField("item");
        private static final @Nullable Field ITEM_BUILDER_RENDER_HANDLER_FIELD = resolveItemBuilderField("renderHandler");

        /**
         * State reference to the parent view used for automatic back navigation.
         * through the {@link Return} utility head.
         */
        protected final Class<? extends View> parentClazz;
        /**
         * Cached base translation key resolved during construction to avoid.
         * repeated {@link #getKey()} lookups when building child keys.
         */
        private final   String                baseKey;
	
	/**
	 * Executes BaseView.
	 */
	public BaseView(
		final @Nullable Class<? extends View> parentClazz
	) {
		
		this.parentClazz = parentClazz;
		this.baseKey = this.getKey();
	}
	
	/**
	 * Executes BaseView.
	 */
	public BaseView() {
		
		this(null);
	}
	
        /**
         * Retrieves the translation namespace used for titles, lore, and action prompts.
         *
         * @return the base i18n key for this view (for example {@code "rank_main_ui"})
         */
        protected abstract String getKey();

        /**
         * Resolves the translation key used for inventory titles by appending the {@code .title}.
         * suffix to the base key. Subclasses may override for bespoke naming.
         *
         * @return the fully qualified title translation key
         */
        protected String getTitleKey() {
                return "title";
        }

        /**
         * Creates an {@link I18n.Builder} scoped to the view's translation namespace so.
         * that downstream calls only provide the suffix for a specific message or lore entry.
         *
         * @param suffix the suffix to append to the base key (for example {@code "button.confirm"})
         * @param player the player whose locale should be used when building the component
         * @return a translation builder with the prefixed key ready for placeholder injection
         */
        protected I18n.Builder i18n(
                final @NotNull String suffix,
                final @NotNull Player player
        ) {
        return new I18n.Builder(this.baseKey + "." + suffix, player);
	}
	
        /**
         * Supplies the default inventory layout, mapping template characters to Inventory Framework.
         * layout slots for consistent background and navigation placement.
         *
         * @return the layout rows, each string representing one row of nine slots
         */
        protected String[] getLayout() {
		
		return new String[]{
			"         ", "         ",
			"         ", "         ",
			"         ", "         "
		};
	}
	
        /**
         * Provides the fallback inventory height when a concrete layout is not defined.
         *
         * @return the number of rows composing the inventory
         */
        protected int getSize() {
		
		return 6;
	}
	
        /**
         * Declares how frequently the view should refresh when scheduled updates are required.
         *
         * @return the update cadence in ticks, or {@code 0} when no scheduled updates are needed
         */
        protected int getUpdateSchedule() {
		
		return 0;
	}
	
        /**
         * Identifies the {@link Material} used while auto-filling empty slots before rendering.
         * interactive components.
         *
         * @return the filler material employed by {@link #createFillItem(Player)}
         */
        protected Material getFillMaterial() {
                return Material.GRAY_STAINED_GLASS_PANE;
        }

        /**
         * Signals whether {@link #autoFillEmptySlots(RenderContext, Player)} should seed unused.
         * slots with the filler item ahead of custom rendering.
         *
         * @return {@code true} if empty slots are padded automatically; {@code false} otherwise
         */
        protected boolean shouldAutoFill() {
                return true;
        }

        /**
         * Builds the {@link ItemStack} inserted in each empty slot when auto-fill is enabled.
         *
         * @param player the player viewing the inventory, allowing locale-sensitive metadata
         * @return the item placed in empty slots during the initial render
         */
        protected ItemStack createFillItem(
                final @NotNull Player player
        ) {
		return UnifiedBuilderFactory.item(
			this.getFillMaterial()
		).setName(Component.empty()).setLore(new ArrayList<>()).build();
	}
	
        /**
         * Supplies placeholder values that hydrate translated titles via {@link #i18n(String, Player)}.
         *
         * @param open the open context supplied by Inventory Framework
         * @return a map of placeholder keys and values applied to the title translation
         */
        protected Map<String, Object> getTitlePlaceholders(
                final @NotNull OpenContext open
        ) {
		
		return Map.of();
	}
	
	/**
	 * Returns the character used for the back/return button.
	 *
	 * @deprecated This method is no longer used as the back button is now placed in the bottom-left corner automatically.
	 */
	@Deprecated
	protected char getBackButtonChar() {
		
		return 'b';
	}
	
        /**
         * Handles activation of the {@link Return} head and either closes the inventory or.
         * navigates back to the configured {@link #parentClazz}.
         *
         * @param clickContext the click context provided by Inventory Framework
         */
        protected void handleBackButtonClick(
                final @NotNull SlotClickContext clickContext
        ) {
		
		if (
			this.parentClazz == null
		) {
			clickContext.closeForPlayer();
			return;
		}
		
		try {
			clickContext.openForPlayer(
				this.parentClazz,
				clickContext.getInitialData()
			);
		} catch (
			final Exception exception
		) {
			Logger.getLogger(BaseView.class.getName()).log(
				Level.WARNING,
				"Failed to open parent view: " + this.parentClazz.getSimpleName(),
				exception
			);
			clickContext.closeForPlayer();
		}
	}
	
	/**
	 * Checks if the layout is meaningful (contains non-space characters).
	 *
	 * @return true if the layout contains meaningful characters, false otherwise.
	 */
	private boolean meaningfulLayout() {
		
		final String[] layout = this.getLayout();
		
		if (layout == null) {
			return false;
		}
		
		for (
			final String row : layout
		) {
			if (
				row != null &&
				! row.trim().isEmpty()
			) {
				for (
					char c : row.toCharArray()
				) {
					if (
						c != ' '
					) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Calculates the number of rows in the inventory based on the current configuration.
	 *
	 * @return The number of rows in the inventory.
	 */
	private int getInventoryRows() {
		
		if (
			this.meaningfulLayout()
		) {
			final String[] layout = this.getLayout();
			return layout != null ? layout.length : this.getSize();
		} else {
			return this.getSize();
		}
	}
	
	/**
	 * Calculates the total number of slots in the inventory.
	 *
	 * @return The total number of slots.
	 */
	private int getTotalSlots() {
		return this.getInventoryRows() * 9;
	}
	
	/**
	 * Calculates the slot number for the bottom-left corner (last row, first column).
	 *
	 * @return The slot number for the bottom-left corner, or -1 if inventory has only 1 row.
	 */
	private int getBottomLeftSlot() {
		
		final int rows = this.getInventoryRows();
		
		return rows <= 1 ? -1 : (rows - 1) * 9;
	}
	
        /**
         * Configures the {@link ViewConfigBuilder} using either the declarative layout or fallback.
         * size and wires any scheduled updates prior to the view being opened.
         *
         * @param config the configuration builder provided during the initialization phase
         */
        @Override
        public void onInit(
                final @NotNull ViewConfigBuilder config
        ) {
		if (
			this.meaningfulLayout()
		) {
			config.layout(this.getLayout());
			Logger.getLogger(BaseView.class.getName()).log(
				Level.FINE,
				"Using layout configuration for view: " + this.getClass().getSimpleName()
			);
		} else {
			config.size(this.getSize());
			Logger.getLogger(BaseView.class.getName()).log(
				Level.FINE,
				"Using size configuration (" + this.getSize() + ") for view: " + this.getClass().getSimpleName()
			);
		}
		
		if (
			this.getUpdateSchedule() > 0
		) {
			config.scheduleUpdate(this.getUpdateSchedule());
		}
		
		config.build();
	}
	
        /**
         * Applies layout or size configuration, translation-backed titles, and ensures the.
         * Inventory Framework config stays synchronized with the player's locale.
         *
         * @param open the open context triggered when the view is presented to the player
         */
        @Override
        public void onOpen(
                final @NotNull OpenContext open
        ) {
		final Component titleComponent = this.i18n(
			this.getTitleKey(),
			open.getPlayer()
		).withPlaceholders(this.getTitlePlaceholders(open)).build().component();
		
		final String titleString = LegacyComponentSerializer.legacySection().serialize(titleComponent);
		open.modifyConfig().title(titleString);
		
		Logger.getLogger(BaseView.class.getName()).log(
			Level.FINE,
			"Set inventory title for " + open.getPlayer().getName() + 
			" using " + ServerEnvironment.getInstance().getServerType().name() + " format"
		);
	}
	
        /**
         * Performs initial render orchestration by drawing navigation heads, delegating to the.
         * subclass {@link #onFirstRender(RenderContext, Player)} implementation, and optionally
         * filling unused slots.
         *
         * @param render the render context associated with the current frame
         */
        @Override
        public void onFirstRender(
                final @NotNull RenderContext render
        ) {
		
		final Player player = render.getPlayer();
		
		this.renderNavigationButtons(render, player);
		this.onFirstRender(render, player);
		
		if (
			this.shouldAutoFill()
		) {
			this.autoFillEmptySlots(render, player);
		}

		this.decorateFreeEditionButtons(render);
	}

    /**
     * Executes onResume.
     */
    @Override
    public void onResume(@NotNull Context origin, @NotNull Context target) {
        super.onResume(origin, target);
    }

    /**
	 * Auto-fills empty slots with the fill material.
	 * This is called before any other rendering to provide a base layer.
	 */
	private void autoFillEmptySlots(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		final ItemStack fillItem = this.createFillItem(player);
		final int totalSlots = this.getTotalSlots();
		
/*
		//TODO GONNA DO A PULL REQUEST FOR THIS FEATURE
   
 		for (
			int slot = 0; slot < totalSlots; slot++
		) {
			render.slot(slot).onRender(onRender -> {
				if (
					onRender.getItem() == null ||
					onRender.getItem().getType().isAir()
				) {
					onRender.setItem(fillItem);
				}
			});
		} */
		
		Logger.getLogger(BaseView.class.getName()).log(
			Level.FINE,
			"Auto-filled " + totalSlots + " slots with " + getFillMaterial().name() + " for view: " + this.getClass().getSimpleName()
		);
	}

	private void decorateFreeEditionButtons(
		final @NotNull RenderContext render
	) {
		if (!this.shouldAppendPromotionForFreeEdition(render)) {
			return;
		}

		for (final ComponentFactory componentFactory : render.getComponentFactories()) {
			this.decorateComponentFactory(componentFactory);
		}

		this.decorateLayoutSlotFactories(render);
		this.decorateAvailableSlotFactories(render);
	}

	private void decorateComponentFactory(
		final @Nullable ComponentFactory componentFactory
	) {
		if (componentFactory instanceof BukkitItemComponentBuilder itemBuilder) {
			this.decorateItemBuilder(itemBuilder);
		}
	}

	private void decorateLayoutSlotFactories(
		final @NotNull RenderContext render
	) {
		final List<LayoutSlot> layoutSlots = render.getLayoutSlots();
		for (int index = 0; index < layoutSlots.size(); index++) {
			final LayoutSlot layoutSlot = layoutSlots.get(index);
			final IntFunction<ComponentFactory> slotFactory = layoutSlot.getFactory();
			if (slotFactory == null) {
				continue;
			}

			layoutSlots.set(index, layoutSlot.withFactory(position -> {
				final ComponentFactory componentFactory = slotFactory.apply(position);
				this.decorateComponentFactory(componentFactory);
				return componentFactory;
			}));
		}
	}

	private void decorateAvailableSlotFactories(
		final @NotNull RenderContext render
	) {
		final List<BiFunction<Integer, Integer, ComponentFactory>> availableSlotFactories = render.getAvailableSlotFactories();
		for (int index = 0; index < availableSlotFactories.size(); index++) {
			final BiFunction<Integer, Integer, ComponentFactory> availableFactory = availableSlotFactories.get(index);
			availableSlotFactories.set(index, (row, column) -> {
				final ComponentFactory componentFactory = availableFactory.apply(row, column);
				this.decorateComponentFactory(componentFactory);
				return componentFactory;
			});
		}
	}

	private static @Nullable Field resolveItemBuilderField(
		final @NotNull String fieldName
	) {
		try {
			final Field field = BukkitItemComponentBuilder.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field;
		} catch (final NoSuchFieldException ignored) {
			return null;
		}
	}

	protected final void decorateItemBuilder(
		final @NotNull BukkitItemComponentBuilder itemBuilder
	) {
		if (ITEM_BUILDER_RENDER_HANDLER_FIELD != null) {
			try {
				@SuppressWarnings("unchecked")
				final Consumer<SlotRenderContext> existingRenderHandler =
					(Consumer<SlotRenderContext>) ITEM_BUILDER_RENDER_HANDLER_FIELD.get(itemBuilder);
				if (existingRenderHandler != null) {
					itemBuilder.onRender(renderContext -> {
						existingRenderHandler.accept(renderContext);
						final ItemStack updatedItem = this.appendPromotionToItem(renderContext.getItem());
						if (updatedItem != null) {
							renderContext.setItem(updatedItem);
						}
					});
					return;
				}
			} catch (final IllegalAccessException ignored) {
			}
		}

		if (ITEM_BUILDER_ITEM_FIELD != null) {
			try {
				final Object fallbackItem = ITEM_BUILDER_ITEM_FIELD.get(itemBuilder);
				if (fallbackItem instanceof ItemStack itemStack) {
					final ItemStack updatedItem = this.appendPromotionToItem(itemStack);
					if (updatedItem != null) {
						itemBuilder.withItem(updatedItem);
					}
				}
			} catch (final IllegalAccessException ignored) {
			}
		}
	}

	protected final boolean shouldAppendPromotionForFreeEdition(
		final @NotNull Context context
	) {
		final Object initialData;
		try {
			initialData = context.getInitialData();
		} catch (final Exception ignored) {
			return false;
		}

		if (!(initialData instanceof Map<?, ?> initialDataMap)) {
			return false;
		}

		final Object pluginRuntime = initialDataMap.get(INITIAL_DATA_PLUGIN_KEY);
		if (pluginRuntime == null) {
			return false;
		}

		if (!this.isTargetModuleRuntime(pluginRuntime.getClass().getName())) {
			return false;
		}

		final String edition = this.resolveRuntimeEdition(pluginRuntime);
		return edition != null && "free".equalsIgnoreCase(edition.trim());
	}

	private boolean isTargetModuleRuntime(
		final @NotNull String className
	) {
		return className.startsWith(RDS_PACKAGE_PREFIX)
			|| className.startsWith(RDR_PACKAGE_PREFIX)
			|| className.startsWith(RDQ_PACKAGE_PREFIX);
	}

	private @Nullable String resolveRuntimeEdition(
		final @NotNull Object pluginRuntime
	) {
		try {
			final Method editionMethod = pluginRuntime.getClass().getMethod("getEdition");
			final Object editionValue = editionMethod.invoke(pluginRuntime);
			if (editionValue instanceof String edition) {
				return edition;
			}
		} catch (final ReflectiveOperationException ignored) {
		}

		final Boolean premiumState = this.resolvePremiumState(pluginRuntime);
		if (premiumState != null) {
			return premiumState ? "Premium" : "Free";
		}

		return null;
	}

	private @Nullable Boolean resolvePremiumState(
		final @NotNull Object pluginRuntime
	) {
		for (final String serviceGetter : List.of(
			"getShopService",
			"getStorageService",
			"getRankSystemService",
			"getBountyService"
		)) {
			try {
				final Method getter = pluginRuntime.getClass().getMethod(serviceGetter);
				final Object service = getter.invoke(pluginRuntime);
				if (service == null) {
					continue;
				}

				final Method isPremiumMethod = service.getClass().getMethod("isPremium");
				final Object premiumValue = isPremiumMethod.invoke(service);
				if (premiumValue instanceof Boolean premium) {
					return premium;
				}
			} catch (final ReflectiveOperationException ignored) {
			}
		}

		return null;
	}

	private @Nullable ItemStack appendPromotionToItem(
		final @Nullable ItemStack item
	) {
		if (item == null || item.getType().isAir()) {
			return null;
		}

		final ItemStack updatedItem = item.clone();
		final ItemMeta itemMeta = updatedItem.getItemMeta();
		if (itemMeta == null) {
			return null;
		}

		final List<Component> currentLore = itemMeta.lore();
		if (currentLore != null && this.containsPromotionLine(currentLore)) {
			return null;
		}

		final List<Component> updatedLore = currentLore == null
			? new ArrayList<>()
			: new ArrayList<>(currentLore);
		updatedLore.add(Component.text(PROMOTION_URL, NamedTextColor.AQUA));
		itemMeta.lore(updatedLore);
		updatedItem.setItemMeta(itemMeta);
		return updatedItem;
	}

	private boolean containsPromotionLine(
		final @NotNull List<Component> lore
	) {
		for (final Component line : lore) {
			final String plainLine = PlainTextComponentSerializer.plainText().serialize(line);
			if (plainLine.contains(PROMOTION_URL)) {
				return true;
			}
		}

		return false;
	}
	
        /**
         * Abstract method for additional rendering logic executed after navigation heads and.
         * auto-fill behaviour complete.
         *
         * @param render the render context for slot registration
         * @param player the player currently viewing the inventory
         */
        public abstract void onFirstRender(
                final @NotNull RenderContext render,
                final @NotNull Player player
        );

        /**
         * Places the {@link Return} head in the bottom-left slot when the layout spans.
         * multiple rows, wiring it to {@link #handleBackButtonClick(SlotClickContext)}.
         * 
         * <p>The back button is placed at the first slot of the last row (bottom-left corner).
         * If the slot is already occupied by a layout character, the back button will not be placed.
         *
         * @param render the render context used to register slot behaviour
         * @param player the player for whom the head should be generated
         */
        public void renderNavigationButtons(
                final @NotNull RenderContext render,
                final @NotNull Player player
	) {
		final int bottomLeftSlot = this.getBottomLeftSlot();
		
		if (bottomLeftSlot < 0) {
			Logger.getLogger(BaseView.class.getName()).log(
				Level.FINE,
				"Skipped back button placement (single row inventory) for view: " + this.getClass().getSimpleName()
			);
			return;
		}
		
		// Check if the slot is already occupied by a layout character
		final String[] layout = this.getLayout();
		if (layout != null && layout.length > 0) {
			final int lastRowIndex = layout.length - 1;
			if (lastRowIndex >= 0 && layout[lastRowIndex] != null && layout[lastRowIndex].length() > 0) {
				final char slotChar = layout[lastRowIndex].charAt(0);
				// Only place back button if the slot is empty (space character)
				if (slotChar != ' ') {
					Logger.getLogger(BaseView.class.getName()).log(
						Level.FINE,
						"Skipped back button placement (slot occupied by '" + slotChar + "') for view: " + this.getClass().getSimpleName()
					);
					return;
				}
			}
		}
		
		try {
			render
				.slot(bottomLeftSlot, new Return().getHead(player))
				.onClick(this::handleBackButtonClick);
			
			Logger.getLogger(BaseView.class.getName()).log(
				Level.FINE,
				"Placed back button at bottom-left slot " + bottomLeftSlot + " for view: " + this.getClass().getSimpleName()
			);
		} catch (Exception e) {
			Logger.getLogger(BaseView.class.getName()).log(
				Level.WARNING,
				"Failed to create custom head for back button, using fallback: " + e.getMessage()
			);
			
			// Fallback to a simple barrier item
			render
				.slot(bottomLeftSlot, new org.bukkit.inventory.ItemStack(org.bukkit.Material.BARRIER))
				.onClick(this::handleBackButtonClick);
		}
	}
	
}
