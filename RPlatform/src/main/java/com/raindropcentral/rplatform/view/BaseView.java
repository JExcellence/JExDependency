package com.raindropcentral.rplatform.view;

import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.heads.view.Return;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;

/**
 * An abstract base class for views that handles common configuration boilerplate.
 * Child classes can override hooks to customize the view's properties.
 */
public abstract class BaseView extends View {
	
	protected final Class<? extends View> parentClazz;
	private final   String                baseKey;
	
	public BaseView(
		final @Nullable Class<? extends View> parentClazz
	) {
		
		this.parentClazz = parentClazz;
		this.baseKey = this.getKey();
	}
	
	public BaseView() {
		
		this(null);
	}
	
	/**
	 * @return The base i18n key for this view (e.g., "rank_main_ui").
	 */
	protected abstract String getKey();
	
	/**
	 * @return The full title key by appending ".title" to the base key.
	 */
	protected String getTitleKey() {
		return "title";
	}
	
	/**
	 * Creates an I18n.Builder with the base key prefix for convenience.
	 *
	 * @param suffix The suffix to append to the base key (e.g., "button.confirm")
	 * @param player The player for localization
	 * @return A new I18n.Builder with the prefixed key
	 */
	protected TranslationService i18n(
		final @NotNull String suffix,
		final @NotNull Player player
	) {
        return TranslationService.create(TranslationKey.of(this.baseKey, suffix), player);
	}
	
	/**
	 * @return The layout of the inventory as rows (each string represents one row of 9 slots).
	 */
	protected String[] getLayout() {
		
		return new String[]{
			"         ", "         ",
			"         ", "         ",
			"         ", "         "
		};
	}
	
	/**
	 * @return The size (number of rows) of the inventory.
	 */
	protected int getSize() {
		
		return 6;
	}
	
	/**
	 * @return The update schedule in ticks, or 0 for none.
	 */
	protected int getUpdateSchedule() {
		
		return 0;
	}
	
	/**
	 * @return The material to use for filling empty slots. Override to change the fill material.
	 */
	protected Material getFillMaterial() {
		return Material.GRAY_STAINED_GLASS_PANE;
	}
	
	/**
	 * @return Whether to auto-fill empty slots with the fill material.
	 */
	protected boolean shouldAutoFill() {
		return true;
	}
	
	/**
	 * Creates the fill item for empty slots.
	 *
	 * @param player The player viewing the inventory
	 * @return The ItemStack to use for filling empty slots
	 */
	protected ItemStack createFillItem(
		final @NotNull Player player
	) {
		return UnifiedBuilderFactory.item(
			this.getFillMaterial()
		).setName(Component.empty()).setLore(new ArrayList<>()).build();
	}
	
	/**
	 * @param open The open context.
	 *
	 * @return A map of placeholders for the title.
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
	 * Handles the back button click event.
	 * Override this method to customize back button behavior.
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
			CentralLogger.getLogger(BaseView.class.getName()).log(
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
	
	@Override
	public void onInit(
		final @NotNull ViewConfigBuilder config
	) {
		if (
			this.meaningfulLayout()
		) {
			config.layout(this.getLayout());
			CentralLogger.getLogger(BaseView.class.getName()).log(
				Level.FINE,
				"Using layout configuration for view: " + this.getClass().getSimpleName()
			);
		} else {
			config.size(this.getSize());
			CentralLogger.getLogger(BaseView.class.getName()).log(
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
	
	@Override
	public void onOpen(
		final @NotNull OpenContext open
	) {
		final TranslatedMessage titleComponent = this.i18n(
			this.getTitleKey(),
			open.getPlayer()
		).withAll(this.getTitlePlaceholders(open))
		 .build();
		
		if (ServerEnvironment.getInstance().isPaper()) {
			open.modifyConfig().title(titleComponent.component());
		} else {
            open.modifyConfig().title(titleComponent.asLegacyText());
		}
		
		CentralLogger.getLogger(BaseView.class.getName()).log(
			Level.FINE,
			"Set inventory title for " + open.getPlayer().getName() + 
			" using " + ServerEnvironment.getInstance().getServerType().name() + " format"
		);
	}
	
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
		
		CentralLogger.getLogger(BaseView.class.getName()).log(
			Level.FINE,
			"Auto-filled " + totalSlots + " slots with " + getFillMaterial().name() + " for view: " + this.getClass().getSimpleName()
		);
	}
	
	/**
	 * Abstract method for additional rendering logic.
	 * Called after the auto-fill and navigation elements are rendered.
	 */
	public abstract void onFirstRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	);
	
	/**
	 * Renders the navigation buttons.
	 * The back button is automatically placed in the bottom-left corner (last row, first column)
	 * if the inventory has more than 1 row.
	 */
	public void renderNavigationButtons(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		final int bottomLeftSlot = this.getBottomLeftSlot();
		
		if (
			bottomLeftSlot < 0
		) {
			CentralLogger.getLogger(BaseView.class.getName()).log(
				Level.FINE,
				"Skipped back button placement (single row inventory) for view: " + this.getClass().getSimpleName()
			);
			return;
		}
		
		render
			.slot(
				bottomLeftSlot,
				new Return().getHead(player)
			)
			.onClick(this::handleBackButtonClick);
		
		CentralLogger.getLogger(BaseView.class.getName()).log(
			Level.FINE,
			"Placed back button at bottom-left slot " + bottomLeftSlot + " for view: " + this.getClass().getSimpleName()
		);
	}
	
}