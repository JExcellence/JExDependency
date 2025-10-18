package com.raindropcentral.rplatform.view;

import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * A confirmation view that allows users to confirm or cancel important actions.
 * Extends BaseView for consistent UI patterns and i18n handling.
 */
public class ConfirmationView extends BaseView {
	
	private final State<String>              customKey = initialState("key");
	private final State<Map<String, Object>> initialData    = initialState("initialData");
	private final State<Consumer<Boolean>>   callback       = initialState("callback");
	
	public ConfirmationView() {
		super();
	}
	
	@Override
	protected String getKey() {
		return "";
	}
	
	@Override
	protected String[] getLayout() {
		return new String[]{
			"         ",
			" ccc xxx ",
			" ccc xxx ",
			" ccc xxx ",
			"         ",
			"b        "
		};
	}
	
	@Override
	public void onOpen(@NotNull OpenContext open) {
		String key = this.customKey.get(open);
		
		if (
			key == null
		) {
			key = super.getTitleKey();
		}
		
		open.modifyConfig().title(
				TranslationService.create(
						TranslationKey.of(key, "title"),
						open.getPlayer()
				).withAll(this.getTitlePlaceholders(open)).build().component()
		);
	}
	
	@Override
	protected void handleBackButtonClick(@NotNull SlotClickContext clickContext) {
		clickContext.back(
			this.mergeWithInitialData(
				Map.of("confirmed", false),
				clickContext
			)
		);
		
		final Consumer<Boolean> callback = this.callback.get(clickContext);
		if (
			callback != null
		) {
			callback.accept(false);
		}
	}
	
	@Override
	public void onFirstRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		render.layoutSlot(
			'c',
			UnifiedBuilderFactory
				.item(Material.LIME_STAINED_GLASS_PANE)
				.setName(
						TranslationService.create(
								TranslationKey.of(this.customKey.get(render), "confirm.name"),
								player
						).build().component()
				)
				.setLore(
						TranslationService.create(
								TranslationKey.of(this.customKey.get(render), "confirm.lore"),
								player
						).withAll(this.initialData.get(render)).build().splitLines()
				)
				.build()
		).onClick(this::handleConfirm);
		
		render.layoutSlot(
			'x',
			UnifiedBuilderFactory
				.item(Material.RED_STAINED_GLASS_PANE)
				.setName(
						TranslationService.create(
								TranslationKey.of(this.customKey.get(render), "cancel.name"),
								player
						).build().component()
				)
				.setLore(
						TranslationService.create(
								TranslationKey.of(this.customKey.get(render), "cancel.lore"),
								player
						).withAll(this.initialData.get(render)).build().splitLines()
				)
				.build()
		).onClick(this::handleCancel);
	}
	
	/**
	 * Handles the confirm button click.
	 */
	private void handleConfirm(@NotNull Context clickContext) {
		clickContext.back(
			this.mergeWithInitialData(
				Map.of("confirmed", true),
				clickContext
			)
		);
		
		final Consumer<Boolean> callback = this.callback.get(clickContext);
		if (callback != null) {
			callback.accept(true);
		}
	}
	
	/**
	 * Handles the cancel button click.
	 */
	private void handleCancel(@NotNull Context clickContext) {
		clickContext.back(
			this.mergeWithInitialData(
				Map.of("confirmed", false),
				clickContext
			)
		);
		
		final Consumer<Boolean> callback = this.callback.get(clickContext);
		if (callback != null) {
			callback.accept(false);
		}
	}
	
	/**
	 * Utility to merge initial data with confirmation result.
	 */
	private Map<String, Object> mergeWithInitialData(
		final @NotNull Map<String, Object> result,
		final @NotNull Context context
	) {
		
		final Map<String, Object> initial = this.initialData.get(context);
		
		if (
			initial == null
		) {
			return result;
		}
		
		final Map<String, Object> merged = new HashMap<>(initial);
		merged.putAll(result);
		return merged;
	}
	
	/**
	 * Builder class for creating ConfirmationView instances with custom configuration.
	 */
	public static class Builder {
		
		private String key;
		private String messageKey;
		private Map<String, Object> initialData;
		private Consumer<Boolean> callback;
		private Class<? extends View> parentViewClass;
		
		/**
		 * Sets a custom title key for the confirmation dialog.
		 */
		public Builder withKey(@NotNull String key) {
			this.key = key;
			return this;
		}
		
		/**
		 * Sets a custom message key for the confirm button lore.
		 */
		public Builder withMessageKey(@NotNull String messageKey) {
			this.messageKey = messageKey;
			return this;
		}
		
		/**
		 * Sets initial data to be passed back to the parent view.
		 */
		public Builder withInitialData(@Nullable Map<String, Object> initialData) {
			this.initialData = initialData;
			return this;
		}
		
		/**
		 * Sets the callback to be executed when the user confirms or cancels.
		 */
		public Builder withCallback(@Nullable Consumer<Boolean> callback) {
			this.callback = callback;
			return this;
		}
		
		/**
		 * Sets the parent view class for proper navigation.
		 */
		public Builder withParentView(@Nullable Class<? extends View> parentViewClass) {
			this.parentViewClass = parentViewClass;
			return this;
		}
		
		/**
		 * Opens the confirmation view for the given player.
		 */
		public void openFor(
			final @NotNull Context context,
			final @NotNull Player player
		) {
			Map<String, Object> data = new HashMap<>();
			
			if (
				this.key != null
			) {
				data.put("key", this.key);
			}
			if (
				this.messageKey != null
			) {
				data.put("messageKey", this.messageKey);
			}
			if (
				this.initialData != null
			) {
				data.put("initialData", this.initialData);
			}
			if (
				this.callback != null
			) {
				data.put("callback", this.callback);
			}
			if (
				this.parentViewClass != null
			) {
				data.put("parentViewClass", this.parentViewClass);
			}
			try {
				context.openForPlayer(
					ConfirmationView.class,
					data
				);
			} catch (
				final Exception exception
			) {
				CentralLogger.getLogger(ConfirmationView.class.getName()).log(
					Level.WARNING,
					"Failed to open confirmation dialog",
					exception
				);
				context.back(data);
			}
		}
	}
	
}
