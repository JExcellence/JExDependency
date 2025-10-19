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
 * Template-method implementation for Inventory Framework views that centralizes
 * common layout, translation, and navigation behaviour.
 *
 * <p>The view bootstraps titles through {@link TranslationService} using the
 * base translation key supplied by subclasses and wires a {@link Return} head
 * into the default navigation row. Concrete views extend this class and
 * override the lifecycle hooks to provide domain specific rendering while
 * preserving the shared UX contract.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class BaseView extends View {

        /**
         * State reference to the parent view used for automatic back navigation
         * through the {@link Return} utility head.
         */
        protected final Class<? extends View> parentClazz;
        /**
         * Cached base translation key resolved during construction to avoid
         * repeated {@link #getKey()} lookups when building child keys.
         */
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
         * Retrieves the translation namespace used for titles, lore, and action prompts.
         *
         * @return the base i18n key for this view (for example {@code "rank_main_ui"})
         */
        protected abstract String getKey();

        /**
         * Resolves the translation key used for inventory titles by appending the {@code .title}
         * suffix to the base key. Subclasses may override for bespoke naming.
         *
         * @return the fully qualified title translation key
         */
        protected String getTitleKey() {
                return "title";
        }

        /**
         * Creates a {@link TranslationService} builder scoped to the view's translation namespace so
         * that downstream calls only provide the suffix for a specific message or lore entry.
         *
         * @param suffix the suffix to append to the base key (for example {@code "button.confirm"})
         * @param player the player whose locale should be used when building the component
         * @return a translation builder with the prefixed key ready for placeholder injection
         */
        protected TranslationService i18n(
                final @NotNull String suffix,
                final @NotNull Player player
        ) {
        return TranslationService.create(TranslationKey.of(this.baseKey, suffix), player);
	}
	
        /**
         * Supplies the default inventory layout, mapping template characters to Inventory Framework
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
         * Identifies the {@link Material} used while auto-filling empty slots before rendering
         * interactive components.
         *
         * @return the filler material employed by {@link #createFillItem(Player)}
         */
        protected Material getFillMaterial() {
                return Material.GRAY_STAINED_GLASS_PANE;
        }

        /**
         * Signals whether {@link #autoFillEmptySlots(RenderContext, Player)} should seed unused
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
         * Handles activation of the {@link Return} head and either closes the inventory or
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
	
        /**
         * Configures the {@link ViewConfigBuilder} using either the declarative layout or fallback
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
	
        /**
         * Applies layout or size configuration, translation-backed titles, and ensures the
         * Inventory Framework config stays synchronized with the player's locale.
         *
         * @param open the open context triggered when the view is presented to the player
         */
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
	
        /**
         * Performs initial render orchestration by drawing navigation heads, delegating to the
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
         * Abstract method for additional rendering logic executed after navigation heads and
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
         * Places the {@link Return} head in the template-defined back slot when the layout spans
         * multiple rows, wiring it to {@link #handleBackButtonClick(SlotClickContext)}.
         *
         * @param render the render context used to register slot behaviour
         * @param player the player for whom the head should be generated
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