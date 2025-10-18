package com.raindropcentral.core.database.entity.inventory;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.server.RServer;
import com.raindropcentral.rplatform.database.converter.ItemStackMapConverter;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "r_player_inventory")
public class RPlayerInventory extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private RPlayer rPlayer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "server_id", nullable = false)
    private RServer rServer;

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "inventory", columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> inventory = new HashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "armor_contents", columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> armor = new HashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "enderchest", columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> enderchest = new HashMap<>();

    protected RPlayerInventory() {}

    public RPlayerInventory(
            final @NotNull RServer rServer,
            final @NotNull RPlayer rPlayer,
            final @NotNull Player player
    ) {
        this.rServer = Objects.requireNonNull(rServer, "rServer cannot be null");
        this.rPlayer = Objects.requireNonNull(rPlayer, "rPlayer cannot be null");
        Objects.requireNonNull(player, "player cannot be null");

        // Do not create a half-initialized entity when in creative. Persist empty maps instead.
        if (player.getGameMode() == GameMode.CREATIVE) {
            handleCreativeMode(player);
            this.inventory = new HashMap<>();
            this.armor = new HashMap<>();
            this.enderchest = new HashMap<>();
            return;
        }

        this.inventory = extractInventoryMap(player.getInventory().getContents());
        this.armor = extractInventoryMap(player.getInventory().getArmorContents());
        this.enderchest = extractInventoryMap(player.getEnderChest().getContents());
    }

    public @NotNull RPlayer getRPlayer() {
        return this.rPlayer;
    }

    public void setRPlayer(final @NotNull RPlayer rPlayer) {
        this.rPlayer = Objects.requireNonNull(rPlayer, "rPlayer cannot be null");
    }

    public @NotNull RServer getRServer() {
        return this.rServer;
    }

    public void setRServer(final @NotNull RServer rServer) {
        this.rServer = Objects.requireNonNull(rServer, "rServer cannot be null");
    }

    public @NotNull Map<Integer, ItemStack> getInventory() {
        return Map.copyOf(this.inventory);
    }

    public void setInventory(final @NotNull Map<Integer, ItemStack> inventory) {
        Objects.requireNonNull(inventory, "inventory cannot be null");
        this.inventory = new HashMap<>(inventory);
    }

    public @NotNull Map<Integer, ItemStack> getArmor() {
        return Map.copyOf(this.armor);
    }

    public void setArmor(final @NotNull Map<Integer, ItemStack> armor) {
        Objects.requireNonNull(armor, "armor cannot be null");
        this.armor = new HashMap<>(armor);
    }

    public @NotNull Map<Integer, ItemStack> getEnderchest() {
        return Map.copyOf(this.enderchest);
    }

    public void setEnderchest(final @NotNull Map<Integer, ItemStack> enderchest) {
        Objects.requireNonNull(enderchest, "enderchest cannot be null");
        this.enderchest = new HashMap<>(enderchest);
    }

    public void applyToPlayer(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");

        player.getInventory().setContents(
                mapToArray(this.inventory, player.getInventory().getSize())
        );
        player.getInventory().setArmorContents(
                mapToArray(this.armor, player.getInventory().getArmorContents().length)
        );
        player.getEnderChest().setContents(
                mapToArray(this.enderchest, player.getEnderChest().getSize())
        );
    }

    public void updateFromPlayer(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");

        this.inventory = extractInventoryMap(player.getInventory().getContents());
        this.armor = extractInventoryMap(player.getInventory().getArmorContents());
        this.enderchest = extractInventoryMap(player.getEnderChest().getContents());
    }

    public int getTotalItemCount() {
        return this.inventory.size() + this.armor.size() + this.enderchest.size();
    }

    public boolean isEmpty() {
        return this.inventory.isEmpty() && this.armor.isEmpty() && this.enderchest.isEmpty();
    }

    private static void handleCreativeMode(final @NotNull Player player) {
        CentralLogger.getLogger(RPlayerInventory.class.getName())
                .info("Player %s is in creative mode, inventory storage is disabled".formatted(player.getName()));
    }

    private static Map<Integer, ItemStack> extractInventoryMap(final @Nullable ItemStack[] items) {
        final Map<Integer, ItemStack> map = new HashMap<>();

        for (int slot = 0; slot < items.length; slot++) {
            final ItemStack item = items[slot];
            if (item != null && item.getType() != Material.AIR) {
                map.put(slot, item.clone());
            }
        }

        return map;
    }

    private static ItemStack[] mapToArray(
            final @NotNull Map<Integer, ItemStack> map,
            final int size
    ) {
        final ItemStack[] array = new ItemStack[size];
        map.forEach((slot, item) -> {
            if (slot >= 0 && slot < size) {
                array[slot] = item;
            }
        });
        return array;
    }

    @Override
    public String toString() {
        return "RPlayerInventory[id=%d, player=%s, server=%s, items=%d]"
                .formatted(
                        getId(),
                        rPlayer != null ? rPlayer.getPlayerName() : "null",
                        rServer != null ? rServer.getServerName() : "null",
                        getTotalItemCount()
                );
    }
}