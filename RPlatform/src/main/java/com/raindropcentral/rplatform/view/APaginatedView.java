package com.raindropcentral.rplatform.view;

import com.raindropcentral.rplatform.utility.heads.RHead;
import com.raindropcentral.rplatform.utility.heads.view.Next;
import com.raindropcentral.rplatform.utility.heads.view.Previous;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Template-method extension of {@link BaseView} that implements a paginated grid pattern with
 * navigation heads sourced from the head utility library.
 *
 * <p>The class coordinates asynchronous data loading via {@link Pagination} state, wires
 * {@link Next} and {@link Previous} heads into layout slots, and exposes hooks for subclasses to
 * render individual entries. Concrete implementations provide domain specific translation keys for
 * page labels and entry metadata.</p>
 *
 * @param <T> the type of elements rendered in the paginated grid
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class APaginatedView<T> extends BaseView {

        /**
         * Lazy pagination state that drives asynchronous data retrieval and exposes the
         * {@link Pagination} instance to rendering callbacks.
         */
        private final State<Pagination> pagination =
                this.buildLazyAsyncPaginationState(
			    this::getAsyncPaginationSource
		    )
			.layoutTarget(this.getPaginationSlotChar())
		    .elementFactory(this::renderEntry)
			.build();
	
	public APaginatedView(
		final @Nullable Class<? extends View> parentClazz
	) {
		
		super(parentClazz);
	}
	
	public APaginatedView() {
		
		this(null);
	}
	
	/**
	 * Provides the asynchronous data source for the pagination.
	 *
	 * @param context The context.
	 *
	 * @return A future list of items to paginate.
	 */
	protected abstract CompletableFuture<List<T>> getAsyncPaginationSource(
		final @NotNull Context context
	);
	
	/**
	 * Defines how to render a single item in the pagination.
	 * This method is called by the pagination system for each item.
	 * Now has access to context for i18n and other operations!
	 *
	 * @param context The current context (provides access to player, plugin state, etc.)
	 * @param builder The item builder.
	 * @param index   The index of the item in the pagination.
	 * @param entry   The item to render.
	 */
	protected abstract void renderEntry(
		final @NotNull Context context,
		final @NotNull BukkitItemComponentBuilder builder,
		final int index,
		final @NotNull T entry
	);
	
        /**
         * Declares the default pagination layout, mapping navigation heads and entry placeholders to
         * template characters.
         *
         * @return the layout rows for the paginated grid
         */
        @Override
        protected String[] getLayout() {
		
		return new String[]{
			"         ",
			"OOOOOOOOO",
			"OOOOOOOOO",
			"OOOOOOOOO",
			"         ",
			"b  <p>   "
		};
	}
	
        /**
         * Identifies the template character representing paginated content slots.
         *
         * @return the layout character reserved for entries
         */
        protected char getPaginationSlotChar() {

                return 'O';
        }

        /**
         * Declares the layout character assigned to the {@link Previous} head.
         *
         * @return the character used to position the previous page button
         */
        protected char getPreviousButtonChar() {

                return '<';
        }

        /**
         * Declares the layout character assigned to the {@link Next} head.
         *
         * @return the character used to position the next page button
         */
        protected char getNextButtonChar() {

                return '>';
        }

        /**
         * Supplies the layout character where page indicator metadata is rendered.
         *
         * @return the character representing the page indicator slot
         */
        protected char getPageIndicatorChar() {

                return 'p';
        }

        /**
         * Coordinates pagination navigation and indicator rendering after the base view has drawn
         * navigation controls.
         *
         * @param render the render context used to access layout slots
         * @param player the player observing the paginated view
         */
        @Override
        public void onFirstRender(
                final @NotNull RenderContext render,
                final @NotNull Player player
        ) {
		this.renderPaginationNavigationButtons(
			render,
			player,
			this.pagination.get(render)
		);
		
		this.renderPageIndicator(
			render,
			player,
			this.pagination.get(render)
		);
		
		this.onPaginatedRender(
			render,
			player
		);
		
	}
	
        /**
         * Hook for subclasses to render domain specific components after the pagination chrome has
         * been laid out.
         *
         * @param render the render context to bind slot behaviour
         * @param player the player currently observing the view
         */
        protected abstract void onPaginatedRender(
                final @NotNull RenderContext render,
                final @NotNull Player player
        );
	
        /**
         * Renders {@link Previous} and {@link Next} heads into the layout while binding state aware
         * click handlers.
         *
         * @param render the render context exposing layout slots
         * @param player the player whose head textures should be applied
         * @param pagination the pagination state representing current page data
         */
        private void renderPaginationNavigationButtons(
                final @NotNull RenderContext render,
                final @NotNull Player player,
                final @NotNull Pagination pagination
        ) {
		render
			.layoutSlot(
				this.getPreviousButtonChar(),
				new Previous().getHead(player)
			)
			.updateOnStateChange(this.pagination)
			.displayIf(pagination::canBack)
			.onClick(pagination::back);
		
		render
			.layoutSlot(
				this.getNextButtonChar(),
				new Next().getHead(player)
			)
			.updateOnStateChange(this.pagination)
			.displayIf(pagination::canAdvance)
			.onClick(pagination::advance);
	}
	
        /**
         * Renders the page indicator item, translating lore through the view's base key and showing
         * the current and maximum page counts.
         *
         * @param render the render context for slot registration
         * @param player the viewer for whom translations are resolved
         * @param pagination the pagination state providing page metadata
         */
        private void renderPageIndicator(
                final @NotNull RenderContext render,
                final @NotNull Player player,
                final @NotNull Pagination pagination
        ) {
		
		boolean hasPageIndicator = render.getLayoutSlots().stream().anyMatch(layoutSlot -> layoutSlot.getCharacter() == this.getPageIndicatorChar());
		
		if (
			! hasPageIndicator
		) {
			return;
		}
		
		final int       currentPage = pagination.currentPageIndex();
		final ItemStack pageItem    = this.getPageIndicatorItem(
			player,
			currentPage
		);
		
		render
			.layoutSlot(this.getPageIndicatorChar(), UnifiedBuilderFactory.item(pageItem)
			                                       .setName(
                                                           TranslationService.create(
                                                                   TranslationKey.of("page.name"),
                                                                   player
                                                           ).with(
                                                                   "page", currentPage + 1
                                                           ).build().component()
			                                       )
			                                       .setLore(
                                                           TranslationService.create(
                                                                   TranslationKey.of("page.lore"),
                                                                   player
                                                           ).withAll(
                                                                   Map.of(
                                                                           "page",
                                                                           currentPage + 1,
                                                                           "max_page",
                                                                           pagination.lastPageIndex() + 1,
                                                                           "first_page",
                                                                           1,
                                                                           "items_count",
                                                                           pagination.source() == null ?
                                                                                   0 :
                                                                                   pagination.source().size()
                                                                   )
                                                           ).build().splitLines()
			                                       )
			                                       .build())
			.updateOnStateChange(this.pagination);
	}
	
        /**
         * Resolves the {@link ItemStack} displayed in the page indicator slot, preferring numbered
         * heads and falling back to a localized paper item.
         *
         * @param player the viewer whose locale informs translation resolution
         * @param pageIndex the zero-based page index currently rendered
         * @return the indicator item that reflects the active page
         */
        private ItemStack getPageIndicatorItem(
                final @NotNull Player player,
                final int pageIndex
        ) {
		
		try {
			if (pageIndex >= 0 && pageIndex <= 9) {
				final String                 className      = "com.raindropcentral.rplatform.misc.heads.view.pagination.Number" + pageIndex;
				final Class<? extends RHead> pageIndexClass = (Class<? extends RHead>) Class.forName(className);
				return pageIndexClass.getDeclaredConstructor().newInstance().getHead(player);
			}
		} catch (final Exception ignored) {}
		
		return UnifiedBuilderFactory
			       .item(Material.PAPER)
			       .setName(
                           TranslationService.create(
                                   TranslationKey.of("page.fallback"),
                                   player
                           ).with(
                                   "page", pageIndex + 1
                           ).build().component()
			       )
			       .build();
	}
	
        /**
         * Obtains the {@link Pagination} instance for the provided render context so subclasses can
         * inspect page state.
         *
         * @param context the render context associated with the current frame
         * @return the pagination instance bound to this view
         */
        protected final Pagination getPagination(
                final @NotNull RenderContext context
        ) {
		
		return this.pagination.get(context);
	}
	
}