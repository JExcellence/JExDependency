package de.jexcellence.oneblock.view.island;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.oneblock.EOneblockIslandRole;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandMember;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class MembersListView extends BaseView {
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<OneblockIsland> island = initialState("island");
    private final MutableState<Integer> currentPage = mutableState(0);
    
    private static final int MEMBERS_PER_PAGE = 21;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    @Override
    protected String getKey() {
        return "members_list_ui";
    }
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "X1234567X",
            "X8901234X",
            "X5678901X",
            "X<IMRB>X"
        };
    }
    
    @Override
    protected int getSize() {
        return 5;
    }
    
    @Override
    protected Map<String, Object> getTitlePlaceholders(@NotNull OpenContext open) {
        var islandData = island.get(open);
        var memberService = plugin.get(open).getIslandMemberService();
        var allMembers = memberService.getMembersSync(islandData);
        var page = currentPage.get(open);
        
        if (page == null) {
            page = 0;
            currentPage.set(page, open);
        }
        
        var totalPages = (int) Math.ceil((double) (allMembers.size() + 1) / MEMBERS_PER_PAGE);
        
        return Map.of(
            "page", page + 1,
            "total_pages", totalPages,
            "member_count", allMembers.size() + 1,
            "online_count", getOnlineMemberCount(islandData, allMembers)
        );
    }
    
    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var islandData = island.get(render);
        var page = currentPage.get(render);
        
        if (page == null) {
            page = 0;
            currentPage.set(page, render);
        }
        
        renderMembers(render, player, islandData, page);
        renderNavigationButtons(render, player, islandData, page);
        renderBorder(render);
    }
    
    private void renderMembers(@NotNull RenderContext render, @NotNull Player player, 
                              @NotNull OneblockIsland island, int page) {
        var memberService = plugin.get(render).getIslandMemberService();
        var allMembers = memberService.getMembersSync(island);
        
        var displayMembers = new ArrayList<MemberDisplay>();
        
        var owner = Bukkit.getOfflinePlayer(island.getOwnerUuid());
        displayMembers.add(new MemberDisplay(
            island.getOwnerUuid(),
            owner.getName() != null ? owner.getName() : "Unknown",
            EOneblockIslandRole.CO_OWNER,
            island.getCreatedAt(),
            island.getCreatedAt(),
            null,
            true
        ));
        
        allMembers.stream()
            .sorted(Comparator.comparing((OneblockIslandMember m) -> getRoleOrder(convertToERole(m.getRole())))
                .thenComparing(m -> m.getJoinedAt()))
            .forEach(member -> {
                var offlinePlayer = Bukkit.getOfflinePlayer(member.getPlayerUuid());
                var playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : i18n("common.unknown_player", player).build().component().toString();
                displayMembers.add(new MemberDisplay(
                    member.getPlayerUuid(),
                    playerName,
                    convertToERole(member.getRole()),
                    member.getJoinedAt(),
                    member.getLastActivity(),
                    member.getInvitedBy() != null ? member.getInvitedBy().getUniqueId() : null,
                    false
                ));
            });
        
        var startIndex = page * MEMBERS_PER_PAGE;
        var endIndex = Math.min(startIndex + MEMBERS_PER_PAGE, displayMembers.size());
        
        char[] slots = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1'};
        
        for (int i = startIndex; i < endIndex; i++) {
            var memberDisplay = displayMembers.get(i);
            var slotIndex = i - startIndex;
            if (slotIndex < slots.length) {
                renderMemberSlot(render, player, island, memberDisplay, slots[slotIndex]);
            }
        }
    }
    
    private void renderMemberSlot(@NotNull RenderContext render, @NotNull Player player,
                                @NotNull OneblockIsland island, @NotNull MemberDisplay member, char slot) {
        var isOnline = Bukkit.getPlayer(member.uuid()) != null;
        var canManage = canManageMember(render, player, island, member);
        
        var material = member.isOwner() ? Material.GOLDEN_HELMET : 
                      isOnline ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
        
        var nameColor = member.isOwner() ? "§6" : 
                       isOnline ? "§a" : "§7";
        
        var statusIcon = member.isOwner() ? " §6👑" : 
                        isOnline ? " §a●" : " §7●";
        
        render.layoutSlot(slot, UnifiedBuilderFactory
            .item(material)
            .setName(net.kyori.adventure.text.Component.text(nameColor + member.playerName() + statusIcon + " §7(" + getRoleDisplayName(member.role()) + ")"))
            .setLore(buildMemberLore(player, member, isOnline, canManage))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            if (canManage && !member.isOwner()) {
                showMemberManagementOptions(player, island, member);
            } else if (member.isOwner()) {
                showOwnerInfo(player, member);
            } else {
                showMemberInfo(player, member, isOnline);
            }
        });
    }
    
    private List<net.kyori.adventure.text.Component> buildMemberLore(@NotNull Player player, @NotNull MemberDisplay member, 
                                       boolean isOnline, boolean canManage) {
        var loreBuilder = i18n("members.list.member.lore", player)
            .withPlaceholder("role", getRoleDisplayName(member.role()))
            .withPlaceholder("status", isOnline ? i18n("common.online", player).build().component().toString() : i18n("common.offline", player).build().component().toString())
            .withPlaceholder("joined", member.joinedAt().format(DATE_FORMAT))
            .withPlaceholder("last_activity", member.lastActivity().format(DATE_FORMAT));
        
        if (member.invitedBy() != null) {
            var inviter = Bukkit.getOfflinePlayer(member.invitedBy());
            loreBuilder.withPlaceholder("invited_by", inviter.getName() != null ? inviter.getName() : "Unknown");
        } else {
            loreBuilder.withPlaceholder("invited_by", "N/A");
        }
        
        if (canManage && !member.isOwner()) {
            loreBuilder.withPlaceholder("action", "Click to manage");
        } else {
            loreBuilder.withPlaceholder("action", "Click for details");
        }
        
        return loreBuilder.build().children().stream()
            .map(component -> (net.kyori.adventure.text.Component) component)
            .toList();
    }
    
    private void renderNavigationButtons(@NotNull RenderContext render, @NotNull Player player, 
                                       @NotNull OneblockIsland island, int page) {
        var memberService = plugin.get(render).getIslandMemberService();
        var allMembers = memberService.getMembersSync(island);
        var totalMembers = allMembers.size() + 1; // +1 for owner
        var totalPages = (int) Math.ceil((double) totalMembers / MEMBERS_PER_PAGE);
        
        // Previous Page (<)
        render.layoutSlot('<', UnifiedBuilderFactory
            .item(page > 0 ? Material.ARROW : Material.GRAY_DYE)
            .setName(i18n("common.previous", player).build().component())
            .build()
        ).onClick(ctx -> {
            if (page > 0) {
                // TODO: Fix state update - currentPage.set(ctx, page - 1);
                ctx.update();
            } else {
                i18n("members.list.first_page", player).includePrefix().build().sendMessage();
            }
        });
        
        // Invite Member (I)
        var canInvite = canInviteMembers(render, player, island);
        render.layoutSlot('I', UnifiedBuilderFactory
            .item(canInvite ? Material.WRITABLE_BOOK : Material.GRAY_DYE)
            .setName(i18n("members.list.invite.name", player).build().component())
            .setLore(canInvite ? 
                i18n("members.list.invite.lore", player).build().children() :
                i18n("members.list.invite.no_permission", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            if (canInvite) {
                // Close GUI and prompt for player name
                ctx.getPlayer().closeInventory();
                i18n("members.invite.prompt", player).includePrefix().build().sendMessage();
                // TODO: Implement chat input handler for invite
            } else {
                i18n("members.invite.no_permission", player).includePrefix().build().sendMessage();
            }
        });
        
        // Member Permissions (M)
        var canManagePermissions = canManagePermissions(render, player, island);
        render.layoutSlot('M', UnifiedBuilderFactory
            .item(canManagePermissions ? Material.BOOK : Material.GRAY_DYE)
            .setName(i18n("members.list.permissions.name", player).build().component())
            .setLore(canManagePermissions ? 
                i18n("members.list.permissions.lore", player).build().children() :
                i18n("members.list.permissions.no_permission", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            if (canManagePermissions) {
                ctx.openForPlayer(MemberPermissionView.class, ctx.getInitialData());
            } else {
                i18n("members.permissions.no_permission", player).includePrefix().build().sendMessage();
            }
        });
        
        // Refresh (R)
        render.layoutSlot('R', UnifiedBuilderFactory
            .item(Material.CLOCK)
            .setName(i18n("members.list.refresh.name", player).build().component())
            .setLore(i18n("members.list.refresh.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            ctx.update();
            i18n("members.list.refreshed", player).includePrefix().build().sendMessage();
        });
        
        // Back (B)
        render.layoutSlot('B', UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("common.back", player).build().component())
            .build()
        ).onClick(ctx -> ctx.openForPlayer(IslandMainView.class, ctx.getInitialData()));
        
        // Next Page (>)
        render.layoutSlot('>', UnifiedBuilderFactory
            .item(page < totalPages - 1 ? Material.ARROW : Material.GRAY_DYE)
            .setName(i18n("common.next", player).build().component())
            .build()
        ).onClick(ctx -> {
            if (page < totalPages - 1) {
                // TODO: Fix state update - currentPage.set(ctx, page + 1);
                ctx.update();
            } else {
                i18n("members.list.last_page", player).includePrefix().build().sendMessage();
            }
        });
    }
    
    private void renderBorder(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(net.kyori.adventure.text.Component.text(""))
            .build()
        );
    }
    
    // Helper methods
    
    private int getRoleOrder(@NotNull EOneblockIslandRole role) {
        return switch (role) {
            case CO_OWNER -> 0;
            case MODERATOR -> 1;
            case TRUSTED -> 2;
            case MEMBER -> 3;
            case VISITOR -> 4;
        };
    }
    
    private String getRoleDisplayName(@NotNull EOneblockIslandRole role) {
        return switch (role) {
            case CO_OWNER -> "Co-Owner";
            case MODERATOR -> "Moderator";
            case TRUSTED -> "Trusted";
            case MEMBER -> "Member";
            case VISITOR -> "Visitor";
        };
    }
    
    private int getOnlineMemberCount(@NotNull OneblockIsland island, @NotNull List<OneblockIslandMember> members) {
        int count = 0;
        
        // Check owner
        if (Bukkit.getPlayer(island.getOwnerUuid()) != null) {
            count++;
        }
        
        // Check members
        for (var member : members) {
            if (Bukkit.getPlayer(member.getPlayerUuid()) != null) {
                count++;
            }
        }
        
        return count;
    }
    
    private boolean canManageMember(@NotNull RenderContext render, @NotNull Player player, @NotNull OneblockIsland island, @NotNull MemberDisplay member) {
        // Owner can manage everyone except themselves
        if (island.getOwnerUuid().equals(player.getUniqueId())) {
            return !member.isOwner();
        }
        
        // Get player's role
        var memberService = plugin.get(render).getIslandMemberService();
        var oneblockPlayer = new de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer();
        oneblockPlayer.setUniqueId(player.getUniqueId());
        var playerRoleOpt = memberService.getMemberRole(island, oneblockPlayer);
        var playerRole = playerRoleOpt.map(this::convertToERole).orElse(null);
        
        if (playerRole == null) return false;
        
        // Co-owners can manage moderators and members
        if (playerRole == EOneblockIslandRole.CO_OWNER) {
            return member.role() == EOneblockIslandRole.MODERATOR || member.role() == EOneblockIslandRole.MEMBER;
        }
        
        // Moderators can manage members only
        if (playerRole == EOneblockIslandRole.MODERATOR) {
            return member.role() == EOneblockIslandRole.MEMBER;
        }
        
        return false;
    }
    
    private boolean canInviteMembers(@NotNull RenderContext render, @NotNull Player player, @NotNull OneblockIsland island) {
        // Owner can always invite
        if (island.getOwnerUuid().equals(player.getUniqueId())) {
            return true;
        }
        
        // Check member role
        var memberService = plugin.get(render).getIslandMemberService();
        var oneblockPlayer = new de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer();
        oneblockPlayer.setUniqueId(player.getUniqueId());
        var roleOpt = memberService.getMemberRole(island, oneblockPlayer);
        var role = roleOpt.map(this::convertToERole).orElse(null);
        
        return role != null && (role == EOneblockIslandRole.CO_OWNER || role == EOneblockIslandRole.MODERATOR);
    }
    
    private boolean canManagePermissions(@NotNull RenderContext render, @NotNull Player player, @NotNull OneblockIsland island) {
        // Owner can always manage permissions
        if (island.getOwnerUuid().equals(player.getUniqueId())) {
            return true;
        }
        
        // Check member role
        var memberService = plugin.get(render).getIslandMemberService();
        var oneblockPlayer = new de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer();
        oneblockPlayer.setUniqueId(player.getUniqueId());
        var roleOpt = memberService.getMemberRole(island, oneblockPlayer);
        var role = roleOpt.map(this::convertToERole).orElse(null);
        
        return role != null && role == EOneblockIslandRole.CO_OWNER;
    }
    
    private void showMemberManagementOptions(@NotNull Player player, @NotNull OneblockIsland island, @NotNull MemberDisplay member) {
        i18n("members.management.header", player)
            .withPlaceholder("member", member.playerName())
            .includePrefix()
            .build().sendMessage();
        
        i18n("members.management.options", player)
            .withPlaceholder("member", member.playerName())
            .withPlaceholder("role", getRoleDisplayName(member.role()))
            .build().sendMessage();
        
        // TODO: Implement management options (promote, demote, kick, ban)
        i18n("members.management.coming_soon", player).build().sendMessage();
    }
    
    private void showOwnerInfo(@NotNull Player player, @NotNull MemberDisplay owner) {
        i18n("members.owner.info.header", player)
            .withPlaceholder("owner", owner.playerName())
            .includePrefix()
            .build().sendMessage();
        
        i18n("members.owner.info.details", player)
            .withPlaceholder("created", owner.joinedAt().format(DATE_FORMAT))
            .withPlaceholder("last_activity", owner.lastActivity().format(DATE_FORMAT))
            .build().sendMessage();
    }
    
    private void showMemberInfo(@NotNull Player player, @NotNull MemberDisplay member, boolean isOnline) {
        i18n("members.info.header", player)
            .withPlaceholder("member", member.playerName())
            .includePrefix()
            .build().sendMessage();
        
        i18n("members.info.details", player)
            .withPlaceholder("role", getRoleDisplayName(member.role()))
            .withPlaceholder("status", isOnline ? i18n("common.online", player).build().component().toString() : i18n("common.offline", player).build().component().toString())
            .withPlaceholder("joined", member.joinedAt().format(DATE_FORMAT))
            .withPlaceholder("last_activity", member.lastActivity().format(DATE_FORMAT))
            .build().sendMessage();
        
        if (member.invitedBy() != null) {
            var inviter = Bukkit.getOfflinePlayer(member.invitedBy());
            i18n("members.info.invited_by", player)
                .withPlaceholder("inviter", inviter.getName() != null ? inviter.getName() : "Unknown")
                .build().sendMessage();
        }
    }
    
    /**
     * Record representing a member for display purposes.
     */
    private record MemberDisplay(
        UUID uuid,
        String playerName,
        EOneblockIslandRole role,
        java.time.LocalDateTime joinedAt,
        java.time.LocalDateTime lastActivity,
        UUID invitedBy,
        boolean isOwner
    ) {}
    
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
}
