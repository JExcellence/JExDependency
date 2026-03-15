package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.database.converter.LocationConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a player-created town in the server.
 *
 * <p>A town has a stable public UUID, a mayor (creator) UUID, a unique human-friendly name,
 * a spawn location, members, and claimed chunks. Role definitions are persisted on the town so
 * member permissions can be derived from each member's role ID.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.10
 */
@Entity
@Table(name = "towns")
@SuppressWarnings({
        "DefaultAnnotationParam",
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
public class RTown extends BaseEntity {

    /** Built-in role ID assigned to the town creator. */
    public static final String MAYOR_ROLE_ID = "MAYOR";
    /** Built-in role ID assigned to regular members. */
    public static final String MEMBER_ROLE_ID = "MEMBER";
    /** Built-in role ID used for non-member/public access. */
    public static final String PUBLIC_ROLE_ID = "PUBLIC";
    /** Built-in role ID that denies all actions, including mayor-bypass checks. */
    public static final String RESTRICTED_ROLE_ID = "RESTRICTED";

    private static final String MAYOR_ROLE_NAME = "Mayor";
    private static final String MEMBER_ROLE_NAME = "Member";
    private static final String PUBLIC_ROLE_NAME = "Public";
    private static final String RESTRICTED_ROLE_NAME = "Restricted";

    @Column(name = "uuid", unique = true, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID uuid;

    @Column(name = "mayor", unique = true, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID mayor;

    @Column(name = "active", unique = false, nullable = false)
    private boolean active;

    /** All chunks claimed by this town. */
    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            mappedBy = "town"
    )
    private final List<RChunk> chunks = new ArrayList<>();

    @Column(name = "town_name", unique = true, nullable = false)
    private String townName;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "town_uuid", referencedColumnName = "uuid")
    private final Set<RDTPlayer> members = new HashSet<>();

    /** All role definitions available in this town. */
    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            mappedBy = "town"
    )
    private final List<TownRole> roles = new ArrayList<>();

    /** Players explicitly invited to join this town. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "town_invites", joinColumns = @JoinColumn(name = "town_id"))
    @Column(name = "player_uuid", nullable = false)
    private final Set<String> invitedPlayers = new HashSet<>();

    /** Players that requested to join this town. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "town_join_requests", joinColumns = @JoinColumn(name = "town_id"))
    @Column(name = "player_uuid", nullable = false)
    private final Set<String> pendingJoinRequests = new HashSet<>();

    /** Role requirements for town-wide protection checks. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "town_protections", joinColumns = @JoinColumn(name = "town_id"))
    @MapKeyColumn(name = "protection_key")
    @Column(name = "role_id", nullable = false)
    private final Map<String, String> protections = new HashMap<>();

    /** Ledger events for joins, leaves, and role changes. */
    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            mappedBy = "town"
    )
    private final List<TownLedgerEntry> ledgerEntries = new ArrayList<>();

    /** Multi-currency balances stored for this town bank. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "town_bank_entries",
            joinColumns = @JoinColumn(name = "town_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"town_id", "currency_id"})
    )
    private final List<RTownBank> bankEntries = new ArrayList<>();

    /** Stored town level. Defaults to {@code 1}. */
    @Column(name = "town_level", unique = false, nullable = false)
    private int town_level;

    /** Persistent partial progress for town-level requirements. */
    @OneToMany(
            mappedBy = "town",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("progressKey ASC")
    private List<TownLevelRequirementProgress> levelRequirementProgress = new ArrayList<>();

    @Column(name = "founded", unique = false, nullable = false)
    private long founded;

    @Column(name = "nexus_location", unique = false, nullable = true)
    @Convert(converter = LocationConverter.class)
    private Location nexus_location;

    @Column(name = "town_spawn_location", unique = false, nullable = true)
    @Convert(converter = LocationConverter.class)
    private Location town_spawn_location;

    /** Required by Hibernate. */
    protected RTown() {
    }

    /**
     * Creates a new town record and seeds built-in Public, Member, Mayor, and Restricted roles.
     *
     * @param uuid public town UUID
     * @param mayor creator/mayor UUID
     * @param townName unique display name
     * @param nexusLocation initial nexus location, or {@code null} when not placed
     */
    public RTown(
            final UUID uuid,
            final UUID mayor,
            final String townName,
            final @Nullable Location nexusLocation
    ) {
        this(uuid, mayor, townName, nexusLocation, nexusLocation);
    }

    /**
     * Creates a new town record with explicit nexus and town-spawn locations.
     *
     * @param uuid public town UUID
     * @param mayor creator/mayor UUID
     * @param townName unique display name
     * @param nexusLocation initial nexus location, or {@code null} when not placed
     * @param townSpawnLocation initial town spawn location, or {@code null} when not set
     */
    public RTown(
            final UUID uuid,
            final UUID mayor,
            final String townName,
            final @Nullable Location nexusLocation,
            final @Nullable Location townSpawnLocation
    ) {
        this.uuid = uuid;
        this.mayor = mayor;
        this.townName = townName;
        this.founded = System.currentTimeMillis();
        this.nexus_location = nexusLocation;
        this.town_spawn_location = townSpawnLocation;
        this.town_level = 1;
        this.active = true;
        this.ensureDefaultRoles();
    }

    /**
     * Adds a claimed chunk to this town.
     *
     * @param chunk claimed chunk entity
     */
    public void addChunk(final @NonNull RChunk chunk) {
        this.chunks.add(chunk);
    }

    /**
     * Used by repository caches as the unique business identifier.
     *
     * @return town UUID identifier
     */
    public UUID getIdentifier() {
        return this.uuid;
    }

    /**
     * Returns the display name of the town.
     *
     * @return town name
     */
    public String getTownName() {
        return this.townName;
    }

    /**
     * Returns the UUID of this town's mayor.
     *
     * @return mayor UUID
     */
    public UUID getMayor() {
        return this.mayor;
    }

    /**
     * Returns current town member entity records.
     *
     * @return members linked to this town
     */
    public Set<RDTPlayer> getMembers() {
        return this.members;
    }

    /**
     * Adds a member entity reference to this town.
     *
     * @param member member entity to include
     */
    public void addMember(final @NonNull RDTPlayer member) {
        this.members.add(member);
    }

    /**
     * Removes a member entity reference from this town.
     *
     * @param member member entity to remove
     */
    public void removeMember(final @NonNull RDTPlayer member) {
        this.members.remove(member);
    }

    /**
     * Returns the claimed chunk list.
     *
     * @return claimed chunks
     */
    public List<RChunk> getChunks() {
        return this.chunks;
    }

    /**
     * Returns all role definitions configured for this town.
     *
     * @return role definitions, including built-in Public, Member, Mayor, and Restricted roles
     */
    public List<TownRole> getRoles() {
        this.ensureDefaultRoles();
        return this.roles;
    }

    /**
     * Looks up a town role by role ID.
     *
     * @param roleId role identifier
     * @return matching role or {@code null} when no role exists for the ID
     */
    public @Nullable TownRole findRoleById(final @NonNull String roleId) {
        this.ensureDefaultRoles();
        final String normalizedRoleId = normalizeRoleId(roleId);

        for (final TownRole role : this.roles) {
            if (normalizeRoleId(role.getRoleId()).equals(normalizedRoleId)) {
                return role;
            }
        }

        return null;
    }

    /**
     * Creates and attaches a new custom role for this town.
     *
     * @param roleId role ID
     * @param roleName role display name
     * @param permissions role permissions
     * @return {@code true} when the role was created; {@code false} when invalid or duplicate
     */
    public boolean addRole(
            final @NonNull String roleId,
            final @NonNull String roleName,
            final @NonNull Set<String> permissions
    ) {
        final String normalizedRoleId = normalizeRoleId(roleId);
        final String normalizedRoleName = roleName.trim();

        if (normalizedRoleId.isBlank() || normalizedRoleName.isBlank()) {
            return false;
        }

        this.ensureDefaultRoles();
        if (this.findRoleById(normalizedRoleId) != null) {
            return false;
        }

        this.roles.add(new TownRole(this, normalizedRoleId, normalizedRoleName, permissions));
        return true;
    }

    /**
     * Removes a role by role ID.
     *
     * @param roleId role ID to remove
     * @return {@code true} when the role was removed, otherwise {@code false}
     */
    public boolean removeRoleById(final @NonNull String roleId) {
        final String normalizedRoleId = normalizeRoleId(roleId);

        if (isDefaultRoleId(normalizedRoleId)) {
            return false;
        }

        for (final RDTPlayer member : this.members) {
            final String memberRoleId = member.getTownRoleId();
            if (memberRoleId != null && normalizeRoleId(memberRoleId).equals(normalizedRoleId)) {
                return false;
            }
        }

        return this.roles.removeIf(role -> normalizeRoleId(role.getRoleId()).equals(normalizedRoleId));
    }

    /**
     * Returns whether the provided role ID is one of the built-in non-removable roles.
     *
     * @param roleId role ID to test
     * @return {@code true} when the role ID represents Public, Mayor, Member, or Restricted
     */
    public static boolean isDefaultRoleId(final @Nullable String roleId) {
        if (roleId == null) {
            return false;
        }

        final String normalizedRoleId = normalizeRoleId(roleId);
        return MAYOR_ROLE_ID.equals(normalizedRoleId)
                || MEMBER_ROLE_ID.equals(normalizedRoleId)
                || PUBLIC_ROLE_ID.equals(normalizedRoleId)
                || RESTRICTED_ROLE_ID.equals(normalizedRoleId);
    }

    /**
     * Normalizes role IDs for persistence and comparisons.
     *
     * @param roleId raw role ID
     * @return normalized upper-case role ID, or an empty string when blank
     */
    public static @NotNull String normalizeRoleId(final @NonNull String roleId) {
        return roleId.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Returns whether a town member has a specific town permission.
     *
     * @param member member entity record
     * @param permission permission to check
     * @return {@code true} when the member currently has the permission
     */
    public boolean hasTownPermission(
            final @Nullable RDTPlayer member,
            final @NonNull TownPermissions permission
    ) {
        if (member == null) {
            return false;
        }
        if (member.getTownUUID() == null || !member.getTownUUID().equals(this.uuid)) {
            return false;
        }
        if (member.hasTownPermission(permission)) {
            return true;
        }
        if (!member.getTownPermissions().isEmpty()) {
            return false;
        }

        final String roleId = member.getTownRoleId();
        if (roleId == null || roleId.isBlank()) {
            return false;
        }

        final TownRole role = this.findRoleById(roleId);
        return role != null && role.hasPermission(permission);
    }

    /**
     * Returns whether the provided role currently has a specific town permission.
     *
     * @param roleId role identifier
     * @param permission permission to check
     * @return {@code true} when the role has the permission
     */
    public boolean hasRolePermission(
            final @NonNull String roleId,
            final @NonNull TownPermissions permission
    ) {
        final TownRole role = this.findRoleById(roleId);
        return role != null && role.hasPermission(permission);
    }

    /**
     * Returns whether a player is currently invited to this town.
     *
     * @param playerUuid target player UUID
     * @return {@code true} when invited
     */
    public boolean isPlayerInvited(final @NonNull UUID playerUuid) {
        return this.invitedPlayers.contains(playerUuid.toString());
    }

    /**
     * Adds a player invite for this town.
     *
     * @param playerUuid target player UUID
     * @return {@code true} when the invite was newly added
     */
    public boolean invitePlayer(final @NonNull UUID playerUuid) {
        return this.invitedPlayers.add(playerUuid.toString());
    }

    /**
     * Removes a player invite for this town.
     *
     * @param playerUuid target player UUID
     * @return {@code true} when an invite existed and was removed
     */
    public boolean uninvitePlayer(final @NonNull UUID playerUuid) {
        return this.invitedPlayers.remove(playerUuid.toString());
    }

    /**
     * Returns whether a player currently has a pending join request.
     *
     * @param playerUuid target player UUID
     * @return {@code true} when a pending request exists
     */
    public boolean hasPendingJoinRequest(final @NonNull UUID playerUuid) {
        return this.pendingJoinRequests.contains(playerUuid.toString());
    }

    /**
     * Adds a pending join request for this town.
     *
     * @param playerUuid requesting player UUID
     * @return {@code true} when the request was newly added
     */
    public boolean addPendingJoinRequest(final @NonNull UUID playerUuid) {
        return this.pendingJoinRequests.add(playerUuid.toString());
    }

    /**
     * Removes a pending join request for this town.
     *
     * @param playerUuid requesting player UUID
     * @return {@code true} when a pending request existed and was removed
     */
    public boolean removePendingJoinRequest(final @NonNull UUID playerUuid) {
        return this.pendingJoinRequests.remove(playerUuid.toString());
    }

    /**
     * Returns all pending join request UUIDs as parsed values.
     *
     * @return set of pending player UUIDs
     */
    public @NotNull Set<UUID> getPendingJoinRequests() {
        final Set<UUID> result = new HashSet<>();
        for (final String playerUuid : this.pendingJoinRequests) {
            try {
                result.add(UUID.fromString(playerUuid));
            } catch (final IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    /**
     * Returns ledger entries recorded for this town.
     *
     * @return mutable ledger entry list
     */
    public @NotNull List<TownLedgerEntry> getLedgerEntries() {
        return this.ledgerEntries;
    }

    /**
     * Returns mutable town bank entries for multi-currency balances.
     *
     * @return bank entry list
     */
    public @NotNull List<RTownBank> getBankEntries() {
        return this.bankEntries;
    }

    /**
     * Returns the currently stored amount for a currency in this town bank.
     *
     * @param currencyId currency identifier
     * @return non-negative amount for the currency
     */
    public double getBankAmount(final @NonNull String currencyId) {
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (normalizedCurrencyId.isBlank()) {
            return 0.0D;
        }

        final RTownBank bankEntry = this.findBankEntry(normalizedCurrencyId);
        return bankEntry == null ? 0.0D : bankEntry.getAmount();
    }

    /**
     * Deposits a currency amount into this town bank.
     *
     * @param currencyId currency identifier
     * @param depositAmount amount to add
     * @return updated currency amount after deposit
     */
    public double depositBank(
            final @NonNull String currencyId,
            final double depositAmount
    ) {
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (normalizedCurrencyId.isBlank()) {
            return 0.0D;
        }
        if (depositAmount <= 0.0D) {
            return this.getBankAmount(normalizedCurrencyId);
        }

        RTownBank bankEntry = this.findBankEntry(normalizedCurrencyId);
        if (bankEntry == null) {
            bankEntry = new RTownBank(normalizedCurrencyId, 0.0D);
            this.bankEntries.add(bankEntry);
        }

        bankEntry.setAmount(bankEntry.getAmount() + depositAmount);
        return bankEntry.getAmount();
    }

    /**
     * Withdraws a currency amount from this town bank.
     *
     * @param currencyId currency identifier
     * @param withdrawAmount amount to remove
     * @return {@code true} when the withdrawal succeeded
     */
    public boolean withdrawBank(
            final @NonNull String currencyId,
            final double withdrawAmount
    ) {
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (normalizedCurrencyId.isBlank() || withdrawAmount <= 0.0D) {
            return false;
        }

        final RTownBank bankEntry = this.findBankEntry(normalizedCurrencyId);
        if (bankEntry == null || bankEntry.getAmount() + 0.0000001D < withdrawAmount) {
            return false;
        }

        final double updatedAmount = Math.max(0.0D, bankEntry.getAmount() - withdrawAmount);
        bankEntry.setAmount(updatedAmount);
        if (updatedAmount <= 0.0D) {
            this.bankEntries.remove(bankEntry);
        }

        return true;
    }

    /**
     * Returns the number of currencies currently tracked by this town bank.
     *
     * @return currency entry count
     */
    public int getBankCurrencyCount() {
        return this.bankEntries.size();
    }

    /**
     * Returns the current town level.
     *
     * @return current level, always at least {@code 1}
     */
    public int getTownLevel() {
        return Math.max(1, this.town_level);
    }

    /**
     * Updates the current town level.
     *
     * @param townLevel replacement level
     */
    public void setTownLevel(final int townLevel) {
        this.town_level = Math.max(1, townLevel);
    }

    /**
     * Returns a defensive copy of banked requirement item progress.
     *
     * @return copied progress map keyed by requirement token
     */
    public @NotNull Map<String, ItemStack> getLevelItemProgress() {
        final Map<String, ItemStack> itemProgress = new HashMap<>();
        for (final TownLevelRequirementProgress progress : this.levelRequirementProgress) {
            final ItemStack itemStack = progress.getItemStack();
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            itemProgress.put(progress.getProgressKey(), itemStack);
        }
        return itemProgress;
    }

    /**
     * Returns banked requirement item progress for a single key.
     *
     * @param progressKey stable requirement token
     * @return cloned stored item stack, or {@code null} when none exists
     */
    public @Nullable ItemStack getLevelItemProgress(final @NotNull String progressKey) {
        return this.findLevelRequirementProgress(progressKey)
                .map(TownLevelRequirementProgress::getItemStack)
                .orElse(null);
    }

    /**
     * Updates banked requirement item progress for a single key.
     *
     * @param progressKey stable requirement token
     * @param itemStack replacement stack, or {@code null} to clear progress
     */
    public void setLevelItemProgress(
            final @NotNull String progressKey,
            final @Nullable ItemStack itemStack
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        final TownLevelRequirementProgress progress = this.findLevelRequirementProgress(normalizedProgressKey)
                .orElseGet(() -> new TownLevelRequirementProgress(this, normalizedProgressKey));
        progress.setItemStack(itemStack);
        this.removeLevelProgressIfEmpty(progress);
    }

    /**
     * Returns banked requirement currency progress for a single key.
     *
     * @param progressKey stable requirement token
     * @return banked currency amount, or {@code 0.0} when none exists
     */
    public double getLevelCurrencyProgress(final @NotNull String progressKey) {
        return this.findLevelRequirementProgress(progressKey)
                .map(TownLevelRequirementProgress::getCurrencyAmount)
                .orElse(0.0D);
    }

    /**
     * Returns a defensive copy of banked requirement currency progress.
     *
     * @return copied progress map keyed by requirement token
     */
    public @NotNull Map<String, Double> getLevelCurrencyProgress() {
        final Map<String, Double> currencyProgress = new HashMap<>();
        for (final TownLevelRequirementProgress progress : this.levelRequirementProgress) {
            if (!progress.hasCurrencyProgress()) {
                continue;
            }
            currencyProgress.put(progress.getProgressKey(), progress.getCurrencyAmount());
        }
        return currencyProgress;
    }

    /**
     * Updates banked requirement currency progress for a single key.
     *
     * @param progressKey stable requirement token
     * @param amount replacement amount
     */
    public void setLevelCurrencyProgress(
            final @NotNull String progressKey,
            final double amount
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        final TownLevelRequirementProgress progress = this.findLevelRequirementProgress(normalizedProgressKey)
                .orElseGet(() -> new TownLevelRequirementProgress(this, normalizedProgressKey));
        progress.setCurrencyAmount(amount);
        this.removeLevelProgressIfEmpty(progress);
    }

    /**
     * Clears all banked requirement progress entries with a matching key prefix.
     *
     * @param progressKeyPrefix stable prefix for a level requirement set
     */
    public void clearLevelRequirementProgress(final @NotNull String progressKeyPrefix) {
        final String validatedPrefix = Objects.requireNonNull(
                progressKeyPrefix,
                "progressKeyPrefix cannot be null"
        );
        final List<TownLevelRequirementProgress> progressEntries = new ArrayList<>(this.levelRequirementProgress);
        for (final TownLevelRequirementProgress progress : progressEntries) {
            if (!progress.getProgressKey().startsWith(validatedPrefix)) {
                continue;
            }
            this.removeLevelRequirementProgress(progress);
        }
    }

    /**
     * Adds a level requirement progress entry to this town.
     *
     * @param progress progress entry to attach
     */
    void addLevelRequirementProgress(final @NotNull TownLevelRequirementProgress progress) {
        final TownLevelRequirementProgress validatedProgress = Objects.requireNonNull(progress, "progress cannot be null");
        final RTown currentOwner = validatedProgress.getTown();
        if (currentOwner == this && this.containsLevelRequirementProgress(validatedProgress)) {
            return;
        }
        if (currentOwner != null && currentOwner != this) {
            currentOwner.removeLevelRequirementProgress(validatedProgress);
        }
        if (!this.containsLevelRequirementProgress(validatedProgress)) {
            this.levelRequirementProgress.add(validatedProgress);
        }
        validatedProgress.setTownInternal(this);
    }

    /**
     * Removes a level requirement progress entry from this town.
     *
     * @param progress progress entry to remove
     */
    void removeLevelRequirementProgress(final @NotNull TownLevelRequirementProgress progress) {
        final TownLevelRequirementProgress validatedProgress = Objects.requireNonNull(progress, "progress cannot be null");
        if (this.levelRequirementProgress.removeIf(
                existingProgress -> this.matchesLevelRequirementProgress(existingProgress, validatedProgress)
        )) {
            validatedProgress.setTownInternal(null);
        }
    }

    /**
     * Records a join event in the town ledger.
     *
     * @param playerUuid player UUID
     * @param roleId assigned role ID
     */
    public void recordPlayerJoined(
            final @NonNull UUID playerUuid,
            final @NonNull String roleId
    ) {
        this.ledgerEntries.add(
                new TownLedgerEntry(
                        this,
                        playerUuid,
                        "JOINED",
                        "role=" + normalizeRoleId(roleId)
                )
        );
    }

    /**
     * Records a leave event in the town ledger.
     *
     * @param playerUuid player UUID
     */
    public void recordPlayerLeft(final @NonNull UUID playerUuid) {
        this.ledgerEntries.add(new TownLedgerEntry(this, playerUuid, "LEFT", null));
    }

    /**
     * Records a role-change event in the town ledger.
     *
     * @param playerUuid player UUID
     * @param oldRoleId previous role ID, nullable
     * @param newRoleId new role ID, nullable
     */
    public void recordPlayerRoleChanged(
            final @NonNull UUID playerUuid,
            final @Nullable String oldRoleId,
            final @Nullable String newRoleId
    ) {
        final String oldValue = oldRoleId == null ? "-" : normalizeRoleId(oldRoleId);
        final String newValue = newRoleId == null ? "-" : normalizeRoleId(newRoleId);
        this.ledgerEntries.add(
                new TownLedgerEntry(
                        this,
                        playerUuid,
                        "ROLE_CHANGED",
                        "old=" + oldValue + ",new=" + newValue
                )
        );
    }

    /**
     * Returns the configured nexus block location for this town.
     *
     * @return nexus location, or {@code null} when not yet placed
     */
    public @Nullable Location getNexusLocation() {
        return this.nexus_location;
    }

    /**
     * Legacy accessor retained for compatibility with reflective integrations.
     *
     * @return nexus location, or {@code null} when not yet placed
     * @deprecated use {@link #getNexusLocation()} instead
     */
    @Deprecated
    public @Nullable Location getNexus_location() {
        return this.nexus_location;
    }

    /**
     * Updates the nexus block location for this town.
     *
     * @param nexusLocation new nexus location, or {@code null} to clear
     */
    public void setNexusLocation(final @Nullable Location nexusLocation) {
        this.nexus_location = nexusLocation;
    }

    /**
     * Returns the configured town spawn location.
     *
     * @return town spawn location, or {@code null} when not yet set
     */
    public @Nullable Location getTownSpawnLocation() {
        return this.town_spawn_location;
    }

    /**
     * Updates the town spawn location.
     *
     * @param townSpawnLocation new town spawn location, or {@code null} to clear
     */
    public void setTownSpawnLocation(final @Nullable Location townSpawnLocation) {
        this.town_spawn_location = townSpawnLocation;
    }

    /**
     * Returns mutable town-wide protection role overrides.
     *
     * @return protection-key to role-ID map
     */
    public @NotNull Map<String, String> getProtectionRoleIds() {
        return this.protections;
    }

    /**
     * Returns the required role ID for a protection at town scope.
     *
     * @param protection protection being evaluated
     * @return normalized required role ID
     */
    public @NotNull String getProtectionRoleId(final @NonNull TownProtections protection) {
        final String configuredRoleId = this.protections.get(protection.getProtectionKey());
        if (configuredRoleId == null || configuredRoleId.isBlank()) {
            return protection.getDefaultRoleId();
        }
        return normalizeRoleId(configuredRoleId);
    }

    /**
     * Sets the required role ID for a town-wide protection.
     *
     * @param protection protection to update
     * @param roleId normalized or raw role ID
     */
    public void setProtectionRoleId(
            final @NonNull TownProtections protection,
            final @NonNull String roleId
    ) {
        final String normalizedRoleId = normalizeRoleId(roleId);
        if (normalizedRoleId.equals(normalizeRoleId(protection.getDefaultRoleId()))) {
            this.protections.remove(protection.getProtectionKey());
            return;
        }
        this.protections.put(protection.getProtectionKey(), normalizedRoleId);
    }

    /**
     * Returns whether this town currently has a placed nexus location.
     *
     * @return {@code true} when the nexus is placed
     */
    public boolean hasNexusPlaced() {
        return this.nexus_location != null;
    }

    /**
     * Returns whether the town is active.
     *
     * @return {@code true} when active
     */
    public boolean getActive() {
        return this.active;
    }

    /**
     * Updates whether this town is active.
     *
     * @param active true when this town should be treated as active
     */
    public void setActive(final boolean active) {
        this.active = active;
    }

    /**
     * Returns a debug summary of this town entity.
     *
     * @return formatted town summary string
     */
    @Override
    public String toString() {
        return "ID: " + this.uuid + '\n' +
                "Mayor: " + this.mayor + '\n' +
                "Name: " + this.townName + '\n' +
                "founded: " + this.founded + '\n' +
                "Chunks: " + this.chunks + '\n' +
                "Members: " + this.members.stream()
                        .map(member -> member.getIdentifier() + ":" + member.getTownRoleId())
                        .toList() + "\n" +
                "Roles: " + this.roles + "\n" +
                "Invites: " + this.invitedPlayers + "\n" +
                "Pending Join Requests: " + this.pendingJoinRequests + "\n" +
                "Protections: " + this.protections + "\n" +
                "Ledger Entries: " + this.ledgerEntries.size() + "\n" +
                "Bank Entries: " + this.bankEntries + "\n" +
                "Town Level: " + this.getTownLevel() + "\n" +
                "Level Requirement Progress: " + this.levelRequirementProgress.size() + "\n" +
                "Nexus Location: " + this.nexus_location + "\n" +
                "Town Spawn Location: " + this.town_spawn_location + "\n";
    }

    private @NotNull String normalizeCurrencyId(final @NonNull String currencyId) {
        return currencyId.trim().toLowerCase(Locale.ROOT);
    }

    private @Nullable RTownBank findBankEntry(final @NonNull String normalizedCurrencyId) {
        for (final RTownBank bankEntry : this.bankEntries) {
            if (normalizeCurrencyId(bankEntry.getCurrencyId()).equals(normalizedCurrencyId)) {
                return bankEntry;
            }
        }
        return null;
    }

    private boolean containsLevelRequirementProgress(final @NotNull TownLevelRequirementProgress candidate) {
        return this.levelRequirementProgress.stream()
                .anyMatch(existingProgress -> this.matchesLevelRequirementProgress(existingProgress, candidate));
    }

    private boolean matchesLevelRequirementProgress(
            final @NotNull TownLevelRequirementProgress left,
            final @NotNull TownLevelRequirementProgress right
    ) {
        if (left == right) {
            return true;
        }

        final Long leftId = left.getId();
        final Long rightId = right.getId();
        if (leftId != null && Objects.equals(leftId, rightId)) {
            return true;
        }

        return left.getProgressKey().equals(right.getProgressKey());
    }

    private void removeLevelProgressIfEmpty(final @NotNull TownLevelRequirementProgress progress) {
        if (progress.isEmpty()) {
            this.removeLevelRequirementProgress(progress);
        }
    }

    private @NotNull Optional<TownLevelRequirementProgress> findLevelRequirementProgress(
            final @NotNull String progressKey
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        return this.levelRequirementProgress.stream()
                .filter(progress -> progress.getProgressKey().equals(normalizedProgressKey))
                .findFirst();
    }

    private static @NotNull String normalizeProgressKey(final @NotNull String progressKey) {
        final String normalizedProgressKey = Objects.requireNonNull(progressKey, "progressKey cannot be null").trim();
        if (normalizedProgressKey.isEmpty()) {
            throw new IllegalArgumentException("progressKey cannot be blank");
        }
        return normalizedProgressKey;
    }

    private void ensureDefaultRoles() {
        this.addDefaultRoleIfMissing(
                PUBLIC_ROLE_ID,
                PUBLIC_ROLE_NAME,
                TownPermissions.defaultPermissionKeysForRole(PUBLIC_ROLE_ID)
        );
        this.addDefaultRoleIfMissing(
                MAYOR_ROLE_ID,
                MAYOR_ROLE_NAME,
                TownPermissions.defaultPermissionKeysForRole(MAYOR_ROLE_ID)
        );
        this.addDefaultRoleIfMissing(
                MEMBER_ROLE_ID,
                MEMBER_ROLE_NAME,
                TownPermissions.defaultPermissionKeysForRole(MEMBER_ROLE_ID)
        );
        this.addDefaultRoleIfMissing(
                RESTRICTED_ROLE_ID,
                RESTRICTED_ROLE_NAME,
                TownPermissions.defaultPermissionKeysForRole(RESTRICTED_ROLE_ID)
        );
        this.ensureDefaultRolePermissions(PUBLIC_ROLE_ID);
        this.ensureDefaultRolePermissions(MAYOR_ROLE_ID);
        this.ensureDefaultRolePermissions(MEMBER_ROLE_ID);
        this.ensureDefaultRolePermissions(RESTRICTED_ROLE_ID);
    }

    private void addDefaultRoleIfMissing(
            final @NonNull String roleId,
            final @NonNull String roleName,
            final @NonNull Set<String> defaultPermissions
    ) {
        for (final TownRole role : this.roles) {
            if (normalizeRoleId(role.getRoleId()).equals(normalizeRoleId(roleId))) {
                return;
            }
        }
        this.roles.add(new TownRole(this, roleId, roleName, defaultPermissions));
    }

    private void ensureDefaultRolePermissions(final @NonNull String roleId) {
        final Set<String> defaultPermissions = TownPermissions.defaultPermissionKeysForRole(roleId);
        for (final TownRole role : this.roles) {
            if (!normalizeRoleId(role.getRoleId()).equals(normalizeRoleId(roleId))) {
                continue;
            }
            if (defaultPermissions.isEmpty()) {
                if (!TownPermissions.canAssignToRole(roleId)) {
                    role.replacePermissions(Set.of());
                }
                return;
            }
            for (final String permissionKey : defaultPermissions) {
                role.addPermission(permissionKey);
            }
            return;
        }
    }

    /**
     * Persistent multi-currency balance entry owned by a town.
     *
     * @author ItsRainingHP
     * @since 1.0.9
     * @version 1.0.0
     */
    @Embeddable
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    public static class RTownBank {

        @Column(name = "currency_id", nullable = false)
        private String currencyId;

        @Column(name = "amount", nullable = false)
        private double amount;

        /**
         * Required by Hibernate.
         */
        protected RTownBank() {
        }

        /**
         * Creates a new town-bank currency entry.
         *
         * @param currencyId normalized or raw currency identifier
         * @param amount initial amount
         */
        public RTownBank(
                final @NonNull String currencyId,
                final double amount
        ) {
            this.currencyId = normalizeCurrencyIdStatic(currencyId);
            this.amount = Math.max(0.0D, amount);
        }

        /**
         * Returns the normalized currency identifier for this entry.
         *
         * @return currency identifier
         */
        public @NotNull String getCurrencyId() {
            return this.currencyId;
        }

        /**
         * Returns the current non-negative amount for this entry.
         *
         * @return currency amount
         */
        public double getAmount() {
            return this.amount;
        }

        /**
         * Updates the current amount for this entry.
         *
         * @param amount replacement amount
         */
        public void setAmount(final double amount) {
            this.amount = Math.max(0.0D, amount);
        }

        /**
         * Returns a debug representation of this bank entry.
         *
         * @return formatted bank-entry string
         */
        @Override
        public @NotNull String toString() {
            return "RTownBank{" +
                    "currencyId='" + this.currencyId + '\'' +
                    ", amount=" + this.amount +
                    '}';
        }

        private static @NotNull String normalizeCurrencyIdStatic(final @NonNull String currencyId) {
            return Objects.requireNonNull(currencyId, "currencyId").trim().toLowerCase(Locale.ROOT);
        }
    }
}
