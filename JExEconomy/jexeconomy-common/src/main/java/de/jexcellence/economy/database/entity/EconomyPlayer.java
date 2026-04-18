package de.jexcellence.economy.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A player known to the economy system.
 *
 * @author JExcellence
 * @since 3.0.0
 */
@Entity
@Table(name = "economy_player")
public class EconomyPlayer extends LongIdEntity {

    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;

    @Column(name = "player_name", nullable = false, length = 16)
    private String playerName;

    /** JPA constructor. */
    protected EconomyPlayer() {
    }

    /**
     * Creates an economy player record.
     *
     * @param uniqueId   the player's UUID
     * @param playerName the player's current name
     */
    public EconomyPlayer(@NotNull UUID uniqueId, @NotNull String playerName) {
        this.uniqueId = uniqueId;
        this.playerName = playerName;
    }

    /**
     * Returns the player's UUID.
     *
     * @return the UUID
     */
    public @NotNull UUID getUniqueId() {
        return uniqueId;
    }

    /**
     * Sets the player's UUID.
     *
     * @param uniqueId the UUID
     */
    public void setUniqueId(@NotNull UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Returns the player's current name.
     *
     * @return the player name
     */
    public @NotNull String getPlayerName() {
        return playerName;
    }

    /**
     * Sets the player's name.
     *
     * @param playerName the player name
     */
    public void setPlayerName(@NotNull String playerName) {
        this.playerName = playerName;
    }

    @Override
    public String toString() {
        return "EconomyPlayer[" + playerName + "/" + uniqueId + "]";
    }
}
