package de.jexcellence.economy.database.entity;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a user (player) in the currency system.
 * <p>
 * Each user is uniquely identified by their UUID and stores their current player name.
 * This entity is mapped to the {@code p_player} table in the database.
 * </p>
 *
 * <p>
 * Example usages include tracking player balances, associating currencies with players,
 * and managing player-related data in the system.
 * </p>
 *
 * @author JExcellence
 */
@Table(name = "p_player")
@Entity
public class User extends BaseEntity {
	
	/**
	 * The unique identifier (UUID) of the player.
	 */
	@Column(
		name = "unique_id",
		unique = true,
		nullable = false
	)
	private UUID uniqueId;
	
	/**
	 * The current name of the player.
	 */
	@Column(
		name = "player_name",
		nullable = false
	)
	private String playerName;
	
	/**
	 * Protected no-args constructor for JPA/Hibernate.
	 */
	protected User() {
	
	}
	
	/**
	 * Constructs a new {@code User} entity with the specified UUID and player name.
	 *
	 * @param playerUniqueId the unique identifier (UUID) of the player, must not be null
	 * @param currentPlayerName the current name of the player, must not be null
	 * @throws IllegalArgumentException if any parameter is null
	 */
        public User(
                final @NotNull UUID playerUniqueId,
                final @NotNull String currentPlayerName
        ) {

                if (
                        playerUniqueId == null
                ) {
                        throw new IllegalArgumentException("Player unique id cannot be null");
                }
                if (
                        currentPlayerName == null || currentPlayerName.isBlank()
                ) {
                        throw new IllegalArgumentException("Player name cannot be null or blank");
                }

                this.uniqueId = playerUniqueId;
                this.playerName = currentPlayerName;
	}
	
	/**
	 * Constructs a new {@code User} entity from a Bukkit {@link Player} instance.
	 *
	 * @param bukkitPlayer the Bukkit player instance, must not be null
	 * @throws IllegalArgumentException if the player is null
	 */
        public User(
                final @NotNull Player bukkitPlayer
        ) {

                if (
                        bukkitPlayer == null
                ) {
                        throw new IllegalArgumentException("Player cannot be null");
                }

                final UUID playerUniqueId = bukkitPlayer.getUniqueId();
                final String currentPlayerName = bukkitPlayer.getName();

                if (
                        playerUniqueId == null
                ) {
                        throw new IllegalArgumentException("Player unique id cannot be null");
                }
                if (
                        currentPlayerName == null || currentPlayerName.isBlank()
                ) {
                        throw new IllegalArgumentException("Player name cannot be null or blank");
                }

                this.uniqueId = playerUniqueId;
                this.playerName = currentPlayerName;
	}
	
	/**
	 * Gets the unique identifier (UUID) of the player.
	 *
	 * @return the player's UUID, never null
	 */
	public @NotNull UUID getUniqueId() {
		return uniqueId;
	}
	
	/**
	 * Gets the current name of the player.
	 *
	 * @return the player's name, never null
	 */
	public @NotNull String getPlayerName() {
		return playerName;
	}
	
	/**
	 * Sets the current name of the player.
	 *
	 * @param newPlayerName the new player name to set, must not be null
	 * @throws IllegalArgumentException if the new player name is null
	 */
        public void setPlayerName(final @NotNull String newPlayerName) {

                if (
                        newPlayerName == null || newPlayerName.isBlank()
                ) {
                        throw new IllegalArgumentException("Player name cannot be null or blank");
                }

                this.playerName = newPlayerName;
	}
	
	/**
	 * Checks if this user is equal to another object.
	 * <p>
	 * Two users are considered equal if their UUIDs are equal.
	 * </p>
	 *
	 * @param otherObject the object to compare with, can be null
	 * @return {@code true} if the objects are equal, otherwise {@code false}
	 */
	@Override
	public boolean equals(final @Nullable Object otherObject) {
		if (
			this == otherObject
		) {
			return true;
		}
		if (
			otherObject == null || getClass() != otherObject.getClass()
		) {
			return false;
		}
		
		final User otherUser = (User) otherObject;
		return Objects.equals(
			uniqueId,
			otherUser.uniqueId
		);
	}
	
	/**
	 * Returns the hash code for this user, based on their UUID.
	 *
	 * @return the hash code of this user's UUID
	 */
	@Override
	public int hashCode() {
		return Objects.hashCode(uniqueId);
	}
	
	/**
	 * Returns a string representation of this user.
	 *
	 * @return a string containing the user's UUID and player name
	 */
	@Override
	public @NotNull String toString() {
		return String.format(
			"User{uniqueId=%s, playerName='%s'}",
			uniqueId,
			playerName
		);
	}
}