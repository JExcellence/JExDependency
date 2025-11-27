package com.raindropcentral.rdq.database.entity.player;

import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a player in the RDQ system.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Entity
@Table(name = "rdq_players")
public class RDQPlayer {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 16)
    private String name;

    @Column(name = "first_join", nullable = false, updatable = false)
    private Instant firstJoin;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    protected RDQPlayer() {
    }

    public RDQPlayer(@NotNull UUID id, @NotNull String name, @NotNull Instant firstJoin, @NotNull Instant lastSeen) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.firstJoin = Objects.requireNonNull(firstJoin, "firstJoin");
        this.lastSeen = Objects.requireNonNull(lastSeen, "lastSeen");
    }

    @NotNull
    public static RDQPlayer create(@NotNull UUID id, @NotNull String name) {
        var now = Instant.now();
        return new RDQPlayer(id, name, now, now);
    }

    @NotNull
    public UUID id() {
        return id;
    }

    @NotNull
    public String name() {
        return name;
    }

    @NotNull
    public Instant firstJoin() {
        return firstJoin;
    }

    @NotNull
    public Instant lastSeen() {
        return lastSeen;
    }

    public void updateName(@NotNull String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public void updateLastSeen() {
        this.lastSeen = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RDQPlayer that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RDQPlayer[id=" + id + ", name=" + name + "]";
    }
}
