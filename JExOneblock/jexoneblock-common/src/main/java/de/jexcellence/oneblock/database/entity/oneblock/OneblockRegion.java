package de.jexcellence.oneblock.database.entity.oneblock;

import com.raindropcentral.rplatform.database.converter.LocationConverter;
import com.raindropcentral.rplatform.database.converter.WorldConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

@Entity
@Table(name = "oneblock_region")
@Getter
@Setter
public class OneblockRegion extends BaseEntity {

    @Column(name = "x1", nullable = false)
    private int x1;

    @Column(name = "y1", nullable = false)
    private int y1;

    @Column(name = "z1", nullable = false)
    private int z1;

    @Column(name = "x2", nullable = false)
    private int x2;

    @Column(name = "y2", nullable = false)
    private int y2;

    @Column(name = "z2", nullable = false)
    private int z2;

    @Column(name = "current_world", nullable = false)
    @Convert(converter = WorldConverter.class)
    private World currentWorld;

    @Column(name = "spawn_location", nullable = false)
    @Convert(converter = LocationConverter.class)
    private Location spawnLocation;

    @Column(name = "visitor_spawn_location", nullable = false)
    @Convert(converter = LocationConverter.class)
    private Location visitorSpawnLocation;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "island_id")
    private OneblockIsland island;

    protected OneblockRegion() {}

    public OneblockRegion(@NotNull Location corner1, @NotNull Location corner2,
                         @NotNull Location spawnLocation, @NotNull Location visitorSpawnLocation) {
        this.x1 = Math.min(corner1.getBlockX(), corner2.getBlockX());
        this.y1 = Math.min(corner1.getBlockY(), corner2.getBlockY());
        this.z1 = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        this.x2 = Math.max(corner1.getBlockX(), corner2.getBlockX());
        this.y2 = Math.max(corner1.getBlockY(), corner2.getBlockY());
        this.z2 = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        this.currentWorld = corner1.getWorld();
        this.spawnLocation = spawnLocation;
        this.visitorSpawnLocation = visitorSpawnLocation;
    }

    public boolean contains(@NotNull Location location) {
        if (!location.getWorld().equals(this.currentWorld)) {
            return false;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= this.x1 && x <= this.x2 &&
               y >= this.y1 && y <= this.y2 &&
               z >= this.z1 && z <= this.z2;
    }

    public Location getMinLocation() {
        return new Location(this.currentWorld, this.x1, this.y1, this.z1);
    }

    public Location getMaxLocation() {
        return new Location(this.currentWorld, this.x2, this.y2, this.z2);
    }

    public Location getCenterLocation() {
        double centerX = (this.x1 + this.x2) / 2.0;
        double centerY = (this.y1 + this.y2) / 2.0;
        double centerZ = (this.z1 + this.z2) / 2.0;
        return new Location(this.currentWorld, centerX, centerY, centerZ);
    }

    public int getVolume() {
        return (this.x2 - this.x1 + 1) * (this.y2 - this.y1 + 1) * (this.z2 - this.z1 + 1);
    }

    public void expand(int size) {
        this.x1 -= size;
        this.z1 -= size;
        this.x2 += size;
        this.z2 += size;
    }
}