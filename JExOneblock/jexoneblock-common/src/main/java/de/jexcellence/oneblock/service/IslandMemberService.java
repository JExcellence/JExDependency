package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandMember;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.database.repository.OneblockIslandMemberRepository;
import de.jexcellence.oneblock.manager.permission.TrustLevel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class IslandMemberService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final OneblockIslandMemberRepository memberRepository;
    
    public IslandMemberService(@NotNull OneblockIslandMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public @NotNull CompletableFuture<Boolean> addMember(
            @NotNull OneblockIsland island,
            @NotNull OneblockPlayer player,
            @NotNull OneblockIslandMember.MemberRole role,
            @NotNull OneblockPlayer invitedBy
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (island.isMember(player)) {
                    LOGGER.warning("Player " + player.getPlayerName() + " is already a member of island " + island.getIdentifier());
                    return false;
                }
                
                if (island.isBanned(player)) {
                    LOGGER.warning("Player " + player.getPlayerName() + " is banned from island " + island.getIdentifier());
                    return false;
                }
                
                var member = OneblockIslandMember.builder()
                    .island(island)
                    .player(player)
                    .role(role)
                    .invitedBy(invitedBy)
                    .invitedAt(java.time.LocalDateTime.now())
                    .isActive(true)
                    .build();
                
                memberRepository.create(member);
                
                island.addMember(player);
                
                LOGGER.info("Added player " + player.getPlayerName() + " as " + role.name() + " to island " + island.getIdentifier());
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to add member to island: " + e.getMessage());
                return false;
            }
        });
    }

    public @NotNull CompletableFuture<Boolean> removeMember(
            @NotNull OneblockIsland island,
            @NotNull OneblockPlayer player
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!island.isMember(player)) {
                    LOGGER.warning("Player " + player.getPlayerName() + " is not a member of island " + island.getIdentifier());
                    return false;
                }
                
                if (island.isOwner(player)) {
                    LOGGER.warning("Cannot remove island owner " + player.getPlayerName() + " from island " + island.getIdentifier());
                    return false;
                }
                
                island.removeMemberEntity(player);
                
                island.removeMember(player);
                
                memberRepository.findByIslandAndPlayerAsync(island, player)
                    .thenAccept(opt -> opt.ifPresent(member -> {
                        member.leave();
                        memberRepository.create(member);
                    }));
                
                LOGGER.info("Removed player " + player.getPlayerName() + " from island " + island.getIdentifier());
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to remove member from island: " + e.getMessage());
                return false;
            }
        });
    }
    
    public @NotNull CompletableFuture<Boolean> updateRole(
            @NotNull OneblockIslandMember member,
            @NotNull OneblockIslandMember.MemberRole newRole
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var oldRole = member.getRole();
                member.setRole(newRole);
                
                memberRepository.create(member);
                
                LOGGER.info("Updated role for player " + member.getPlayer().getPlayerName() + 
                           " from " + oldRole.name() + " to " + newRole.name() + 
                           " on island " + member.getIsland().getIdentifier());
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to update member role: " + e.getMessage());
                return false;
            }
        });
    }
    
    public @NotNull CompletableFuture<List<OneblockIslandMember>> getMembers(@NotNull OneblockIsland island) {
        return memberRepository.findByIslandAsync(island);
    }
    
    public @NotNull List<OneblockIslandMember> getMembersSync(@NotNull OneblockIsland island) {
        return memberRepository.findByIsland(island.getId());
    }
    
    public @NotNull CompletableFuture<List<OneblockIslandMember>> getActiveMembers(@NotNull OneblockIsland island) {
        return memberRepository.findByIslandAsync(island)
            .thenApply(members -> members.stream()
                .filter(OneblockIslandMember::isActive)
                .toList());
    }
    
    public @NotNull Optional<OneblockIslandMember.MemberRole> getMemberRole(
            @NotNull OneblockIsland island,
            @NotNull OneblockPlayer player
    ) {
        if (island.isOwner(player)) {
            return Optional.of(OneblockIslandMember.MemberRole.CO_OWNER);
        }
        
        return island.getIslandMembers().stream()
            .filter(member -> member.getPlayer().equals(player) && member.isActive())
            .map(OneblockIslandMember::getRole)
            .findFirst();
    }
    
    public @NotNull TrustLevel getTrustLevel(@NotNull OneblockIsland island, @NotNull OneblockPlayer player) {
        if (island.isOwner(player)) {
            return TrustLevel.OWNER;
        }
        
        if (island.isBanned(player)) {
            return TrustLevel.NONE;
        }
        
        var roleOpt = getMemberRole(island, player);
        if (roleOpt.isPresent()) {
            return switch (roleOpt.get()) {
                case VISITOR -> TrustLevel.VISITOR;
                case MEMBER -> TrustLevel.MEMBER;
                case TRUSTED -> TrustLevel.TRUSTED;
                case MODERATOR -> TrustLevel.MODERATOR;
                case CO_OWNER -> TrustLevel.CO_OWNER;
            };
        }
        
        return island.isPrivacy() ? TrustLevel.NONE : TrustLevel.VISITOR;
    }
    
    public boolean canPerformAction(
            @NotNull OneblockIsland island,
            @NotNull OneblockPlayer player,
            @NotNull TrustLevel requiredLevel
    ) {
        var playerLevel = getTrustLevel(island, player);
        return playerLevel.isAtLeast(requiredLevel);
    }
    
    public @NotNull CompletableFuture<Integer> getMemberCount(@NotNull OneblockIsland island) {
        return getActiveMembers(island)
            .thenApply(List::size);
    }
    
    public @NotNull CompletableFuture<Boolean> isAtMemberLimit(@NotNull OneblockIsland island) {
        return getMemberCount(island)
            .thenApply(count -> {
                var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("JExOneblock");
                int memberLimit = 10;
                
                if (plugin instanceof de.jexcellence.oneblock.JExOneblock jexPlugin) {
                    var upgradeService = new de.jexcellence.oneblock.service.UpgradeService();
                    var memberSlotLevel = upgradeService.getCurrentLevel(island, 
                        de.jexcellence.oneblock.service.UpgradeService.UpgradeType.MEMBER_SLOTS);
                    memberLimit = 10 + (memberSlotLevel * 5);
                }
                
                return count >= memberLimit;
            });
    }
}