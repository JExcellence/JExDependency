package com.raindropcentral.rds.database.entity;

import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.json.ItemParser;
import com.raindropcentral.rplatform.database.converter.LocationConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.bukkit.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

    @Column(name = "bank", unique = false, nullable = false)
    private double bank;

    @Column(name = "income", unique = false, nullable = false)
    private double income;

    @Column(name = "shop_items", unique = false, nullable = false, columnDefinition = "LONGTEXT")
    private String itemsJson = "[]";

    @Transient
    private List<AbstractItem> cachedItems = new ArrayList<>();

    public Shop() {
    }

    public Shop(UUID owner_uuid, Location shop_location) {
        this.owner_uuid = owner_uuid;
        this.shop_location = shop_location;
        this.bank = 0;
        this.income = 0;
        setItems(List.of());
    }

    public UUID getOwner() {
        return this.owner_uuid;
    }

    public Location getShopLocation() {
        return this.shop_location;
    }

    public double getBank() {
        return this.bank;
    }

    public double getIncome() {
        return this.income;
    }

    public double addBank(double bank) {
        this.bank += bank;
        return this.bank;
    }

    public double addIncome(double income) {
        this.income += income;
        return this.income;
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

    public int getStoredItemCount() {
        return getItems().size();
    }

    public boolean isOwner(final UUID playerId) {
        return Objects.equals(this.owner_uuid, playerId);
    }
}
