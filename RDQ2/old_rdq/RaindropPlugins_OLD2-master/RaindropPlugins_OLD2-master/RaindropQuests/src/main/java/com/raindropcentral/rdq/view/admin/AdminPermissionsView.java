package com.raindropcentral.rdq.view.admin;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rplatform.logger.CentralLogger;
import com.raindropcentral.rplatform.misc.heads.view.Next;
import com.raindropcentral.rplatform.misc.heads.view.Previous;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.common.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Administrative GUI view for managing plugin permission sets with dual pagination.
 * <p>
 * This view provides a graphical interface for server administrators to assign, manage,
 * and review permission sets for various plugins and integrations supported by the server.
 * Features a dual pagination system with visual separation:
 * </p>
 *
 * <ul>
 *   <li><b>Plugin Row:</b> Always displays available plugins with permission sets</li>
 *   <li><b>Permission Row:</b> Shows individual permissions for the selected plugin</li>
 *   <li>Clear visual separation between plugin selection and permission management</li>
 *   <li>Integrates with {@link com.raindropcentral.rdq.permissions.PermissionsService} for permission data and assignment logic</li>
 *   <li>Uses {@link de.jexcellence.translate.api.I18n} with static keys and placeholders for localized content</li>
 * </ul>
 *
 * @author JExcellence
 * @version 3.0.0
 * @since TBD
 */
public class AdminPermissionsView extends BaseView {
	
	private static final Logger LOGGER = CentralLogger.getLogger(AdminPermissionsView.class.getName());
	
	/**
	 * State for storing the main plugin instance.
	 */
	private final State<RDQImpl> rdq = initialState("plugin");
	
	/**
	 * State for storing the currently selected plugin name.
	 */
	private final MutableState<String> selectedPlugin = mutableState(null);
	
	/**
	 * State for plugin pagination.
	 */
	private final MutableState<Integer> pluginPage = mutableState(0);
	
	/**
	 * State for permission pagination.
	 */
	private final MutableState<Integer> permissionPage = mutableState(0);
	
	/**
	 * State for data refresh timestamp to trigger UI updates.
	 */
	private final MutableState<Long> dataRefreshTimestamp = mutableState(System.currentTimeMillis());
	
	/**
	 * Cached plugin list for pagination.
	 */
	private final MutableState<List<String>> cachedPlugins = mutableState(new ArrayList<>());
	
	/**
	 * Cached permissions map.
	 */
	private final MutableState<Map<String, List<String>>> cachedPermissions = mutableState(Map.of());
	
	private static final int PLUGINS_PER_PAGE     = 7;
	private static final int PERMISSIONS_PER_PAGE = 7;
	
	// Slot layout for 6-row inventory (54 slots: 0-53)
	private static final int[] PLUGIN_SLOTS     = {10, 11, 12, 13, 14, 15, 16};
	private static final int[] PERMISSION_SLOTS = {28, 29, 30, 31, 32, 33, 34};
	
	private static final int PLUGIN_PREV_SLOT = 9;
	private static final int PLUGIN_NEXT_SLOT = 17;
	private static final int PERM_PREV_SLOT   = 27;
	private static final int PERM_NEXT_SLOT   = 35;
	
	private static final int STATUS_INDICATOR_SLOT = 22;
	private static final int CLEAR_SELECTION_SLOT  = 21;
	private static final int ASSIGN_SELECTED_SLOT  = 23;
	
	// Control bar slots (avoiding slot 45 which is auto-managed by BaseView for back button)
	private static final int REFRESH_DATA_SLOT = 49;
	private static final int ASSIGN_ALL_SLOT   = 52;
	
	public AdminPermissionsView() {
		super(AdminOverviewView.class);
	}
	
	@Override
	protected String getKey() {
		return "admin_permissions_overview_ui";
	}
	
