package com.raindropcentral.rds.database.entity;

import com.raindropcentral.rplatform.database.converter.ItemStackListConverter;
import com.raindropcentral.rplatform.database.converter.LocationConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
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

    @Column(name = "shop_location", unique = false, nullable = true)
    @Convert(converter = LocationConverter.class)
    private Location shop_location;

    @Column(name = "bank", unique = false, nullable = false)
    private double bank;

    @Column(name = "income", unique = false, nullable = false)
    private double income;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "shop_id")
    @Convert(converter = ItemStackListConverter.class)
    private List<ItemStack> items;

    public Shop() {}

    public Shop(UUID owner_uuid, Location shop_location) {
        this.owner_uuid = owner_uuid;
        this.shop_location = shop_location;
        this.bank = 0;
        this.income = 0;
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

}
