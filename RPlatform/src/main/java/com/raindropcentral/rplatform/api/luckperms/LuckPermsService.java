package com.raindropcentral.rplatform.api.luckperms;

import com.raindropcentral.rplatform.RPlatform;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.*;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class LuckPermsService {
	
	private static final Logger    LOGGER = Logger.getLogger(LuckPermsService.class.getName());
	private final        RPlatform platform;
	
	public LuckPermsService(final @NotNull RPlatform platform) {
		this.platform = platform;
	}
	
	@Nullable
	public User getLuckPermsUser(final @NotNull UUID uuid) {
		if (Bukkit.getPlayer(uuid) == null) {
			return null;
		}
		return this.get().getUserManager().getUser(uuid);
	}
	
	@Nullable
	public User getLuckPermsUser(final @NotNull OfflinePlayer offlinePlayer) {
		final UserManager userManager = this.get().getUserManager();
		try {
			return userManager.loadUser(offlinePlayer.getUniqueId()).join();
		} catch (Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to load LuckPerms user for offline player " + offlinePlayer.getUniqueId(), exception);
			return null;
		}
	}
	
	@NotNull
	public String getPrefix(final @NotNull UUID uuid) {
		final User user = getLuckPermsUser(uuid);
		if (user != null) {
			final String prefix = user.getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getPrefix();
			return prefix != null ? prefix : "";
		}
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
		final User    offlineUser   = getLuckPermsUser(offlinePlayer);
		if (offlineUser != null) {
			final String prefix = offlineUser.getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getPrefix();
			return prefix != null ? prefix : "";
		}
		return "";
	}
	
	@NotNull
	public String getSuffix(final @NotNull UUID uuid) {
		final User user = getLuckPermsUser(uuid);
		if (user != null) {
			final String suffix = user.getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getSuffix();
			return suffix != null ? suffix : "";
		}
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
		final User offlineUser = getLuckPermsUser(offlinePlayer);
		if (offlineUser != null) {
			final String suffix = offlineUser.getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getSuffix();
			return suffix != null ? suffix : "";
		}
		return "";
	}
	
	public void applyRank(final @NotNull UUID uuid, final @NotNull String group) {
		checkArguments(uuid, group);
		final User            user = requireUser(uuid);
		final InheritanceNode node = InheritanceNode.builder(group.toLowerCase(Locale.ENGLISH)).value(true).build();
		user.data().add(node);
		saveUserAsync(user);
	}
	
	public void removeRank(final @NotNull UUID uuid, final @NotNull String group) {
		checkArguments(uuid, group);
		final User user = requireUser(uuid);
		final InheritanceNode node = InheritanceNode.builder(group.toLowerCase(Locale.ENGLISH)).value(true).build();
		user.data().remove(node);
		saveUserAsync(user);
	}
	
	/**
	 * Assigns a permission to a group, creating the group if it doesn't exist.
	 *
	 * @param groupName  The name of the group
	 * @param permission The permission node to assign
	 * @return CompletableFuture that completes with true if successful, false otherwise
	 */
	public CompletableFuture<Boolean> assignPermission(final @NotNull String groupName, final @NotNull String permission) {
		return this.get().getGroupManager().loadGroup(groupName)
			.thenComposeAsync(optionalGroup -> {
				if (optionalGroup.isPresent()) {
					return CompletableFuture.completedFuture(optionalGroup.get());
				} else {
					// Group doesn't exist, create it
					LOGGER.info("Group '" + groupName + "' doesn't exist, creating it...");
					return this.get().getGroupManager().createAndLoadGroup(groupName);
				}
			})
			.thenApplyAsync(group -> {
				final PermissionNode node = PermissionNode.builder(permission).build();
				group.data().add(node);
				saveGroupAsync(group);
				LOGGER.info("Assigned permission '" + permission + "' to group '" + groupName + "'");
				return true;
			})
			.exceptionally(ex -> {
				LOGGER.log(Level.SEVERE, "Error assigning permission to group " + groupName + ": " + ex.getMessage());
				return false;
			});
	}
	
	/**
	 * Creates a LuckPerms group if it doesn't exist.
	 *
	 * @param groupName The name of the group to create
	 * @return CompletableFuture that completes with the created/existing group
	 */
	public CompletableFuture<Group> createGroup(final @NotNull String groupName) {
		return this.get().getGroupManager().loadGroup(groupName)
			.thenComposeAsync(optionalGroup -> {
				if (optionalGroup.isPresent()) {
					LOGGER.info("Group '" + groupName + "' already exists");
					return CompletableFuture.completedFuture(optionalGroup.get());
				} else {
					LOGGER.info("Creating group '" + groupName + "'...");
					return this.get().getGroupManager().createAndLoadGroup(groupName);
				}
			})
			.thenApplyAsync(group -> {
				saveGroupAsync(group);
				LOGGER.info("Group '" + groupName + "' is ready");
				return group;
			});
	}
	
	/**
	 * Checks if a group exists.
	 *
	 * @param groupName The name of the group to check
	 * @return CompletableFuture that completes with true if the group exists
	 */
	public CompletableFuture<Boolean> groupExists(final @NotNull String groupName) {
		return this.get().getGroupManager().loadGroup(groupName)
			.thenApply(Optional::isPresent);
	}
	
	@NotNull
	public Set<String> getPlayerGroups(final @NotNull Player player) {
		final User user = requireUser(player.getUniqueId());
		return getGroupNamesFromUser(user);
	}
	
	@NotNull
	public Set<String> getPlayerGroups(final @NotNull OfflinePlayer player) {
		final User user = requireUser(player);
		return getGroupNamesFromUser(user);
	}
	
	@NotNull
	@Deprecated
	public Collection<String> getPlayerGroupsCollection(final @NotNull Player player) {
		return getPlayerGroups(player);
	}
	
	@NotNull
	public CompletableFuture<Boolean> hasPermissionAsync(final @NotNull OfflinePlayer player, final @NotNull String permission) {
		return this.get().getUserManager()
		           .loadUser(player.getUniqueId())
		           .thenApply(user -> {
			           if (user == null) {
				           return false;
			           }
			           return user.getCachedData()
			                      .getPermissionData(QueryOptions.defaultContextualOptions())
			                      .checkPermission(permission)
			                      .asBoolean();
		           })
		           .exceptionally(ex -> {
			           LOGGER.log(Level.SEVERE, ("Error checking permission '" + permission + "' for " + player.getUniqueId() + ": " + ex.getMessage()));
			           return false;
		           });
	}
	
	@NotNull
	public List<String> getPermissions(final @NotNull OfflinePlayer player) {
		final User user = requireUser(player);
		return user.resolveInheritedNodes(QueryOptions.defaultContextualOptions()).stream()
		           .filter(NodeType.PERMISSION::matches)
		           .map(Node::getKey)
		           .filter(k -> !k.toLowerCase(Locale.ENGLISH).startsWith("displayname."))
		           .filter(k -> !k.toLowerCase(Locale.ENGLISH).startsWith("weight."))
		           .filter(k -> !k.toLowerCase(Locale.ENGLISH).startsWith("group."))
		           .distinct()
		           .sorted()
		           .toList();
	}
	
	public boolean hasGroup(final @NotNull Player player, final @NotNull String group) {
		if (group.isBlank()) {
			return false;
		}
		return getPlayerGroups(player).contains(group.toLowerCase(Locale.ENGLISH));
	}
	
	public boolean hasGroup(final @NotNull OfflinePlayer player, final @NotNull String group) {
		if (group.isBlank()) {
			return false;
		}
		return getPlayerGroups(player).contains(group.toLowerCase(Locale.ENGLISH));
	}
	
	public int createOrUpdateGroups(final @NotNull Map<String, List<IRank>> ranks) throws ExecutionException, InterruptedException {
		final Set<Group> currentGroups = this.get().getGroupManager().getLoadedGroups();
		final int currentGroupCount = currentGroups.size();
		final Map<String, Group> currentGroupsMap = currentGroups.stream()
		                                                         .collect(Collectors.toMap(g -> g.getName().toLowerCase(Locale.ENGLISH), g -> g));
		LOGGER.info("Creating permissions groups for " + currentGroups.size() + " rank(s)");
		
		for (final Map.Entry<String, List<IRank>> entry : ranks.entrySet()) {
			for (final IRank rank : entry.getValue()) {
				Group group = currentGroupsMap.getOrDefault(rank.id(), null);
				
				if (group == null) {
					LOGGER.log(Level.INFO, "Creating group " + rank.id() + " for " + rank);
					group = this.get().getGroupManager().createAndLoadGroup(rank.id()).get();
				}
				
				if (group.getWeight().isEmpty()) {
					LOGGER.log(Level.INFO, "Group weight is not set, setting rank weight: " + rank.weight());
					group.data().add(WeightNode.builder(rank.weight()).build());
				} else {
					if (group.getWeight().getAsInt() != rank.weight()) {
						LOGGER.log(Level.INFO, "Group weight differs (group=" + group.getWeight() + ", rank=" + rank.weight() + "). Not overriding.");
					}
				}
				
				if (group.getDisplayName() == null || group.getDisplayName().isEmpty()) {
					LOGGER.log(Level.INFO, "Group display name empty, setting to: " + rank.displayName());
					group.data().add(DisplayNameNode.builder(rank.displayName()).build());
				} else {
					if (!group.getDisplayName().equals(rank.displayName())) {
						LOGGER.log(Level.INFO, "Group display name differs (group=" + group.getDisplayName() + ", rank=" + rank.displayName() + "). Not overriding.");
					}
				}
				
				group.data().add(InheritanceNode.builder("group." + rank.id()).build());
				saveGroupAsync(group);
				
				LOGGER.log(Level.INFO, "Permissions group processed for " + rank.id());
			}
		}
		int newCount = this.get().getGroupManager().getLoadedGroups().size();
		return (newCount == currentGroupCount ? 0 : newCount - currentGroupCount);
	}
	
	public boolean checkGroupsExist(final @NotNull Map<String, IRank> ranks) {
		final Set<String> currentGroupNamesLower = this.get().getGroupManager().getLoadedGroups().stream()
		                                               .map(g -> g.getName().toLowerCase(Locale.ENGLISH))
		                                               .collect(Collectors.toSet());
		
		for (final IRank rank : ranks.values()) {
			if (!currentGroupNamesLower.contains(rank.id().toLowerCase(Locale.ENGLISH))) {
				LOGGER.info("Missing required group: " + rank.id());
				return false;
			}
		}
		return true;
	}
	
	public void setPlayerPrefix(final @NotNull Player player, final @NotNull String prefix, final int priority) {
		setPlayerMeta(player, prefix, priority, true);
	}
	
	public void setPlayerSuffix(final @NotNull Player player, final @NotNull String suffix, final int priority) {
		setPlayerMeta(player, suffix, priority, false);
	}
	
	public void removePlayerPrefix(final @NotNull Player player, final int priority) {
		removePlayerMeta(player, priority, true);
	}
	
	public void removePlayerSuffix(final @NotNull Player player, final int priority) {
		removePlayerMeta(player, priority, false);
	}
	
	private LuckPerms get() {
		return LuckPermsProvider.get();
	}
	
	private void checkArguments(final UUID uuid, final String group) {
		if (uuid == null) {
			LOGGER.severe("UUID cannot be null");
		}
		if (group == null || group.isBlank()) {
			LOGGER.severe("Group name cannot be null or blank");
		}
	}
	
	@NotNull
	private User requireUser(final UUID uuid) {
		final User user = getLuckPermsUser(uuid);
		if (user == null) {
			if (Bukkit.getPlayer(uuid) == null) {
				LOGGER.warning("LuckPerms user could not be obtained for offline player UUID: " + uuid);
				throw new IllegalStateException("LuckPerms user could not be loaded for online player UUID: " + uuid);
			}
			LOGGER.info("LuckPerms user not initially found via getUser for online player UUID: " + uuid + ". Attempting loadUser...");
			final User loadedUser = getLuckPermsUser(Bukkit.getOfflinePlayer(uuid));
			if (loadedUser == null) {
				throw new IllegalStateException("LuckPerms user could not be loaded for online player UUID: " + uuid);
			}
			return loadedUser;
		}
		return user;
	}
	
	@NotNull
	private User requireUser(final @NotNull OfflinePlayer player) {
		final User user = getLuckPermsUser(player);
		if (user == null) {
			LOGGER.info("LuckPerms user not initially found via getUser for offline player UUID: " + player.getUniqueId() + ". Attempting loadUser...");
			final User loadedUser = getLuckPermsUser(player);
			if (loadedUser == null) {
				throw new IllegalStateException("LuckPerms user could not be loaded for offline player UUID: " + player.getUniqueId());
			}
			return loadedUser;
		}
		return user;
	}
	
	private void saveUserAsync(final @NotNull User user) {
		this.get().getUserManager().saveUser(user);
	}
	
	private void saveGroupAsync(final @NotNull Group group) {
		this.get().getGroupManager().saveGroup(group);
		LOGGER.info("Saving group: " + group.getName());
	}
	
	@NotNull
	private Set<String> getGroupNamesFromUser(final @NotNull User user) {
		return user.resolveInheritedNodes(QueryOptions.defaultContextualOptions()).stream()
		           .filter(NodeType.INHERITANCE::matches)
		           .map(n -> NodeType.INHERITANCE.cast(n).getGroupName())
		           .map(n -> n.toLowerCase(Locale.ENGLISH))
		           .collect(Collectors.toUnmodifiableSet());
	}
	
	private void setPlayerMeta(final @NotNull Player player, String meta, final int priority, final boolean isPrefix) {
		if (meta == null) {
			meta = "";
		}
		meta = meta.replace("<", "{").replace(">", "}");
		final User user = requireUser(player.getUniqueId());
		final Node node = isPrefix ? PrefixNode.builder(meta, priority).build() : SuffixNode.builder(meta, priority).build();
		user.data().clear(n -> (isPrefix ? NodeType.PREFIX.matches(n) : NodeType.SUFFIX.matches(n)) && getPriorityFromMetaNode(n) == priority);
		user.data().add(node);
		saveUserAsync(user);
	}
	
	private int getPriorityFromMetaNode(Node node) {
		if (NodeType.PREFIX.matches(node)) {
			return NodeType.PREFIX.cast(node).getPriority();
		} else if (NodeType.SUFFIX.matches(node)) {
			return NodeType.SUFFIX.cast(node).getPriority();
		}
		return 0;
	}
	
	private void removePlayerMeta(final @NotNull Player player, final int priority, final boolean isPrefix) {
		final User user = requireUser(player.getUniqueId());
		java.util.function.Predicate<Node> predicate =
			node -> (isPrefix ? NodeType.PREFIX.matches(node) : NodeType.SUFFIX.matches(node)) && getPriorityFromMetaNode(node) == priority;
		boolean hadAny = user.getNodes().stream().anyMatch(predicate);
		user.data().clear(predicate);
		if (hadAny) {
			saveUserAsync(user);
		} else {
			LOGGER.info("No " + (isPrefix ? "prefix" : "suffix") + " found with priority " + priority + " to remove for player " + player.getName());
		}
	}
}