	@Override
	public void onFirstRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		try {
			LOGGER.log(
				Level.INFO,
				"Starting enhanced AdminPermissionsView render for player: " + player.getName()
			);
			
			this.initializeData(render);
			
			this.renderHeaderSection(render, player);
			this.renderPluginSection(render, player);
			this.renderActionBar(render, player);
			this.renderPermissionSection(render, player);
			this.renderControlBar(render, player);
			this.renderDecorations(render, player);
			
			LOGGER.log(
				Level.INFO,
				"Enhanced AdminPermissionsView rendered successfully for player: " + player.getName()
			);
		} catch (final Exception exception) {
			LOGGER.log(
				Level.SEVERE,
				"Failed to render enhanced AdminPermissionsView",
				exception
			);
			this.renderErrorState(render, player);
		}
	}
	
	/**
	 * Initialize and cache data with enhanced error handling.
	 */
	private void initializeData(final @NotNull Context render) {
		try {
			Map<String, List<String>> permissionsMap = this.rdq.get(render).getPermissionsService().getPermissions();
			this.cachedPermissions.set(permissionsMap, render);
			this.cachedPlugins.set(new ArrayList<>(permissionsMap.keySet()), render);
			this.dataRefreshTimestamp.set(System.currentTimeMillis(), render);
			
			LOGGER.log(
				Level.FINE,
				"Data initialization completed with " + permissionsMap.size() + " plugins"
			);
		} catch (final Exception exception) {
			LOGGER.log(
				Level.WARNING,
				"Failed to initialize data",
				exception
			);
			this.cachedPermissions.set(Map.of(), render);
			this.cachedPlugins.set(new ArrayList<>(), render);
		}
	}
	
	/**
	 * Create assign selected plugin item.
	 */
	private @NotNull ItemStack createAssignSelectedItem(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		String selectedPlugin = this.selectedPlugin.get(render);
		if (selectedPlugin != null) {
			Map<String, List<String>> permissionsMap  = this.cachedPermissions.get(render);
			int permissionCount = permissionsMap.getOrDefault(selectedPlugin, List.of()).size();
			
			return UnifiedBuilderFactory.item(Material.EMERALD)
			                            .setName(this.i18n("actions.assign_selected.name", player)
			                                         .withPlaceholder("plugin_name", selectedPlugin)
			                                         .build().component())
			                            .setLore(this.i18n("actions.assign_selected.lore", player)
			                                         .withPlaceholders(Map.of(
				                                         "plugin_name", selectedPlugin,
				                                         "permission_count", permissionCount
			                                         )).build().children())
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build();
		} else {
			return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
			                            .setName(this.i18n("actions.no_selection.name", player).build().component())
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build();
		}
	}
	
	/**
	 * Handle assign selected plugin click.
	 */
	private void handleAssignSelectedClick(final @NotNull SlotClickContext clickContext) {
		String selectedPlugin = this.selectedPlugin.get(clickContext);
		
		if (selectedPlugin != null) {
			Map<String, List<String>> permissionsMap = this.cachedPermissions.get(clickContext);
			List<String> permissions = permissionsMap.get(selectedPlugin);
			
			if (permissions != null && !permissions.isEmpty()) {
				if (!this.rdq.get(clickContext).getPermissionsService().addPermissionSet(
					clickContext.getPlayer(),
					selectedPlugin,
					selectedPlugin.toLowerCase(),
					permissions,
					true
				)) {
					return;
				}
				
				this.i18n("feedback.permissions_assigned.plugin_all", clickContext.getPlayer())
				    .withPlaceholders(Map.of(
					    "plugin_name", selectedPlugin,
					    "permission_count", permissions.size()
				    ))
				    .includePrefix()
				    .sendMessage();
				
				LOGGER.log(
					Level.INFO,
					"Assigned " + permissions.size() + " permissions from " + selectedPlugin + " to player: " + clickContext.getPlayer().getName()
				);
			} else {
				this.i18n("errors.no_permissions_available", clickContext.getPlayer())
				    .includePrefix()
				    .sendMessage();
				
				LOGGER.log(Level.WARNING, "No permissions found for plugin: " + selectedPlugin);
			}
		} else {
			this.i18n("errors.plugin_not_found", clickContext.getPlayer())
			    .withPlaceholder("plugin_name", "Unknown")
			    .includePrefix()
			    .sendMessage();
			
			LOGGER.log(Level.WARNING, "Attempted to assign permissions for null plugin selection");
		}
	}
	
	/**
	 * Render header section with title and info.
	 */
	private void renderHeaderSection(final @NotNull RenderContext render, final @NotNull Player player) {
		for (int slot : new int[]{1, 2, 3, 4, 5, 6, 7}) {
			render.slot(slot).renderWith(() -> this.createHeaderDecorationItem(player));
		}
	}
	
	/**
	 * Render plugin selection section.
	 */
	private void renderPluginSection(final @NotNull RenderContext render, final @NotNull Player player) {
		render.slot(PLUGIN_PREV_SLOT)
		      .renderWith(() -> this.createNavigationItem(new Previous(), "plugins.navigation.previous", player))
		      .updateOnStateChange(this.pluginPage, this.cachedPlugins)
		      .displayIf(() -> this.canNavigatePlugins(render, -1))
		      .onClick(clickContext -> this.handlePluginPageChange(clickContext, -1));
		
		render.slot(PLUGIN_NEXT_SLOT)
		      .renderWith(() -> this.createNavigationItem(new Next(), "plugins.navigation.next", player))
		      .updateOnStateChange(this.pluginPage, this.cachedPlugins)
		      .displayIf(() -> this.canNavigatePlugins(render, 1))
		      .onClick(clickContext -> this.handlePluginPageChange(clickContext, 1));
		
		for (int i = 0; i < PLUGIN_SLOTS.length; i++) {
			final int slotIndex  = i;
			final int slotNumber = PLUGIN_SLOTS[i];
			
			render.slot(slotNumber)
			      .renderWith(() -> this.createPluginSlotContent(render, slotIndex, player))
			      .updateOnStateChange(this.selectedPlugin, this.pluginPage, this.dataRefreshTimestamp)
			      .onClick(clickContext -> this.handlePluginSlotClick(clickContext, slotIndex));
		}
	}
	
	/**
	 * Render central action bar.
	 */
	private void renderActionBar(final @NotNull RenderContext render, final @NotNull Player player) {
		render.slot(STATUS_INDICATOR_SLOT)
		      .renderWith(() -> this.createStatusIndicatorItem(render, player))
		      .updateOnStateChange(this.selectedPlugin, this.cachedPlugins, this.cachedPermissions);
		
		render.slot(CLEAR_SELECTION_SLOT)
		      .renderWith(() -> this.createClearSelectionItem(player))
		      .updateOnStateChange(this.selectedPlugin)
		      .displayIf(() -> this.selectedPlugin.get(render) != null)
		      .onClick(this::handleClearSelection);
		
		render.slot(ASSIGN_SELECTED_SLOT)
		      .renderWith(() -> this.createAssignSelectedItem(render, player))
		      .updateOnStateChange(this.selectedPlugin, this.cachedPermissions)
		      .displayIf(() -> this.selectedPlugin.get(render) != null)
		      .onClick(this::handleAssignSelectedClick);
	}
	
	/**
	 * Render permission display section.
	 */
	private void renderPermissionSection(final @NotNull RenderContext render, final @NotNull Player player) {
		render.slot(PERM_PREV_SLOT)
		      .renderWith(() -> this.createNavigationItem(new Previous(), "permissions.navigation.previous", player))
		      .updateOnStateChange(this.permissionPage, this.selectedPlugin, this.cachedPermissions)
		      .displayIf(() -> this.canNavigatePermissions(render, -1))
		      .onClick(clickContext -> this.handlePermissionPageChange(clickContext, -1));
		
		render.slot(PERM_NEXT_SLOT)
		      .renderWith(() -> this.createNavigationItem(new Next(), "permissions.navigation.next", player))
		      .updateOnStateChange(this.permissionPage, this.selectedPlugin, this.cachedPermissions)
		      .displayIf(() -> this.canNavigatePermissions(render, 1))
		      .onClick(clickContext -> this.handlePermissionPageChange(clickContext, 1));
		
		for (int i = 0; i < PERMISSION_SLOTS.length; i++) {
			final int slotIndex  = i;
			final int slotNumber = PERMISSION_SLOTS[i];
			
			render.slot(slotNumber)
			      .renderWith(() -> this.createPermissionSlotContent(render, slotIndex, player))
			      .updateOnStateChange(this.selectedPlugin, this.permissionPage, this.dataRefreshTimestamp)
			      .onClick(clickContext -> this.handlePermissionSlotClick(clickContext, slotIndex));
		}
	}
	
	/**
	 * Render bottom control bar.
	 */
	private void renderControlBar(final @NotNull RenderContext render, final @NotNull Player player) {
		render.slot(REFRESH_DATA_SLOT)
		      .renderWith(() -> this.createRefreshDataItem(player))
		      .onClick(this::handleRefreshData);
		
		render.slot(ASSIGN_ALL_SLOT)
		      .renderWith(() -> this.createAssignAllItem(render, player))
		      .updateOnStateChange(this.cachedPlugins)
		      .onClick(this::handleAssignAllClick);
	}
	
	/**
	 * Render decorative elements.
	 */
	private void renderDecorations(final @NotNull RenderContext render, final @NotNull Player player) {
		// Decoration slots for 6-row inventory, avoiding all functional slots and slot 45 (back button)
		for (int slot : new int[]{0, 8, 18, 19, 20, 24, 25, 26, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 48, 50, 51, 53}) {
			render.slot(slot).renderWith(() -> this.createDecorationItem(player));
		}
	}
	
	private @NotNull ItemStack createPluginSlotContent(
		final @NotNull RenderContext render,
		final int slotIndex,
		final @NotNull Player player
	) {
		
		final List<String> allPlugins = this.cachedPlugins.get(render);
		final int currentPage = this.pluginPage.get(render);
		final String currentSelection = this.selectedPlugin.get(render);
		final Map<String, List<String>> permissionsMap = this.cachedPermissions.get(render);
		
		final int pluginIndex = (currentPage * PLUGINS_PER_PAGE) + slotIndex;
		
		if (pluginIndex < allPlugins.size()) {
			final String pluginName = allPlugins.get(pluginIndex);
			final boolean isSelected = pluginName.equals(currentSelection);
			final int permissionCount = permissionsMap.getOrDefault(pluginName, List.of()).size();
			return this.createPluginItem(pluginName, isSelected, permissionCount, player);
		} else {
			return this.createEmptyPluginSlotItem(player);
		}
	}
	
	private @NotNull ItemStack createPermissionSlotContent(
		final @NotNull RenderContext render,
		final int slotIndex,
		final @NotNull Player player
	) {
		
		final String selectedPluginName = this.selectedPlugin.get(render);
		final Map<String, List<String>> permissionsMap = this.cachedPermissions.get(render);
		final int currentPage = this.permissionPage.get(render);
		
		final List<String> permissions = selectedPluginName != null ?
		                                 permissionsMap.getOrDefault(selectedPluginName, new ArrayList<>()) :
		                                 new ArrayList<>();
		
		final int permIndex = (currentPage * PERMISSIONS_PER_PAGE) + slotIndex;
		
		if (selectedPluginName == null) {
			return this.createNoSelectionItem(player);
		} else if (permIndex < permissions.size()) {
			return this.createPermissionItem(permissions.get(permIndex), selectedPluginName, player);
		} else {
			return this.createEmptyPermissionSlotItem(player);
		}
	}
	
	private @NotNull ItemStack createPluginItem(
		final @NotNull String pluginName,
		final boolean isSelected,
		final int permissionCount,
		final @NotNull Player player
	) {
		
		Material material = isSelected ? Material.ENCHANTED_BOOK : Material.BOOK;
		
		List<Component> lore = new ArrayList<>(this.i18n("plugins.entry.lore", player)
		                                           .withPlaceholders(Map.of(
			                                           "plugin_name", pluginName,
			                                           "permission_count", permissionCount,
			                                           "status", isSelected ? "selected" : "available"
		                                           )).build().children());
		
		lore.add(Component.empty());
		lore.addAll(this.i18n("plugins.entry.click_instructions", player).build().children());
		
		return UnifiedBuilderFactory.item(material)
		                            .setName(this.i18n("plugins.entry.name", player)
		                                         .withPlaceholder("plugin_name", pluginName)
		                                         .build().component())
		                            .setLore(lore)
		                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		                            .build();
	}
	
	private @NotNull ItemStack createPermissionItem(
		final @NotNull String permission,
		final @NotNull String pluginName,
		final @NotNull Player player
	) {
		
		List<Component> lore = new ArrayList<>();
		lore.add(this.i18n("permissions.entry.plugin", player)
		             .withPlaceholder("plugin_name", pluginName)
		             .build().component());
		lore.add(this.i18n("permissions.entry.node", player)
		             .withPlaceholder("permission", permission)
		             .build().component());
		
		lore.add(Component.empty());
		lore.addAll(this.i18n("permissions.entry.click_instructions", player).build().children());
		
		return UnifiedBuilderFactory.item(Material.PAPER)
		                            .setName(this.i18n("permissions.entry.name", player)
		                                         .withPlaceholder("permission", permission)
		                                         .build().component())
		                            .setLore(lore)
		                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		                            .build();
	}
	
	private @NotNull ItemStack createStatusIndicatorItem(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		String selectedPlugin = this.selectedPlugin.get(render);
		int totalPlugins = this.cachedPlugins.get(render).size();
		
		if (selectedPlugin != null) {
			Map<String, List<String>> permissionsMap = this.cachedPermissions.get(render);
			int permissionCount = permissionsMap.getOrDefault(selectedPlugin, List.of()).size();
			
			return UnifiedBuilderFactory.item(Material.COMPASS)
			                            .setName(this.i18n("status.selected_plugin.name", player)
			                                         .withPlaceholder("plugin_name", selectedPlugin)
			                                         .build().component())
			                            .setLore(this.i18n("status.selected_plugin.lore", player)
			                                         .withPlaceholders(Map.of(
				                                         "plugin_name", selectedPlugin,
				                                         "permission_count", permissionCount,
				                                         "total_plugins", totalPlugins
			                                         )).build().children())
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build();
		} else {
			return UnifiedBuilderFactory.item(Material.CLOCK)
			                            .setName(this.i18n("status.overview.name", player).build().component())
			                            .setLore(this.i18n("status.overview.lore", player)
			                                         .withPlaceholder("total_plugins", totalPlugins)
			                                         .build().children())
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build();
		}
	}
	
	// Helper methods for creating various UI elements
	private @NotNull ItemStack createNoSelectionItem(final @NotNull Player player) {
		return UnifiedBuilderFactory.item(Material.BARRIER)
		                            .setName(this.i18n("permissions.no_selection.name", player).build().component())
		                            .setLore(this.i18n("permissions.no_selection.lore", player).build().children())
		                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		                            .build();
	}
	
	private @NotNull ItemStack createEmptyPluginSlotItem(@NotNull Player player) {
		return UnifiedBuilderFactory.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
		                            .setName(this.i18n("plugins.empty_slot.name", player).build().component())
		                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		                            .build();
	}
	
	private @NotNull ItemStack createEmptyPermissionSlotItem(@NotNull Player player) {
		return UnifiedBuilderFactory.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
		                            .setName(this.i18n("permissions.empty_slot.name", player).build().component())
		                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		                            .build();
	}
	
	private @NotNull ItemStack createHeaderDecorationItem(@NotNull Player player) {
		return UnifiedBuilderFactory.item(Material.PURPLE_STAINED_GLASS_PANE)
		                            .setName(this.i18n("decoration.header.name", player).build().component())
		                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		                            .build();
	}
	
	private @NotNull ItemStack createDecorationItem(@NotNull Player player) {
		return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
		                            .setName(this.i18n("decoration.border.name", player).build().component())
		                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		                            .build();
	}
	
	private @NotNull ItemStack createRefreshDataItem(@NotNull Player player) {
		return UnifiedBuilderFactory.item(Material.EMERALD)
		                            .setName(this.i18n("controls.refresh.name", player).build().component())
		                            .setLore(this.i18n("controls.refresh.lore", player).build().children())
		                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		                            .build();
	}
	
	private @NotNull ItemStack createClearSelectionItem(final @NotNull Player player) {
		return UnifiedBuilderFactory.item(Material.BARRIER)
		                            .setName(this.i18n("clear_selection.name", player).build().component())
		                            .setLore(this.i18n("clear_selection.lore", player).build().children())
		                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		                            .build();
	}
	
	private @NotNull ItemStack createAssignAllItem(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		String selected = this.selectedPlugin.get(render);
		if (selected != null) {
			return UnifiedBuilderFactory.item(Material.EMERALD)
			                            .setName(this.i18n("assign_selected_plugin_permissions.name", player)
			                                         .withPlaceholder("plugin_name", selected)
			                                         .build().component())
			                            .setLore(this.i18n("assign_selected_plugin_permissions.lore", player).build().children())
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build();
		} else {
			int totalPlugins = this.cachedPlugins.get(render).size();
			return UnifiedBuilderFactory.item(Material.DIAMOND)
			                            .setName(this.i18n("assign_all_permissions.name", player).build().component())
			                            .setLore(this.i18n("assign_all_permissions.lore", player)
			                                         .withPlaceholder("total_plugins", totalPlugins)
			                                         .build().children())
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build();
		}
	}
	
	private @NotNull ItemStack createNavigationItem(
		final @NotNull Object headProvider,
		final @NotNull String name,
		final @NotNull Player player
	) {
		
		try {
			final ItemStack headItem = this.extractHeadFromProvider(headProvider, player);
			return UnifiedBuilderFactory.item(headItem)
			                            .setName(Component.text(name))
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to create navigation item", exception);
			return UnifiedBuilderFactory.item(Material.ARROW)
			                            .setName(Component.text(name))
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build();
		}
	}
	
	private @NotNull ItemStack extractHeadFromProvider(
		final @NotNull Object headProvider,
		final @NotNull Player player
	) {
		
		if (headProvider instanceof Previous) {
			return ((Previous) headProvider).getHead(player);
		} else if (headProvider instanceof Next) {
			return ((Next) headProvider).getHead(player);
		}
		return UnifiedBuilderFactory.item(Material.ARROW).addItemFlags(ItemFlag.HIDE_ATTRIBUTES).build();
	}
	
	// Event handlers and navigation logic
	private void handleRefreshData(@NotNull SlotClickContext clickContext) {
		this.initializeData(clickContext);
		this.selectedPlugin.set(null, clickContext);
		this.pluginPage.set(0, clickContext);
		this.permissionPage.set(0, clickContext);
		
		this.i18n("feedback.data_refreshed", clickContext.getPlayer())
		    .includePrefix()
		    .sendMessage();
	}
	
	private boolean canNavigatePlugins(@NotNull RenderContext render, int delta) {
		final int currentPage = this.pluginPage.get(render);
		final List<String> allPlugins = this.cachedPlugins.get(render);
		final int maxPage = Math.max(0, (allPlugins.size() - 1) / PLUGINS_PER_PAGE);
		
		final int newPage = currentPage + delta;
		return newPage >= 0 && newPage <= maxPage && !allPlugins.isEmpty();
	}
	
	private boolean canNavigatePermissions(@NotNull RenderContext render, int delta) {
		final String selectedPluginName = this.selectedPlugin.get(render);
		if (selectedPluginName == null) return false;
		
		final Map<String, List<String>> permissionsMap = this.cachedPermissions.get(render);
		final List<String> permissions = permissionsMap.getOrDefault(selectedPluginName, new ArrayList<>());
		
		final int currentPage = this.permissionPage.get(render);
		final int maxPage = Math.max(0, (permissions.size() - 1) / PERMISSIONS_PER_PAGE);
		
		final int newPage = currentPage + delta;
		return newPage >= 0 && newPage <= maxPage && !permissions.isEmpty();
	}
	
	private void handlePluginSlotClick(@NotNull SlotClickContext clickContext, int slotIndex) {
		final List<String> allPlugins = this.cachedPlugins.get(clickContext);
		final int currentPage = this.pluginPage.get(clickContext);
		final int pluginIndex = (currentPage * PLUGINS_PER_PAGE) + slotIndex;
		
		if (pluginIndex < allPlugins.size()) {
			this.handlePluginSelection(clickContext, allPlugins.get(pluginIndex));
		}
	}
	
	private void handlePermissionSlotClick(@NotNull SlotClickContext clickContext, int slotIndex) {
		final String selectedPluginName = this.selectedPlugin.get(clickContext);
		if (selectedPluginName == null) return;
		
		final Map<String, List<String>> permissionsMap = this.cachedPermissions.get(clickContext);
		final List<String> permissions = permissionsMap.getOrDefault(selectedPluginName, new ArrayList<>());
		final int currentPage = this.permissionPage.get(clickContext);
		final int permIndex = (currentPage * PERMISSIONS_PER_PAGE) + slotIndex;
		
		if (permIndex < permissions.size()) {
			this.handlePermissionClick(clickContext, selectedPluginName, permissions.get(permIndex));
		}
	}
	
	private void handlePluginSelection(final @NotNull SlotClickContext clickContext, final @NotNull String pluginName) {
		final String currentSelection = this.selectedPlugin.get(clickContext);
		
		if (clickContext.getClickOrigin().getClick().isLeftClick()) {
			if (pluginName.equals(currentSelection)) {
				this.selectedPlugin.set(null, clickContext);
				this.permissionPage.set(0, clickContext);
				LOGGER.log(Level.FINE, "Deselected plugin: " + pluginName);
			} else {
				this.selectedPlugin.set(pluginName, clickContext);
				this.permissionPage.set(0, clickContext);
				LOGGER.log(Level.FINE, "Selected plugin: " + pluginName);
			}
			
			this.i18n("plugin_selected", clickContext.getPlayer())
			    .withPlaceholder("plugin_name", pluginName)
			    .includePrefix()
			    .sendMessage();
		} else if (clickContext.getClickOrigin().getClick().isRightClick()) {
			Map<String, List<String>> permissionsMap = this.cachedPermissions.get(clickContext);
			List<String> permissions = permissionsMap.get(pluginName);
			
			if (permissions != null && !permissions.isEmpty()) {
				if (!this.rdq.get(clickContext).getPermissionsService().addPermissionSet(
					clickContext.getPlayer(),
					pluginName,
					pluginName.toLowerCase(),
					permissions,
					true
				)) {
					return;
				}
				
				this.i18n("plugin_permissions_assigned", clickContext.getPlayer())
				    .withPlaceholders(Map.of(
					    "plugin_name", pluginName,
					    "permission_count", permissions.size()
				    ))
				    .includePrefix()
				    .sendMessage();
			}
		}
	}
	
	private void handlePermissionClick(
		final @NotNull SlotClickContext clickContext,
		final @NotNull String pluginName,
		final @NotNull String permission
	) {
		
		if (!this.rdq.get(clickContext).getPermissionsService().addPermissionSet(
			clickContext.getPlayer(),
			pluginName,
			pluginName.toLowerCase(),
			List.of(permission),
			true
		)) {
			return;
		}
		
		this.i18n("single_permission_assigned", clickContext.getPlayer())
		    .withPlaceholders(Map.of(
			    "permission", permission,
			    "plugin_name", pluginName
		    ))
		    .includePrefix()
		    .sendMessage();
	}
	
	private void handleClearSelection(final @NotNull SlotClickContext clickContext) {
		this.selectedPlugin.set(null, clickContext);
		this.permissionPage.set(0, clickContext);
		
		this.i18n("selection_cleared", clickContext.getPlayer())
		    .includePrefix()
		    .sendMessage();
	}
	
	private void handleAssignAllClick(final @NotNull SlotClickContext clickContext) {
		String selected = this.selectedPlugin.get(clickContext);
		
		if (selected != null) {
			Map<String, List<String>> permissionsMap = this.cachedPermissions.get(clickContext);
			List<String> permissions = permissionsMap.get(selected);
			
			if (permissions != null && !permissions.isEmpty()) {
				if (!this.rdq.get(clickContext).getPermissionsService().addPermissionSet(
					clickContext.getPlayer(),
					selected,
					selected.toLowerCase(),
					permissions,
					true
				)) {
					return;
				}
				
				this.i18n("selected_plugin_all_assigned", clickContext.getPlayer())
				    .withPlaceholders(Map.of(
					    "plugin_name", selected,
					    "permission_count", permissions.size()
				    ))
				    .includePrefix()
				    .sendMessage();
			}
		} else {
			this.rdq.get(clickContext).getPermissionsService().addAllPermissionSets(clickContext.getPlayer());
			
			int totalPlugins = this.cachedPlugins.get(clickContext).size();
			this.i18n("all_plugins_assigned", clickContext.getPlayer())
			    .withPlaceholder("plugin_count", totalPlugins)
			    .includePrefix()
			    .sendMessage();
		}
	}
	
	private void handlePluginPageChange(final @NotNull SlotClickContext clickContext, final int delta) {
		final int currentPage = this.pluginPage.get(clickContext);
		final List<String> allPlugins = this.cachedPlugins.get(clickContext);
		final int maxPage = Math.max(0, (allPlugins.size() - 1) / PLUGINS_PER_PAGE);
		
		final int newPage = Math.max(0, Math.min(maxPage, currentPage + delta));
		this.pluginPage.set(newPage, clickContext);
		
		LOGGER.log(Level.FINE, "Plugin page changed to: " + newPage);
	}
	
	private void handlePermissionPageChange(final @NotNull SlotClickContext clickContext, final int delta) {
		final String selectedPluginName = this.selectedPlugin.get(clickContext);
		if (selectedPluginName == null) return;
		
		final Map<String, List<String>> permissionsMap = this.cachedPermissions.get(clickContext);
		final List<String> permissions = permissionsMap.getOrDefault(selectedPluginName, new ArrayList<>());
		
		final int currentPage = this.permissionPage.get(clickContext);
		final int maxPage = Math.max(0, (permissions.size() - 1) / PERMISSIONS_PER_PAGE);
		
		final int newPage = Math.max(0, Math.min(maxPage, currentPage + delta));
		this.permissionPage.set(newPage, clickContext);
		
		LOGGER.log(Level.FINE, "Permission page changed to: " + newPage);
	}
	
	private void renderErrorState(final @NotNull RenderContext render, final @NotNull Player player) {
		try {
			final ItemStack errorItem = UnifiedBuilderFactory.item(Material.BARRIER)
			                                                 .setName(this.i18n("error.general.name", player).build().component())
			                                                 .setLore(this.i18n("error.general.lore", player).build().children())
			                                                 .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                                                 .build();
			
			render.slot(22).renderWith(() -> errorItem);
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to render error state", exception);
		}
	}
}