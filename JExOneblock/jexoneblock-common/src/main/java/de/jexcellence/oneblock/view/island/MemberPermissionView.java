package de.jexcellence.oneblock.view.island;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandMember;
import de.jexcellence.oneblock.database.entity.oneblock.EOneblockIslandRole;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MemberPermissionView extends BaseView {
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<OneblockIsland> island = initialState("island");
    private final State<Integer> currentPage = initialState("currentPage");
    private final State<UUID> selectedMember = initialState("selectedMember");
    
    private static final int MEMBERS_PER_PAGE = 21;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    @Override
    protected String getKey() {
        return "member_permission_ui";
    }
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "X1234567X",
            "X8901234X",
            "X5678901X",
            "X<RSAB>X"
        };
    }
    
    @Override
    protected int getSize() {
        return 5;
    }
    
    @Override
    protected Map<String, Object> getTitlePlaceholders(@NotNull OpenContext open) {
        var islandData = island.get(open);
        var page = currentPage.get(open);
        
        return Map.of(
            "page", page + 1,
            "total_pages", 1,
            "member_count", 0
        );
    }
    
    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var islandData = island.get(render);
        
        if (!canManagePermissions(player, islandData)) {
            i18n("member.permissions.no_access", player).includePrefix().build().sendMessage();
            player.closeInventory();
            return;
        }
        
        var page = currentPage.get(render);
        var selected = selectedMember.get(render);
        
        if (selected != null) {
            renderRoleSelection(render, player, islandData, selected);
        } else {
            renderMemberSelection(render, player, islandData, page);
        }
        
        renderNavigationButtons(render, player, islandData, page);
        renderBorder(render);
    }
    
    private void renderMemberSelection(@NotNull RenderContext render, @NotNull Player player, 
                                     @NotNull OneblockIsland island, int page) {
        var memberService = plugin.get(render).getIslandMemberService();
        
        // Get members asynchronously and update UI when ready
        memberService.getMembers(island).thenAccept(allMembers -> {
            var manageableMembers = allMembers.stream()
                .filter(member -> canManageMemberRole(player, island, convertToERole(member.getRole())))
                .sorted(Comparator.comparing((OneblockIslandMember m) -> getRoleOrder(convertToERole(m.getRole())))
                    .thenComparing(m -> m.getJoinedAt() != null ? m.getJoinedAt() : m.getInvitedAt()))
                .toList();
            
            var startIndex = page * MEMBERS_PER_PAGE;
            var endIndex = Math.min(startIndex + MEMBERS_PER_PAGE, manageableMembers.size());
            
            char[] slots = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1'};
            
            org.bukkit.Bukkit.getScheduler().runTask((org.bukkit.plugin.Plugin) plugin.get(render), () -> {
                for (int i = startIndex; i < endIndex; i++) {
                    var member = manageableMembers.get(i);
                    var slotIndex = i - startIndex;
                    if (slotIndex < slots.length) {
                        renderMemberSelectionSlot(render, player, island, member, slots[slotIndex]);
                    }
                }
                render.update();
            });
        });
    }
    
    private void renderMemberSelectionSlot(@NotNull RenderContext render, @NotNull Player player,
                                         @NotNull OneblockIsland island, @NotNull OneblockIslandMember member, char slot) {
        var offlinePlayer = Bukkit.getOfflinePlayer(member.getPlayerUuid());
        var isOnline = Bukkit.getPlayer(member.getPlayerUuid()) != null;
        var playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
        
        var material = isOnline ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
        var nameColor = isOnline ? "§a" : "§7";
        var statusIcon = isOnline ? " §a●" : " §7●";
        
        render.layoutSlot(slot, UnifiedBuilderFactory
            .item(material)
            .setName(Component.text(nameColor + playerName + statusIcon + " §7(" + i18n(convertToERole(member.getRole()).getDisplayNameKey(), player).build().component().toString() + ")"))
            .setLore(buildMemberSelectionLore(player, member, isOnline))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            ctx.update();
        });
    }
    
    private List<net.kyori.adventure.text.Component> buildMemberSelectionLore(@NotNull Player player, @NotNull OneblockIslandMember member, boolean isOnline) {
        var inviterName = "N/A";
        if (member.getInvitedBy() != null) {
            var inviter = Bukkit.getOfflinePlayer(member.getInvitedBy().getUuid());
            inviterName = inviter.getName() != null ? inviter.getName() : "Unknown";
        }
        
        var loreStrings = i18n("member.permissions.selection.lore", player)
            .withPlaceholder("role", i18n(convertToERole(member.getRole()).getDisplayNameKey(), player).build().component().toString())
            .withPlaceholder("status", isOnline ? "Online" : "Offline")
            .withPlaceholder("joined", member.getJoinedAt() != null ? member.getJoinedAt().format(DATE_FORMAT) : "Not joined")
            .withPlaceholder("invited_by", inviterName)
            .withPlaceholder("action", "Click to manage role")
            .build().children();
        
        return loreStrings.stream()
            .map(component -> (net.kyori.adventure.text.Component) component)
            .toList();
    }
    
    private void renderRoleSelection(@NotNull RenderContext render, @NotNull Player player,
                                   @NotNull OneblockIsland island, @NotNull UUID memberUuid) {
        var memberService = plugin.get(render).getIslandMemberService();
        
        // Get members asynchronously
        memberService.getMembers(island).thenAccept(members -> {
            var member = members.stream()
                .filter(m -> m.getPlayerUuid().equals(memberUuid))
                .findFirst()
                .orElse(null);
            
            if (member == null) {
                // Simplified state update - would need proper context handling
                // selectedMember.set(render, null);
                render.update();
                return;
            }
            
            var offlinePlayer = Bukkit.getOfflinePlayer(memberUuid);
            var playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            var currentRole = convertToERole(member.getRole());
            var availableRoles = getAvailableRoles(player, island, currentRole);
            
            // Schedule UI update on main thread
            org.bukkit.Bukkit.getScheduler().runTask((org.bukkit.plugin.Plugin) plugin.get(render), () -> {
                // Role slots
                char[] roleSlots = {'1', '2', '3', '4', '5'};
                
                for (int i = 0; i < Math.min(availableRoles.size(), roleSlots.length); i++) {
                    var role = availableRoles.get(i);
                    renderRoleSlot(render, player, island, member, role, roleSlots[i], currentRole == role);
                }
                
                // Member info slot
                render.layoutSlot('6', UnifiedBuilderFactory
                    .item(Material.PLAYER_HEAD)
                    .setName(Component.text("§e" + playerName + " §7(Current: " + getRoleDisplayName(currentRole, player) + ")"))
                    .setLore(buildSelectedMemberLore(player, member))
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build()
                );
                
                render.update();
            });
        });
    }
    
    private void renderRoleSlot(@NotNull RenderContext render, @NotNull Player player,
                              @NotNull OneblockIsland island, @NotNull OneblockIslandMember member,
                              @NotNull EOneblockIslandRole role, char slot, boolean isCurrent) {
        var material = getRoleMaterial(role);
        var nameColor = isCurrent ? "§a" : "§f";
        var status = isCurrent ? " §7(Current)" : "";
        
        render.layoutSlot(slot, UnifiedBuilderFactory
            .item(material)
            .setName(net.kyori.adventure.text.Component.text(nameColor + getRoleDisplayName(role, player) + status))
            .setLore(buildRoleLore(player, role, isCurrent))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            if (!isCurrent) {
                changeRole(player, island, member, role);
                // Simplified state update - would need proper context handling
                // selectedMember.set(ctx, null);
                ctx.update();
            }
        });
    }
    
    private List<net.kyori.adventure.text.Component> buildSelectedMemberLore(@NotNull Player player, @NotNull OneblockIslandMember member) {
        var isOnline = Bukkit.getPlayer(member.getPlayerUuid()) != null;
        var inviterName = "N/A";
        if (member.getInvitedBy() != null) {
            var inviter = Bukkit.getOfflinePlayer(member.getInvitedBy().getUuid());
            inviterName = inviter.getName() != null ? inviter.getName() : "Unknown";
        }
        
        var loreStrings = i18n("member.permissions.selected.lore", player)
            .withPlaceholder("status", isOnline ? "Online" : "Offline")
            .withPlaceholder("joined", member.getJoinedAt() != null ? member.getJoinedAt().format(DATE_FORMAT) : "Not joined")
            .withPlaceholder("last_activity", member.getLastActivity().format(DATE_FORMAT))
            .withPlaceholder("invited_by", inviterName)
            .build().children();
        
        // Convert List<TextComponent> to List<Component>
        return loreStrings.stream()
            .map(component -> (net.kyori.adventure.text.Component) component)
            .toList();
    }
    
    private List<net.kyori.adventure.text.Component> buildRoleLore(@NotNull Player player, @NotNull EOneblockIslandRole role, boolean isCurrent) {
        var permissions = getRolePermissions(role);
        var loreBuilder = i18n("member.permissions.role.lore", player)
            .withPlaceholder("permissions", String.join(", ", permissions));
        
        if (isCurrent) {
            loreBuilder.withPlaceholder("action", "Current role");
        } else {
            loreBuilder.withPlaceholder("action", "Click to assign");
        }
        
        var loreStrings = loreBuilder.build().children();
        
        // Convert List<TextComponent> to List<Component>
        return loreStrings.stream()
            .map(component -> (net.kyori.adventure.text.Component) component)
            .toList();
    }
    
    private void renderNavigationButtons(@NotNull RenderContext render, @NotNull Player player,
                                       @NotNull OneblockIsland island, int page) {
        var selected = selectedMember.get(render);
        
        if (selected != null) {
            // Back to member selection (<)
            render.layoutSlot('<', UnifiedBuilderFactory
                .item(Material.ARROW)
                .setName(i18n("member.permissions.back_to_selection", player).build().component())
                .build()
            ).onClick(ctx -> {
                // Simplified state update - would need proper context handling
                // selectedMember.set(ctx, null);
                ctx.update();
            });
        } else {
            // Pagination for member selection - simplified to avoid async complexity in navigation
            var totalPages = 1; // Simplified - would need async handling for accurate count
            
            // Previous Page (<)
            render.layoutSlot('<', UnifiedBuilderFactory
                .item(page > 0 ? Material.ARROW : Material.GRAY_DYE)
                .setName(i18n("common.previous", player).build().component())
                .build()
            ).onClick(ctx -> {
                if (page > 0) {
                    // Simplified state update - would need proper context handling
                    // currentPage.set(ctx, page - 1);
                    ctx.update();
                }
            });
        }
        
        // Refresh (R)
        render.layoutSlot('R', UnifiedBuilderFactory
            .item(Material.CLOCK)
            .setName(i18n("member.permissions.refresh.name", player).build().component())
            .setLore(i18n("member.permissions.refresh.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            // Simplified state update - would need proper context handling
            // selectedMember.set(ctx, null);
            // currentPage.set(ctx, 0);
            ctx.update();
            i18n("member.permissions.refreshed", player).includePrefix().build().sendMessage();
        });
        
        // Role Guide (S)
        render.layoutSlot('S', UnifiedBuilderFactory
            .item(Material.BOOK)
            .setName(i18n("member.permissions.guide.name", player).build().component())
            .setLore(i18n("member.permissions.guide.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> showRoleGuide(player));
        
        // Activity Log (A)
        render.layoutSlot('A', UnifiedBuilderFactory
            .item(Material.WRITABLE_BOOK)
            .setName(i18n("member.permissions.activity.name", player).build().component())
            .setLore(i18n("member.permissions.activity.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            // TODO: Implement activity log view
            i18n("member.permissions.activity.coming_soon", player).includePrefix().build().sendMessage();
        });
        
        // Back (B)
        render.layoutSlot('B', UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("common.back", player).build().component())
            .build()
        ).onClick(ctx -> ctx.openForPlayer(MembersListView.class, ctx.getInitialData()));
    }
    
    private void renderBorder(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.text(""))
            .build()
        );
    }
    
    // Helper methods
    
    private boolean canManagePermissions(@NotNull Player player, @NotNull OneblockIsland island) {
        // Owner can always manage permissions
        if (island.getOwnerUuid().equals(player.getUniqueId())) {
            return true;
        }
        
        // Simplified - would check actual member role in full implementation
        return false; // Only owner for now
    }
    
    private boolean canManageMemberRole(@NotNull Player player, @NotNull OneblockIsland island, @NotNull EOneblockIslandRole memberRole) {
        // Owner can manage everyone except other co-owners
        if (island.getOwnerUuid().equals(player.getUniqueId())) {
            return memberRole != EOneblockIslandRole.CO_OWNER;
        }
        
        // Simplified - assume co-owners can manage moderators and members
        // In a full implementation, this would check the actual member role
        return false; // Simplified for compilation
    }
    
    private List<EOneblockIslandRole> getAvailableRoles(@NotNull Player player, @NotNull OneblockIsland island, @NotNull EOneblockIslandRole currentRole) {
        var availableRoles = new ArrayList<EOneblockIslandRole>();
        
        // Owner can assign any role except owner
        if (island.getOwnerUuid().equals(player.getUniqueId())) {
            availableRoles.add(EOneblockIslandRole.CO_OWNER);
            availableRoles.add(EOneblockIslandRole.MODERATOR);
            availableRoles.add(EOneblockIslandRole.MEMBER);
        }
        // Simplified - in full implementation would check actual player role
        
        return availableRoles;
    }
    
    private void changeRole(@NotNull Player player, @NotNull OneblockIsland island, 
                          @NotNull OneblockIslandMember member, @NotNull EOneblockIslandRole newRole) {
        var offlinePlayer = Bukkit.getOfflinePlayer(member.getPlayerUuid());
        var playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
        var oldRole = convertToERole(member.getRole());
        
        try {
            // Convert EOneblockIslandRole to MemberRole and update
            var newMemberRole = convertToMemberRole(newRole);
            // Simplified - would use member service in full implementation
            member.setRole(newMemberRole);
            
            // Success message
            i18n("member.permissions.role_changed", player)
                .withPlaceholder("member", playerName)
                .withPlaceholder("old_role", getRoleDisplayName(oldRole, player))
                .withPlaceholder("new_role", getRoleDisplayName(newRole, player))
                .includePrefix()
                .build().sendMessage();
            
            // Notify the member if online
            var targetPlayer = Bukkit.getPlayer(member.getPlayerUuid());
            if (targetPlayer != null) {
                i18n("member.permissions.role_changed_notification", targetPlayer)
                    .withPlaceholder("island", island.getIslandName() != null ? island.getIslandName() : "Island")
                    .withPlaceholder("old_role", getRoleDisplayName(oldRole, targetPlayer))
                    .withPlaceholder("new_role", getRoleDisplayName(newRole, targetPlayer))
                    .withPlaceholder("changed_by", player.getName())
                    .includePrefix()
                    .build().sendMessage();
            }
            
        } catch (Exception e) {
            i18n("member.permissions.role_change_failed", player)
                .withPlaceholder("member", playerName)
                .withPlaceholder("error", e.getMessage())
                .includePrefix()
                .build().sendMessage();
        }
    }
    
    private int getRoleOrder(@NotNull EOneblockIslandRole role) {
        return switch (role) {
            case CO_OWNER -> 0;
            case MODERATOR -> 1;
            case TRUSTED -> 2;
            case MEMBER -> 3;
            case VISITOR -> 4;
        };
    }
    
    private String getRoleDisplayName(@NotNull EOneblockIslandRole role, @NotNull Player player) {
        return i18n(role.getDisplayNameKey(), player).build().component().toString();
    }
    
    private Material getRoleMaterial(@NotNull EOneblockIslandRole role) {
        return switch (role) {
            case CO_OWNER -> Material.GOLDEN_HELMET;
            case MODERATOR -> Material.DIAMOND_HELMET;
            case TRUSTED -> Material.IRON_HELMET;
            case MEMBER -> Material.LEATHER_HELMET;
            case VISITOR -> Material.CHAINMAIL_HELMET;
        };
    }
    
    private List<String> getRolePermissions(@NotNull EOneblockIslandRole role) {
        return switch (role) {
            case CO_OWNER -> List.of("Almost all permissions", "Manage members", "Island settings", "Visitor permissions", "Upgrades");
            case MODERATOR -> List.of("Manage members and island settings", "Invite members", "Kick members", "Ban visitors", "Basic settings");
            case TRUSTED -> List.of("Extended permissions", "Invite friends", "Use advanced features");
            case MEMBER -> List.of("Basic permissions", "Build", "Use items", "Invite friends");
            case VISITOR -> List.of("Visit only", "View public areas");
        };
    }
    
    private void showRoleGuide(@NotNull Player player) {
        i18n("member.permissions.guide.header", player).includePrefix().build().sendMessage();
        
        for (var role : EOneblockIslandRole.values()) {
            if (role == EOneblockIslandRole.VISITOR) continue; // Skip visitor role in guide
            
            var permissions = String.join(", ", getRolePermissions(role));
            i18n("member.permissions.guide.role", player)
                .withPlaceholder("role", getRoleDisplayName(role, player))
                .withPlaceholder("permissions", permissions)
                .build().sendMessage();
        }
        
        i18n("member.permissions.guide.footer", player).build().sendMessage();
    }
    
    // Helper methods for role conversion
    
    /**
     * Converts OneblockIslandMember.MemberRole to EOneblockIslandRole
     */
    private EOneblockIslandRole convertToERole(OneblockIslandMember.MemberRole memberRole) {
        return switch (memberRole) {
            case VISITOR -> EOneblockIslandRole.VISITOR;
            case MEMBER -> EOneblockIslandRole.MEMBER;
            case TRUSTED -> EOneblockIslandRole.TRUSTED;
            case MODERATOR -> EOneblockIslandRole.MODERATOR;
            case CO_OWNER -> EOneblockIslandRole.CO_OWNER;
        };
    }
    
    /**
     * Converts EOneblockIslandRole to OneblockIslandMember.MemberRole
     */
    private OneblockIslandMember.MemberRole convertToMemberRole(EOneblockIslandRole eRole) {
        return switch (eRole) {
            case VISITOR -> OneblockIslandMember.MemberRole.VISITOR;
            case MEMBER -> OneblockIslandMember.MemberRole.MEMBER;
            case TRUSTED -> OneblockIslandMember.MemberRole.TRUSTED;
            case MODERATOR -> OneblockIslandMember.MemberRole.MODERATOR;
            case CO_OWNER -> OneblockIslandMember.MemberRole.CO_OWNER;
        };
    }
}
