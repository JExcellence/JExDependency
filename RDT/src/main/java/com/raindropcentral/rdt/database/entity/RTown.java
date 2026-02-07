package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rplatform.database.converter.LocationConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.Location;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * Represents a player-created town in the server.
 * <p>
 * A town has a stable public UUID, a mayor (creator) UUID, a unique human-friendly
 * name, a spawn location (x/y/z), members, financial data, and a collection of
 * claimed {@link RChunk chunks}. This entity is the root aggregate for most town
 * operations in the plugin.
 */
@Entity
@Table(name = "towns")
@SuppressWarnings({
        "LombokGetterMayBeUsed",
        "StringTemplateMigration",
        "DefaultAnnotationParam",
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
public class RTown extends BaseEntity {

    // Unique town UUID (primary public identifier)
    @Column(name = "uuid", unique = true, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID uuid;

    // UUID of the mayor/player who founded the town
    @Column(name = "mayor", unique = true, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID mayor;

    /** All chunks claimed by this town. */
    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            mappedBy = "town"
    )
    private final List<RChunk> chunks = new ArrayList<>();

    // Human-friendly town name
    @Column(name = "town_name", unique = true, nullable = false)
    private String townName;

    @Column(name = "bank", unique = false, nullable = false)
    private double bank;

    @Column(name = "x_spawn", unique = false, nullable = false)
    private double x_spawn;

    @Column(name = "y_spawn", unique = false, nullable = false)
    private double y_spawn;

    @Column(name = "z_spawn", unique = false, nullable = false)
    private double z_spawn;

    @ElementCollection(fetch = FetchType.EAGER)
    Set<String> members;

    @Column(name = "founded", unique = false, nullable = false)
    private long founded;

    @Column(name = "con_spent", unique = false, nullable = false)
    private double con_spent;

    @Column(name = "upkeep", unique = false, nullable = false)
    private double upkeep;

    @Column(name = "nexus_location", unique = false, nullable = false)
    @Convert(converter = LocationConverter.class)
    private Location nexus_location;

    @Column(name = "last_taxed", unique = false, nullable = true)
    private long last_taxed;

    // Required by Hibernate
    protected RTown() {}

    /**
     * Create a new town record.
     *
     * @param uuid           public town UUID
     * @param mayor          creator/mayor UUID (also added to members)
     * @param townName       unique display name
     * @param x_spawn        spawn X
     * @param y_spawn        spawn Y
     * @param z_spawn        spawn Z
     * @param initial_funds  construction cost recorded as "con_spent"
     */
    public RTown(UUID uuid, UUID mayor, String townName,
                 double x_spawn, double y_spawn, double z_spawn, double initial_funds, Location nexus_location) {
        this.uuid = uuid;
        this.mayor = mayor;

        this.townName = townName;

        this.bank = 0;
        this.x_spawn = x_spawn;
        this.y_spawn = y_spawn;
        this.z_spawn = z_spawn;
        this.members = new HashSet<>();
        this.members.add(mayor.toString());
        this.founded = System.currentTimeMillis();
        this.con_spent = initial_funds;
        this.upkeep = 0;
        this.nexus_location = nexus_location;
        this.last_taxed = 0;
    }

    /**
     * Add a claimed chunk to this town.
     */
    public void addChunk(@NonNull RChunk chunk) {
        this.chunks.add(chunk);
    }

    // Used by RepositoryManager as the unique identifier
    public UUID getIdentifier() {
        return this.uuid;
    }

    public String getTownName() {
        return this.townName;
    }

    // Basic getters
    public UUID getMayor() {
        return this.mayor;
    }

    public double getCon_spent() {
        return this.con_spent;
    }

    /** Increase construction cost spent by this town by the given amount. */
    public void addCon_spent(double amount) { this.con_spent += amount;}

    public void subCon_spent(double amount) { this.con_spent -= amount;}

    public double getUpkeep() {
        return this.upkeep;
    }

    public void deposit(double amount) {
        this.bank += amount;
    }

    public void withdraw(double amount) {
        this.bank -= amount;
    }

    public double getBank() {
        return this.bank;
    }

    public Set<String> getMembers() {
        return this.members;
    }

    public List<RChunk> getChunks() {
        return this.chunks;
    }

    public Location getNexus_location() {return this.nexus_location;}

    public long getLast_taxed() {return this.last_taxed;}

    public void setLast_Taxed(long current_time) {
        this.last_taxed = current_time;
    }

    @Override
    public String toString() {
        return "ID: " + this.uuid + '\n' +
                "Mayor: " + this.mayor + '\n' +
                "Name: " + this.townName + '\n' +
                "founded: " + this.founded + '\n' +
                "Con Cost: " + this.con_spent + '\n' +
                "Upkeep: " + this.upkeep + '\n' +
                "Bank: " + this.bank + '\n' +
                "Spawn: " + this.x_spawn + ", " +
                this.y_spawn + ", " +
                this.z_spawn + '\n' +
                "Chunks: " + this.chunks + '\n' +
                "Members: " + this.members + "\n" +
                "Nexus Location: " + this.nexus_location + "\n" +
                "Last Taxed: " + this.last_taxed
                ;
    }


}