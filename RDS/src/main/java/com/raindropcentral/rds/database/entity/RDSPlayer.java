package com.raindropcentral.rds.database.entity;

import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "rds_players")
@SuppressWarnings({
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
public class RDSPlayer extends BaseEntity {

    @Column(name = "player_uuid", unique = true, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID player_uuid;

    @Column(name = "shops", unique = false, nullable = false)
    private int shops;

    @Column(name = "shop_bar_enabled", unique = false, nullable = false)
    private boolean shop_bar_enabled;

    public RDSPlayer(UUID player_uuid) {
        this.player_uuid = player_uuid;
        this.shops = 0;
        this.shop_bar_enabled = false;
    }

    public RDSPlayer() {}

    public UUID getIdentifier() {
        return this.player_uuid;
    }

    public int getShops() {
        return this.shops;
    }

    public boolean isShopBarEnabled() {
        return this.shop_bar_enabled;
    }

    public boolean toggleShopBar() {
        this.shop_bar_enabled = !this.shop_bar_enabled;
        return this.shop_bar_enabled;
    }

    public void setShopBarEnabled(final boolean enabled) {
        this.shop_bar_enabled = enabled;
    }

    public void addShop(int amount) {
        if (amount <= 0) {
            return;
        }

        this.shops += amount;
    }

    public void removeShop(int amount) {
        if (amount <= 0) {
            return;
        }

        this.shops = Math.max(0, this.shops - amount);
    }
}
