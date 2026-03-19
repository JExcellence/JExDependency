package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandMember;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.database.repository.OneblockIslandMemberRepository;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class IslandInviteService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    private static final Duration DEFAULT_INVITE_EXPIRATION = Duration.ofMinutes(5);
    
    private final OneblockIslandMemberRepository memberRepository;
    private final ScheduledExecutorService scheduler;
    private final Duration inviteExpiration;
    
    public IslandInviteService(@NotNull OneblockIslandMemberRepository memberRepository) {
        this(memberRepository, DEFAULT_INVITE_EXPIRATION);
    }
    
    public IslandInviteService(
            @NotNull OneblockIslandMemberRepository memberRepository,
            @NotNull Duration inviteExpiration
    ) {
        this.memberRepository = memberRepository;
        this.inviteExpiration = inviteExpiration;
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        startExpiredInviteCleanup();
    }

    public @NotNull CompletableFuture<Boolean> sendInvite(
            @NotNull OneblockIsland island,
            @NotNull OneblockPlayer target,
            @NotNull OneblockPlayer invitedBy,
            @NotNull OneblockIslandMember.MemberRole role
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (island.isMember(target)) {
                    LOGGER.warning("Player " + target.getPlayerName() + " is already a member of island " + island.getIdentifier());
                    return false;
                }
                
                if (island.isBanned(target)) {
                    LOGGER.warning("Player " + target.getPlayerName() + " is banned from island " + island.getIdentifier());
                    return false;
                }
                
                var existingInvites = memberRepository.findByIslandAndIsActive(island, false);
                var existingInvite = existingInvites.stream()
                    .filter(m -> m.getPlayer().getId().equals(target.getId()))
                    .findFirst();
                    
                if (existingInvite.isPresent()) {
                    var invite = existingInvite.get();
                    if (invite.getInvitedAt().plus(inviteExpiration).isAfter(LocalDateTime.now())) {
                        LOGGER.warning("Player " + target.getPlayerName() + " already has a pending invitation to island " + island.getIdentifier());
                        return false;
                    } else {
                        memberRepository.deleteAsync(invite.getId());
                    }
                }
                
                var invitation = OneblockIslandMember.builder()
                    .island(island)
                    .player(target)
                    .role(role)
                    .invitedBy(invitedBy)
                    .invitedAt(LocalDateTime.now())
                    .isActive(false)
                    .build();
                
                memberRepository.create(invitation);
                
                LOGGER.info("Sent invitation to player " + target.getPlayerName() + " for island " + island.getIdentifier() + " with role " + role.name());
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to send island invitation: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Accepts a pending invitation.
     * 
     * @param invite the invitation to accept
     * @return CompletableFuture that completes with true if successful
     */
    @NotNull
    public CompletableFuture<Boolean> acceptInvite(@NotNull OneblockIslandMember invite) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if invite is still valid
                if (invite.isActive()) {
                    LOGGER.warning("Invitation for player " + invite.getPlayer().getPlayerName() + " is already accepted");
                    return false;
                }
                
                // Check if invite has expired
                if (invite.getInvitedAt().plus(inviteExpiration).isBefore(LocalDateTime.now())) {
                    LOGGER.warning("Invitation for player " + invite.getPlayer().getPlayerName() + " has expired");
                    // Remove expired invite
                    memberRepository.deleteAsync(invite.getId());
                    return false;
                }
                
                // Check if player is now banned (could have been banned after invite was sent)
                if (invite.getIsland().isBanned(invite.getPlayer())) {
                    LOGGER.warning("Player " + invite.getPlayer().getPlayerName() + " is now banned from island " + invite.getIsland().getIdentifier());
                    memberRepository.deleteAsync(invite.getId());
                    return false;
                }
                
                // Accept the invitation
                invite.acceptInvitation();
                
                // Add to island's member collection (for legacy compatibility)
                invite.getIsland().addMember(invite.getPlayer());
                
                // Save to database
                memberRepository.create(invite);
                
                LOGGER.info("Player " + invite.getPlayer().getPlayerName() + " accepted invitation to island " + invite.getIsland().getIdentifier());
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to accept island invitation: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Declines a pending invitation.
     * 
     * @param invite the invitation to decline
     * @return CompletableFuture that completes with true if successful
     */
    @NotNull
    public CompletableFuture<Boolean> declineInvite(@NotNull OneblockIslandMember invite) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if invite is still pending
                if (invite.isActive()) {
                    LOGGER.warning("Invitation for player " + invite.getPlayer().getPlayerName() + " is already accepted");
                    return false;
                }
                
                // Remove the invitation
                memberRepository.deleteAsync(invite.getId());
                
                LOGGER.info("Player " + invite.getPlayer().getPlayerName() + " declined invitation to island " + invite.getIsland().getIdentifier());
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to decline island invitation: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Gets all pending invitations for a player.
     * 
     * @param player the player to get invitations for
     * @return CompletableFuture containing list of pending invitations
     */
    @NotNull
    public CompletableFuture<List<OneblockIslandMember>> getPendingInvites(@NotNull OneblockPlayer player) {
        return memberRepository.findByPlayerAsync(player.getUniqueId())
            .thenApply(memberships -> memberships.stream()
                .filter(m -> !m.isActive()) // Pending invites are not active
                .filter(m -> m.getInvitedAt().plus(inviteExpiration).isAfter(LocalDateTime.now())) // Not expired
                .toList());
    }
    
    /**
     * Gets all pending invitations for an island.
     * 
     * @param island the island to get invitations for
     * @return CompletableFuture containing list of pending invitations
     */
    @NotNull
    public CompletableFuture<List<OneblockIslandMember>> getPendingInvitesForIsland(@NotNull OneblockIsland island) {
        return memberRepository.findByIslandAndIsActive(island, false)
            .stream()
            .filter(m -> m.getInvitedAt().plus(inviteExpiration).isAfter(LocalDateTime.now())) // Not expired
            .collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(),
                CompletableFuture::completedFuture
            ));
    }
    
    /**
     * Cleans up expired invitations.
     * This method is called periodically to remove expired invitations.
     */
    public void cleanupExpiredInvites() {
        memberRepository.findAll().stream()
            .filter(m -> !m.isActive()) // Pending invites
            .filter(m -> m.getInvitedAt().plus(inviteExpiration).isBefore(LocalDateTime.now())) // Expired
            .forEach(invite -> {
                memberRepository.deleteAsync(invite.getId());
                LOGGER.fine("Removed expired invitation for player " + invite.getPlayer().getPlayerName() + 
                           " to island " + invite.getIsland().getIdentifier());
            });
    }
    
    /**
     * Gets the configured invitation expiration duration.
     * 
     * @return the invitation expiration duration
     */
    @NotNull
    public Duration getInviteExpiration() {
        return inviteExpiration;
    }
    
    /**
     * Cancels a pending invitation.
     * 
     * @param island the island the invitation is for
     * @param target the player the invitation was sent to
     * @param cancelledBy the player cancelling the invitation
     * @return CompletableFuture that completes with true if successful
     */
    @NotNull
    public CompletableFuture<Boolean> cancelInvite(
            @NotNull OneblockIsland island,
            @NotNull OneblockPlayer target,
            @NotNull OneblockPlayer cancelledBy
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var inviteOpt = memberRepository.findByIslandAndPlayerAndIsActive(island, target, false);
                if (inviteOpt.isEmpty()) {
                    LOGGER.warning("No pending invitation found for player " + target.getPlayerName() + " to island " + island.getIdentifier());
                    return false;
                }
                
                var invite = inviteOpt.get();
                memberRepository.deleteAsync(invite.getId());
                
                LOGGER.info("Cancelled invitation for player " + target.getPlayerName() + " to island " + island.getIdentifier());
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to cancel island invitation: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Gets the count of pending invitations for an island.
     * 
     * @param island the island to count invitations for
     * @return CompletableFuture containing the invitation count
     */
    @NotNull
    public CompletableFuture<Integer> getPendingInviteCount(@NotNull OneblockIsland island) {
        return getPendingInvitesForIsland(island)
            .thenApply(List::size);
    }
    
    /**
     * Starts the periodic cleanup of expired invitations.
     */
    private void startExpiredInviteCleanup() {
        // Run cleanup every 2 minutes
        scheduler.scheduleAtFixedRate(
            this::cleanupExpiredInvites,
            2, // Initial delay
            2, // Period
            TimeUnit.MINUTES
        );
        
        LOGGER.info("Started expired invite cleanup scheduler (runs every 2 minutes)");
    }
    
    /**
     * Shuts down the invite service and cleanup scheduler.
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("Island invite service shut down");
    }
}