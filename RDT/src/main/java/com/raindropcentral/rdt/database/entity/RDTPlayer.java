package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Persistent representation of a player participating in Raindrop Towns.
 * <p>
 * Stores the player's UUID and, when applicable, the UUID of the town they belong to.
 * The {@code townJoinDate} is updated when the town membership changes via {@link #setTownUUID(UUID)}.
 */
@Entity
@Table(name = "rdt_players")
@SuppressWarnings({
        "DefaultAnnotationParam",
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection",
        "LombokGetterMayBeUsed"
})
public class RDTPlayer extends BaseEntity {

    /** Player's unique UUID (public identifier and cache key). */
    @Column(name = "player_uuid", unique = true, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID player_uuid;

    /** UUID of the town this player is currently a member of; {@code null} when not in a town. */
    @Column(name = "town_uuid", unique = false, nullable = true)
    @Convert(converter = UUIDConverter.class)
    private UUID town_uuid;

    /** Timestamp (epoch millis) when the player last joined their current town. */
    @Column(name = "join_date", unique = false, nullable = true)
    private long townJoinDate;

    @Column(name = "contributions", unique = false, nullable = true)
    private double contributions;

    @Column(name = "withdrew", unique = false, nullable = true)
    private double withdrew;

    /**
     * Construct a player record with an initial town membership.
     */
    public RDTPlayer(UUID player_uuid, UUID town_uuid) {
        this.player_uuid = player_uuid;
        this.town_uuid = town_uuid;
    }

    /**
     * Construct a player record with no town membership.
     */
    public RDTPlayer(UUID player_uuid) {
        this.player_uuid = player_uuid;
        this.town_uuid = null;
    }

    /** Required by Hibernate. */
    protected RDTPlayer() {}

    /**
     * Unique identifier used by repositories and caches.
     */
    public UUID getIdentifier() {
        return this.player_uuid;
    }

    /**
     * Current town UUID or {@code null} if the player is unaffiliated.
     */
    public UUID getTownUUID() {
        return this.town_uuid;
    }

    /**
     * Set the player's town membership and records the current time as townJoinDate.
     *
     * @param town_uuid new town UUID, or {@code null} to clear membership
     */
    public void setTownUUID(UUID town_uuid) {
        this.town_uuid = town_uuid;
        this.townJoinDate = System.currentTimeMillis();
    }

    public double getContributions() {
        return this.contributions;
    }

    public void addContributions(double amount) {
        this.contributions += amount;
    }

    public double getWithdrew() {
        return this.withdrew += this.contributions;
    }

    public void addWithdrew(double amount) {
        this.withdrew += amount;
    }
}