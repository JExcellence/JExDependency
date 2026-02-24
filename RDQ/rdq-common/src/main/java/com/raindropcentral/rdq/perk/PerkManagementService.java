package com.raindropcentral.rdq.perk;

import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PerkCategory;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.repository.PerkRepository;
import com.raindropcentral.rdq.database.repository.PlayerPerkRepository;
import com.raindropcentral.rdq.perk.cache.SimplePerkCache;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for managing perk ownership, enable/disable states, and queries.
 *
 * @author JExcellence
 * @version 2.0.0
 */
public class PerkManagementService {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private final PerkRepository perkRepository;
	private final PlayerPerkRepository playerPerkRepository;
	private final int maxEnabledPerksPerPlayer;
	private SimplePerkCache cache;
	
	public PerkManagementService(
			@NotNull final PerkRepository perkRepository,
			@NotNull final PlayerPerkRepository playerPerkRepository,
			final int maxEnabledPerksPerPlayer
	) {
		this.perkRepository = perkRepository;
		this.playerPerkRepository = playerPerkRepository;
		this.maxEnabledPerksPerPlayer = maxEnabledPerksPerPlayer;
	}
	
	public void setCache(@NotNull final SimplePerkCache cache) {
		this.cache = cache;
	}
	
	public CompletableFuture<PlayerPerk> grantPerk(
			@NotNull final RDQPlayer player,
			@NotNull final Perk perk,
			final boolean autoEnable
	) {
		return CompletableFuture.supplyAsync(() -> {
			Optional<PlayerPerk> existingOpt = playerPerkRepository.findByAttributes(
					Map.of("player", player, "perk", perk));
			
			if (existingOpt.isPresent()) {
				return handleExistingPerk(player, perk, existingOpt.get(), autoEnable);
			}
			return createNewPerk(player, perk, autoEnable);
		}).exceptionally(ex -> {
			LOGGER.log(Level.SEVERE, "Failed to grant perk " + perk.getIdentifier() + 
					" to " + player.getPlayerName(), ex);
			return null;
		});
	}
	
	private PlayerPerk handleExistingPerk(
			@NotNull final RDQPlayer player,
			@NotNull final Perk perk,
			@NotNull final PlayerPerk existing,
			final boolean autoEnable
	) {
		boolean needsUpdate = false;
		
		if (!existing.isUnlocked()) {
			existing.setUnlocked(true);
			needsUpdate = true;
		}
		
		if (autoEnable && !existing.isEnabled()) {
			existing.setEnabled(true);
			needsUpdate = true;
		}
		
		if (!needsUpdate) {
			return existing;
		}
		
		PlayerPerk updated = playerPerkRepository.update(existing);
		updateCache(player, updated);
		return updated;
	}
	
	private PlayerPerk createNewPerk(
			@NotNull final RDQPlayer player,
			@NotNull final Perk perk,
			final boolean autoEnable
	) {
		PlayerPerk playerPerk = new PlayerPerk(player, perk);
		playerPerk.setUnlocked(true);
		playerPerk.setEnabled(autoEnable);
		
		PlayerPerk saved = playerPerkRepository.save(playerPerk);
		updateCache(player, saved);
		return saved;
	}
	
	public CompletableFuture<Boolean> revokePerk(
			@NotNull final RDQPlayer player,
			@NotNull final Perk perk
	) {
		return CompletableFuture.supplyAsync(() -> {
			Optional<PlayerPerk> playerPerkOpt = playerPerkRepository.findByAttributes(
					Map.of("player", player, "perk", perk));
			
			if (playerPerkOpt.isEmpty()) {
				return false;
			}
			
			boolean deleted = playerPerkRepository.delete(playerPerkOpt.get().getId());
			if (deleted) {
				removeFromCache(player, perk);
			}
			return deleted;
		}).exceptionally(ex -> {
			LOGGER.log(Level.SEVERE, "Failed to revoke perk " + perk.getIdentifier() + 
					" from " + player.getPlayerName(), ex);
			return false;
		});
	}
	
	public boolean enablePerk(@NotNull final RDQPlayer player, @NotNull final Perk perk) {
		if (!isCacheLoaded(player)) {
			LOGGER.log(Level.WARNING, "Cache not loaded for {0}, attempting to load now", player.getPlayerName());
			cache.loadPlayer(player.getUniqueId());
			
			if (!isCacheLoaded(player)) {
				LOGGER.log(Level.SEVERE, "Failed to load cache for {0}", player.getPlayerName());
				return false;
			}
		}
		
		if (getEnabledPerkCount(player) >= maxEnabledPerksPerPlayer) {
			LOGGER.log(Level.FINE, "Cannot enable perk {0} for {1} - limit reached", 
					new Object[]{perk.getIdentifier(), player.getPlayerName()});
			return false;
		}
		
		PlayerPerk playerPerk = cache.getPerk(player.getUniqueId(), perk.getId());
		if (playerPerk == null || !playerPerk.isUnlocked()) {
			LOGGER.log(Level.FINE, "Cannot enable perk {0} for {1} - not found or not unlocked", 
					new Object[]{perk.getIdentifier(), player.getPlayerName()});
			return false;
		}
		
		playerPerk.setEnabled(true);
		cache.updatePerk(player.getUniqueId(), playerPerk);
		return true;
	}
	
