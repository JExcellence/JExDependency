package de.jexcellence.core.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A persisted inventory snapshot scoped to one {@link CorePlayer} / {@link CentralServer}
 * pair. Contents are stored as opaque strings — serialization is handled
 * by the service layer rather than the entity itself.
 */
@Entity
@Table(name = "jexcore_player_inventory")
public class PlayerInventory extends LongIdEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private CorePlayer player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "server_id", nullable = false)
    private CentralServer server;

    @Column(name = "inventory", columnDefinition = "LONGTEXT")
    private String inventory;

    @Column(name = "armor_contents", columnDefinition = "LONGTEXT")
    private String armor;

    @Column(name = "enderchest", columnDefinition = "LONGTEXT")
    private String enderchest;

    protected PlayerInventory() {
    }

    public PlayerInventory(@NotNull CorePlayer player, @NotNull CentralServer server) {
        this.player = player;
        this.server = server;
    }

    public @NotNull CorePlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(@NotNull CorePlayer player) {
        this.player = player;
    }

    public @NotNull CentralServer getServer() {
        return this.server;
    }

    public void setServer(@NotNull CentralServer server) {
        this.server = server;
    }

    public @Nullable String getInventory() {
        return this.inventory;
    }

    public void setInventory(@Nullable String inventory) {
        this.inventory = inventory;
    }

    public @Nullable String getArmor() {
        return this.armor;
    }

    public void setArmor(@Nullable String armor) {
        this.armor = armor;
    }

    public @Nullable String getEnderchest() {
        return this.enderchest;
    }

    public void setEnderchest(@Nullable String enderchest) {
        this.enderchest = enderchest;
    }

    @Override
    public String toString() {
        return "PlayerInventory[player=" + (this.player != null ? this.player.getPlayerName() : "null")
                + ", server=" + (this.server != null ? this.server.getServerUuid() : "null") + "]";
    }
}
