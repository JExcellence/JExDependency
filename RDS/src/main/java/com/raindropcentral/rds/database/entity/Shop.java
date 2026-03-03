package com.raindropcentral.rds.database.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.json.ItemParser;
import com.raindropcentral.rplatform.database.converter.LocationConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.bukkit.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persists a single logical shop and its chest anchor locations.
 *
 * <p>A shop may occupy one chest block or two adjacent chest blocks when upgraded to a
 * double chest. Both blocks still represent the same logical shop entity.</p>
 */
@Entity
@Table(name = "shops")
@SuppressWarnings({
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
public class Shop extends BaseEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger("RDS");

    @Column(name = "owner_uuid", unique = false, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID owner_uuid;

    @Column(name = "shop_location", unique = false, nullable = true)
    @Convert(converter = LocationConverter.class)
    private Location shop_location;

    @Column(name = "secondary_shop_location", unique = false, nullable = true)
    @Convert(converter = LocationConverter.class)
    private Location secondary_shop_location;

    @OneToMany(
            mappedBy = "shop",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<Bank> bankEntries = new ArrayList<>();

    @Column(name = "shop_items", unique = false, nullable = false, columnDefinition = "LONGTEXT")
    private String itemsJson = "[]";

    @Column(name = "admin_shop", unique = false, nullable = false)
    private boolean admin_shop = false;

    @Column(name = "trusted_players", unique = false, nullable = false, columnDefinition = "LONGTEXT")
    private String trustedPlayersJson = "{}";

    @OneToMany(
            mappedBy = "shop",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("createdAt DESC, id DESC")
    private List<ShopLedgerEntry> ledgerEntries = new ArrayList<>();

    @Transient
    private List<AbstractItem> cachedItems = new ArrayList<>();

    @Transient
    private Map<UUID, ShopTrustStatus> cachedTrustedPlayers;

    public Shop() {
    }

    public Shop(UUID owner_uuid, Location shop_location) {
        this.owner_uuid = owner_uuid;
        this.shop_location = shop_location;
        this.secondary_shop_location = null;
        this.bankEntries = new ArrayList<>();
        this.ledgerEntries = new ArrayList<>();
        setItems(List.of());
    }

    public UUID getOwner() {
        return this.owner_uuid;
    }

    public Location getShopLocation() {
        return this.shop_location;
    }

    /**
     * Returns the optional second chest location used when this shop is a double chest.
     *
     * @return the second chest block location, or {@code null} when this shop is still single-wide
     */
    public @Nullable Location getSecondaryShopLocation() {
        return this.secondary_shop_location;
    }

    /**
     * Updates the optional second chest location for this shop.
     *
     * @param secondaryShopLocation the second chest block location, or {@code null} to clear it
     */
    public void setSecondaryShopLocation(
            final @Nullable Location secondaryShopLocation
    ) {
        this.secondary_shop_location = secondaryShopLocation;
    }

    /**
     * Indicates whether this shop currently spans two chest blocks.
     *
     * @return {@code true} when a secondary chest location is present
     */
    public boolean isDoubleChest() {
        return this.secondary_shop_location != null;
    }

    /**
     * Checks whether the provided block location belongs to this shop.
     *
     * @param location block location to test
     * @return {@code true} when the location matches either chest half of this shop
     */
    public boolean occupiesLocation(
            final @Nullable Location location
    ) {
        return Objects.equals(this.shop_location, location)
                || Objects.equals(this.secondary_shop_location, location);
    }

    /**
     * Returns the number of placed shop blocks represented by this shop.
     *
     * @return {@code 2} for a double chest shop, otherwise {@code 1}
     */
    public int getShopBlockCount() {
        return this.isDoubleChest() ? 2 : 1;
    }

    public double getBank() {
        return this.getBankAmount("vault");
    }

    public double addBank(double bank) {
        return this.addBank("vault", bank);
    }
    
    public @NotNull List<Bank> getBankEntries() {
        return List.copyOf(this.bankEntries);
    }

    public int getBankCurrencyCount() {
        return this.bankEntries.size();
    }

    public double getBankAmount(
            final @NotNull String currencyType
    ) {
        final Bank bankEntry = this.findBankEntry(currencyType);
        return bankEntry == null ? 0D : bankEntry.getAmount();
    }

    public double addBank(
            final @NotNull String currencyType,
            final double amount
    ) {
        if (amount <= 0D) {
            return this.getBankAmount(currencyType);
        }

        Bank bankEntry = this.findBankEntry(currencyType);
        if (bankEntry == null) {
            bankEntry = new Bank(this, currencyType, 0D);
            this.bankEntries.add(bankEntry);
        }
        return bankEntry.deposit(amount);
    }

    public boolean withdrawBank(
            final @NotNull String currencyType,
            final double amount
    ) {
        if (amount <= 0D) {
            return true;
        }

        final Bank bankEntry = this.findBankEntry(currencyType);
        if (bankEntry == null || bankEntry.getAmount() + 1.0E-6D < amount) {
            return false;
        }

        bankEntry.withdraw(amount);
        if (bankEntry.getAmount() <= 1.0E-6D) {
            this.bankEntries.remove(bankEntry);
        }

        return true;
    }

    public List<AbstractItem> getItems() {
        if (this.cachedItems == null) {
            this.cachedItems = new ArrayList<>();
        }

        if (
                this.cachedItems.isEmpty() &&
                this.itemsJson != null &&
                !this.itemsJson.isBlank() &&
                !"[]".equals(this.itemsJson.trim())
        ) {
            try {
                this.cachedItems = ItemParser.parseList(this.itemsJson);
            } catch (Exception e) {
                LOGGER.error("Failed to parse shop items JSON", e);
                throw new RuntimeException("Failed to parse shop items", e);
            }
        }

        return new ArrayList<>(this.cachedItems);
    }

    public void setItems(final List<? extends AbstractItem> items) {
        final List<AbstractItem> safeItems = new ArrayList<>();
        if (items != null) {
            for (AbstractItem item : items) {
                if (item != null) {
                    safeItems.add(item);
                }
            }
        }

        this.cachedItems = safeItems;

        try {
            this.itemsJson = ItemParser.serializeList(safeItems);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize shop items", e);
            throw new RuntimeException("Failed to serialize shop items", e);
        }
    }

    public boolean isAdminShop() {
        return this.admin_shop;
    }

    public void setAdminShop(final boolean adminShop) {
        this.admin_shop = adminShop;
    }

    public int getStoredItemCount() {
        return getItems().size();
    }

    public @NotNull Map<UUID, ShopTrustStatus> getTrustedPlayers() {
        if (this.cachedTrustedPlayers == null) {
            this.cachedTrustedPlayers = this.parseTrustedPlayers();
        }

        return new HashMap<>(this.cachedTrustedPlayers);
    }

    public @NotNull List<ShopLedgerEntry> getLedgerEntries() {
        if (this.ledgerEntries == null) {
            this.ledgerEntries = new ArrayList<>();
        }

        return List.copyOf(this.ledgerEntries);
    }

    public void addLedgerEntry(
            final @NotNull ShopLedgerEntry ledgerEntry
    ) {
        if (this.ledgerEntries == null) {
            this.ledgerEntries = new ArrayList<>();
        }

        ledgerEntry.setShop(this);
        this.ledgerEntries.add(0, ledgerEntry);
    }

    public int getLedgerEntryCount() {
        return this.ledgerEntries == null ? 0 : this.ledgerEntries.size();
    }

    public int getLedgerEntryCount(
            final @NotNull ShopLedgerType ledgerType
    ) {
        if (this.ledgerEntries == null || this.ledgerEntries.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (final ShopLedgerEntry ledgerEntry : this.ledgerEntries) {
            if (ledgerEntry != null && ledgerEntry.getEntryType() == ledgerType) {
                count++;
            }
        }

        return count;
    }

    public void setTrustedPlayers(
            final @Nullable Map<UUID, ShopTrustStatus> trustedPlayers
    ) {
        final Map<UUID, ShopTrustStatus> safeTrustedPlayers = new HashMap<>();
        if (trustedPlayers != null) {
            for (final Map.Entry<UUID, ShopTrustStatus> entry : trustedPlayers.entrySet()) {
                final UUID playerId = entry.getKey();
                final ShopTrustStatus status = entry.getValue();
                if (playerId == null || status == null || status == ShopTrustStatus.PUBLIC || this.isOwner(playerId)) {
                    continue;
                }

                safeTrustedPlayers.put(playerId, status);
            }
        }

        this.cachedTrustedPlayers = safeTrustedPlayers;

        try {
            this.trustedPlayersJson = ItemParser.getObjectMapper().writeValueAsString(safeTrustedPlayers);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize trusted players", e);
            throw new RuntimeException("Failed to serialize trusted players", e);
        }
    }

    public @NotNull ShopTrustStatus getTrustStatus(
            final @NotNull UUID playerId
    ) {
        if (this.isOwner(playerId)) {
            return ShopTrustStatus.TRUSTED;
        }

        return this.getTrustedPlayers().getOrDefault(playerId, ShopTrustStatus.PUBLIC);
    }

    public void setTrustStatus(
            final @NotNull UUID playerId,
            final @NotNull ShopTrustStatus status
    ) {
        if (this.isOwner(playerId)) {
            return;
        }

        final Map<UUID, ShopTrustStatus> trustedPlayers = this.getTrustedPlayers();
        if (status == ShopTrustStatus.PUBLIC) {
            trustedPlayers.remove(playerId);
        } else {
            trustedPlayers.put(playerId, status);
        }

        this.setTrustedPlayers(trustedPlayers);
    }

    public int getTrustedPlayerCount(
            final @NotNull ShopTrustStatus status
    ) {
        int count = 0;
        for (final ShopTrustStatus trustedStatus : this.getTrustedPlayers().values()) {
            if (trustedStatus == status) {
                count++;
            }
        }

        return count;
    }

    public boolean canAccessOverview(
            final @NotNull UUID playerId
    ) {
        return this.isOwner(playerId) || this.getTrustStatus(playerId) != ShopTrustStatus.PUBLIC;
    }

    public boolean canSupply(
            final @NotNull UUID playerId
    ) {
        return this.isOwner(playerId) || this.getTrustStatus(playerId).hasSupplyAccess();
    }

    public boolean canManage(
            final @NotNull UUID playerId
    ) {
        return this.isOwner(playerId) || this.getTrustStatus(playerId).hasFullAccess();
    }

    public boolean isOwner(final UUID playerId) {
        return Objects.equals(this.owner_uuid, playerId);
    }

    private @NotNull Map<UUID, ShopTrustStatus> parseTrustedPlayers() {
        if (this.trustedPlayersJson == null || this.trustedPlayersJson.isBlank()) {
            return new HashMap<>();
        }

        try {
            final Map<UUID, ShopTrustStatus> parsed = ItemParser.getObjectMapper().readValue(
                    this.trustedPlayersJson,
                    new TypeReference<Map<UUID, ShopTrustStatus>>() {
                    }
            );
            return parsed == null ? new HashMap<>() : new HashMap<>(parsed);
        } catch (Exception e) {
            LOGGER.error("Failed to parse trusted players JSON", e);
            throw new RuntimeException("Failed to parse trusted players", e);
        }
    }

    private @Nullable Bank findBankEntry(
            final @NotNull String currencyType
    ) {
        if (this.bankEntries == null) {
            this.bankEntries = new ArrayList<>();
        }

        for (final Bank bankEntry : this.bankEntries) {
            if (bankEntry != null && bankEntry.matchesCurrencyType(currencyType)) {
                return bankEntry;
            }
        }

        return null;
    }
}
