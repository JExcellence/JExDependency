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
 * An abstract paginated view that provides common pagination functionality.
 * This view always uses layout-based configuration for consistent UI structure.
 *
 * @param <T> The type of items being paginated
 */
public abstract class APaginatedView<T> extends BaseView {
	
	/**
	 * The pagination state that manages the data and rendering.
	 * Uses buildLazyAsyncPaginationState with elementFactory to get context access.
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
	 * Returns the default pagination layout.
	 * Override this method to customize the layout structure.
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
	 * Returns the character used for pagination content slots.
	 * Override to customize the pagination area.
	 */
	protected char getPaginationSlotChar() {
		
		return 'O';
	}
	
	/**
	 * Returns the character used for the previous page button.
	 */
	protected char getPreviousButtonChar() {
		
		return '<';
	}
	
	/**
	 * Returns the character used for the next page button.
	 */
	protected char getNextButtonChar() {
		
		return '>';
	}
	
	/**
	 * Returns the character used for the page indicator.
	 */
	protected char getPageIndicatorChar() {
		
		return 'p';
	}
	
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
	 * Abstract method for additional rendering logic in paginated views.
	 * Called after the pagination and navigation elements are rendered.
	 * Renamed to avoid conflict with BaseView's onFirstRender method.
	 */
	protected abstract void onPaginatedRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	);
	
	/**
	 * Renders the pagination navigation buttons (previous, next).
	 * Separated from the back button to avoid conflicts.
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
	 * Renders the page indicator showing current page information.
	 * Now uses the base key pattern for i18n.
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
	 * Gets the item stack for the page indicator.
	 * Attempts to use a numbered head if available, falls back to paper.
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
	 * Gets the current pagination instance from a context.
	 */
	protected final Pagination getPagination(
		final @NotNull RenderContext context
	) {
		
		return this.pagination.get(context);
	}
	
}