	public boolean disablePerk(@NotNull final RDQPlayer player, @NotNull final Perk perk) {
		if (!isCacheLoaded(player)) {
			LOGGER.log(Level.WARNING, "Cache not loaded for {0}, attempting to load now", player.getPlayerName());
			cache.loadPlayer(player.getUniqueId());
			
			if (!isCacheLoaded(player)) {
				LOGGER.log(Level.SEVERE, "Failed to load cache for {0}", player.getPlayerName());
				return false;
			}
		}
		
		PlayerPerk playerPerk = cache.getPerk(player.getUniqueId(), perk.getId());
		if (playerPerk == null) {
			LOGGER.log(Level.WARNING, "Cannot disable perk {0} for {1} - perk not found in cache", 
					new Object[]{perk.getIdentifier(), player.getPlayerName()});
			return false;
		}
		
		playerPerk.setEnabled(false);
		cache.updatePerk(player.getUniqueId(), playerPerk);
		return true;
	}
	
	public boolean togglePerk(@NotNull final RDQPlayer player, @NotNull final Perk perk) {
		if (!isCacheLoaded(player)) {
			return false;
		}
		
		PlayerPerk playerPerk = cache.getPerk(player.getUniqueId(), perk.getId());
		if (playerPerk == null) {
			return false;
		}
		
		return playerPerk.isEnabled() ? disablePerk(player, perk) : enablePerk(player, perk);
	}
	
	public boolean hasUnlocked(@NotNull final RDQPlayer player, @NotNull final Perk perk) {
		if (isCacheLoaded(player)) {
			PlayerPerk playerPerk = cache.getPerk(player.getUniqueId(), perk.getId());
			return playerPerk != null && playerPerk.isUnlocked();
		}
		
		return playerPerkRepository.findByAttributes(Map.of("player", player, "perk", perk))
				.map(PlayerPerk::isUnlocked)
				.orElse(false);
	}
	
	@NotNull
	public List<PlayerPerk> getUnlockedPerks(@NotNull final RDQPlayer player) {
		if (isCacheLoaded(player)) {
			return cache.getPerks(player.getUniqueId()).stream()
					.filter(PlayerPerk::isUnlocked)
					.collect(Collectors.toList());
		}
		
		return getPerksFromDb(player).stream()
				.filter(PlayerPerk::isUnlocked)
				.collect(Collectors.toList());
	}
	
	@NotNull
	public List<PlayerPerk> getEnabledPerks(@NotNull final RDQPlayer player) {
		if (isCacheLoaded(player)) {
			return cache.getPerks(player.getUniqueId()).stream()
					.filter(PlayerPerk::isEnabled)
					.collect(Collectors.toList());
		}
		
		return getPerksFromDb(player).stream()
				.filter(PlayerPerk::isEnabled)
				.collect(Collectors.toList());
	}
	
	@NotNull
	public List<PlayerPerk> getActivePerks(@NotNull final RDQPlayer player) {
		if (isCacheLoaded(player)) {
			return cache.getPerks(player.getUniqueId()).stream()
					.filter(p -> p.isUnlocked() && p.isEnabled())
					.collect(Collectors.toList());
		}
		
		return getPerksFromDb(player).stream()
				.filter(p -> p.isUnlocked() && p.isEnabled())
				.collect(Collectors.toList());
	}
	
	@NotNull
	public Optional<PlayerPerk> getPlayerPerk(@NotNull final RDQPlayer player, @NotNull final Perk perk) {
		if (isCacheLoaded(player)) {
			return Optional.ofNullable(cache.getPerk(player.getUniqueId(), perk.getId()));
		}
		
		return playerPerkRepository.findByAttributes(Map.of("player", player, "perk", perk));
	}
	
	public int getEnabledPerkCount(@NotNull final RDQPlayer player) {
		return getEnabledPerks(player).size();
	}
	
	public boolean canEnableAnotherPerk(@NotNull final RDQPlayer player) {
		return getEnabledPerkCount(player) < maxEnabledPerksPerPlayer;
	}
	
	public int getRemainingPerkSlots(@NotNull final RDQPlayer player) {
		return Math.max(0, maxEnabledPerksPerPlayer - getEnabledPerkCount(player));
	}
	
	public int getMaxEnabledPerks() {
		return maxEnabledPerksPerPlayer;
	}
	
	@NotNull
	public List<Perk> getAvailablePerks(@Nullable final PerkCategory category) {
		if (category == null) {
			return perkRepository.findAll();
		}
		
		return perkRepository.getCachedByKey().values().stream()
				.filter(p -> p.getCategory() == category)
				.collect(Collectors.toList());
	}
	
	@NotNull
	public CompletableFuture<List<Perk>> getAvailablePerksAsync(@Nullable final PerkCategory category) {
		return CompletableFuture.supplyAsync(() -> getAvailablePerks(category));
	}
	
	private boolean isCacheLoaded(@NotNull final RDQPlayer player) {
		return cache != null && cache.isLoaded(player.getUniqueId());
	}
	
	private void updateCache(@NotNull final RDQPlayer player, @NotNull final PlayerPerk playerPerk) {
		if (cache != null && cache.isLoaded(player.getUniqueId())) {
			cache.updatePerk(player.getUniqueId(), playerPerk);
		}
	}
	
	private void removeFromCache(@NotNull final RDQPlayer player, @NotNull final Perk perk) {
		if (cache != null && cache.isLoaded(player.getUniqueId())) {
			List<PlayerPerk> perks = cache.getPerks(player.getUniqueId());
			perks.removeIf(p -> p.getPerk().getId().equals(perk.getId()));
			cache.markDirty(player.getUniqueId());
		}
	}
	
	@NotNull
	private List<PlayerPerk> getPerksFromDb(@NotNull final RDQPlayer player) {
		try {
			return playerPerkRepository.findByAttributes(Map.of("player", player))
					.map(List::of)
					.orElse(List.of());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to get perks from DB for " + player.getPlayerName(), e);
			return List.of();
		}
	}
}
