package com.raindropcentral.rds.database.entity;

import com.raindropcentral.rplatform.database.converter.LocationConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.bukkit.Location;

import java.util.UUID;

@Entity
@Table(name = "shops")
@SuppressWarnings({
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
public class Shop extends BaseEntity {

    @Column(name = "owner_uuid", unique = false, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID owner_uuid;

    @Column(name = "shop_location", unique = false, nullable = false)
    @Convert(converter = LocationConverter.class)
    private Location shop_location;

    public Shop(UUID owner_uuid) {
        this.owner_uuid = owner_uuid;
    }

    public Shop() {}

    public UUID getOwner() {
        return this.owner_uuid;
    }

    public Location getShopByLocation() {
        return this.shop_location;
    }

}
