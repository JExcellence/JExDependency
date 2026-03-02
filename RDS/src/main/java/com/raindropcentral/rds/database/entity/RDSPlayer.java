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

    public RDSPlayer(UUID player_uuid) {
        this.player_uuid = player_uuid;
        this.shops = 0;
    }

    public RDSPlayer() {}

    public UUID getIdentifier() {
        return this.player_uuid;
    }

    public int getShops() {
        return this.shops;
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
