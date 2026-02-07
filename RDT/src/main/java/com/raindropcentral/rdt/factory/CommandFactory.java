package com.raindropcentral.rdt.factory;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.items.Nexus;
import com.raindropcentral.rdt.utils.Type;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Factory that handles player-facing command operations related to towns.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Create and delete towns</li>
 *   <li>Show town info and debug details</li>
 *   <li>Handle invitations and join requests</li>
 *   <li>Claim chunks for a town (mayor only)</li>
 * </ul>
 *
 * Notes:
 * <ul>
 *   <li>Economy (Vault) deductions and refunds are performed when configured and available.</li>
 *   <li>Boss bar is refreshed after state-changing actions to reflect the current chunk/town.</li>
 *   <li>This class follows the uniform logging API used in the plugin via helper methods
 *   {@code info()}, {@code warn()}, and {@code error()} for consistency; kept intentionally
 *   quiet unless needed to avoid log spam.</li>
 * </ul>
 */
@SuppressWarnings({
        "StringTemplateMigration",
        "unused",
        "Duplicates"
})
public class CommandFactory {

    private final RDT plugin;

    /**
     * Construct a new command factory bound to the plugin instance.
     *
     * @param plugin main RDT plugin (required)
     */
    public CommandFactory(RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Pending invitations mapping player -> town they were invited to.
     */
    private final HashMap<UUID, UUID> townInvites = new HashMap<>();

    /**
     * Pending join requests per mayor: mayor -> (requestingPlayer -> town).
     */
    private final HashMap<UUID, Map<UUID, UUID>> townRequests = new HashMap<>();

    /**
     * Create a new town with the provided name, charging the founding cost if the economy is present.
     * Also claims the current chunk for the new town and updates the player's boss bar.
     * <p>
     * Usage: {@code /rt create <townName>}
     *
     * @param player invoking player (becomes mayor)
     * @param alias  command alias
     * @param args   command arguments
     */
    public void create(@NotNull Player player, @NotNull String alias, @NotNull String[] args) {
        if (args.length < 2){
            new I18n.Builder("bad_args.create", player).build().sendMessage();
            return;
        }

        if (this.plugin.getTownRepository().findByTName(args[1]) != null){
            new I18n.Builder("town_exists").build().sendMessage();
            return;
        }
        RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (rPlayer.getTownUUID() != null){
            new I18n.Builder("no_town", player).build().sendMessage();
            return;
        }
        final double cost = this.plugin.getDefaultConfig().getFoundingCost();
        if (this.plugin.getEco() != null){
            if (!this.plugin.getEco().has(player, cost)){
                new I18n.Builder("no_funds_town", player).withPlaceholder("cost", cost).build().sendMessage();
                return;
            }
            this.plugin.getEco().withdrawPlayer(player, cost);
            new I18n.Builder("charged", player).withPlaceholder("cost", cost).build().sendMessage();
        } else {
            new I18n.Builder("no_eco", player).build().sendMessage();
        }
        UUID town_uuid = UUID.randomUUID();

        if (this.plugin.getTownRepository().findByTownUUID(town_uuid) != null) {
            new I18n.Builder("uuid_used", player).build().sendMessage();
            return;
        }
        ItemStack nexus = Nexus.getNexusItem(this.plugin, town_uuid, args[1]);
        player.getInventory().addItem(nexus);
        new I18n.Builder("place_nexus", player).build().sendMessage();
    }

    /**
     * Delete the town owned by the invoking mayor, refunding a portion of construction costs
     * when the economy is present. Removes the player from the town and updates the boss bar.
     *
     * @param player invoking player (must be the mayor)
     * @param alias  command alias
     * @param args   command arguments
     */
    public void delete(@NotNull Player player, @NotNull String alias, @NotNull String[] args) {
        RTown rTown = this.plugin.getTownRepository().findByMayor(player.getUniqueId());
        RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (rPlayer == null){
            new I18n.Builder("no_player", player).build().sendMessage();
            return;
        }
        if (rTown == null) {
            new I18n.Builder("no_town", player).build().sendMessage();
            return;
        }
        String name = rTown.getTownName();
        if (rTown.getId() == null) {
            new I18n.Builder("no_town_db", player).withPlaceholder("town", name).build().sendMessage();
            return;
        }
        // Refund con cost
        if (this.plugin.getEco() != null) {
            final double ref = (rTown.getCon_spent() * this.plugin.getDefaultConfig().getRefundRate());
            this.plugin.getEco().depositPlayer(player, ref);
            new I18n.Builder("refunded_town", player)
                    .withPlaceholder("refund", ref)
                    .withPlaceholder("town", name)
                    .build()
                    .sendMessage();
        }
        // Remove mayor from Town
        rPlayer.setTownUUID(null);
        this.plugin.getPlayerRepository().update(rPlayer);
        // Remove members from Town
        rTown.getMembers().forEach(s -> {
            Player member = Bukkit.getPlayer(UUID.fromString(s));
            RDTPlayer rPlayer2;
            if (member == null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(s));
                rPlayer2 = this.plugin.getPlayerRepository().findByPlayer(offlinePlayer.getUniqueId());
            } else {
                rPlayer2 = this.plugin.getPlayerRepository().findByPlayer(member.getUniqueId());
            }
            if (rPlayer2 != null) {
                if (rPlayer2.getTownUUID() != null) {
                    rPlayer2.setTownUUID(null);
                    this.plugin.getPlayerRepository().update(rPlayer2);
                }
            }
        });
        rTown.getNexus_location().getBlock().setType(Material.AIR);
        // Delete RTown entity
        this.plugin.getTownRepository().delete(rTown.getId());
        // Update boss bar
        this.plugin.getBossBarFactory().run(
                player,
                player.getLocation().getChunk().getX(),
                player.getLocation().getChunk().getZ()
        );
    }

    /**
     * Display a summary of the invoking player's town, including name, mayor UUID,
     * claimed chunks, construction cost, bank balance, and upkeep.
     *
     * @param player invoking player
     * @param alias  command alias
     * @param args   command arguments
     */
    public void info(@NotNull Player player, @NotNull String alias, @NotNull String[] args) {
        RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (rPlayer.getTownUUID() == null){
            new I18n.Builder("no_town", player).build().sendMessage();
            return;
        }
        RTown rTown = this.plugin.getTownRepository().findByTownUUID(rPlayer.getTownUUID());
        player.sendMessage(
                "====RaindropTowns===\n" +
                        "Town: " + rTown.getTownName() + "\n" +
                        "Mayor: " + rTown.getMayor() + "\n" +
                        "Chunks: " + rTown.getChunks().size() + " / " + this.plugin.getDefaultConfig().getClaimLimit() + "\n" +
                        "Con Cost: " + rTown.getCon_spent() + "\n" +
                        "Bank: " + rTown.getBank() + "\n" +
                        "Upkeep: " + rTown.getUpkeep() + "\n" +
                        "Your Contributions: " + rPlayer.getContributions() + "\n" +
                        "====================");
    }

    /**
     * Print the raw {@code RTown#toString()} for the invoking player's town.
     * Intended for troubleshooting.h
     *
     * @param player invoking player
     * @param alias  command alias
     * @param args   command arguments
     */
    public void debug(@NotNull Player player, @NotNull String alias, @NotNull String[] args) {
        RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (rPlayer.getTownUUID() == null){
            new I18n.Builder("no_town", player).build().sendMessage();
            return;
        }
        RTown rTown = this.plugin.getTownRepository().findByTownUUID(rPlayer.getTownUUID());
        player.sendMessage(rTown.toString());
    }
    /**
     * Invite another player to the invoking player's town.
     * Stores an invitation for the target player to accept.
     * Usage: {@code /rt invite <player>}
     *
     * @param player invoking player (must be in a town)
     * @param alias  command alias
     * @param args   command arguments; expects target player name at index 1
     */
    public void invite(@NotNull Player player, @NotNull String alias, @NotNull String[] args) {
        if (args.length < 2){
            new I18n.Builder("bad_args.invite", player).build().sendMessage();
            return;
        }
        Player target = this.plugin.getServer().getPlayer(args[1]);
        if (target == null){
            new I18n.Builder("no_player", player).build().sendMessage();
            return;
        }
        RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (rPlayer.getTownUUID() == null){
            new I18n.Builder("no_town", player).build().sendMessage();
            return;
        }
        RTown rTown = this.plugin.getTownRepository().findByTownUUID(rPlayer.getTownUUID());
        if (rTown == null){
            new I18n.Builder("no_town", player).build().sendMessage();
            return;
        }
        if (rTown.getMembers().contains(target.getUniqueId().toString())){
            new I18n.Builder("player_in_town", player).build().sendMessage();
            return;
        }
        new I18n.Builder("invited", player).withPlaceholder("town", rTown.getTownName()).build().sendMessage();
        this.townInvites.put(target.getUniqueId(), rTown.getIdentifier());
    }

    /**
     * Request to join a town by name. The mayor of the town is notified and may accept the request.
     * <p>
     * Usage: {@code /rt join <townName>}
     *
     * @param player invoking player
     * @param alias  command alias
     * @param args   command arguments; expects town name at index 1
     */
    public void join(@NotNull Player player, @NotNull String alias, @NotNull String[] args) {
        RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (rPlayer == null) {
            new I18n.Builder("no_player", player).build().sendMessage();
            return;
        }
        if (args.length < 2){
            new I18n.Builder("bad_args.invite", player).build().sendMessage();
            return;
        }
        RTown rTown = this.plugin.getTownRepository().findByTName(args[1]);
        if (rTown == null) {
            new I18n.Builder("no_town", player).build().sendMessage();
            return;
        }
        Player mayor = this.plugin.getServer().getPlayer(rTown.getMayor());
        if (mayor == null) {
            new I18n.Builder("no_mayor", player).build().sendMessage();
            return;
        }
        new I18n.Builder("join_request", player)
                .withPlaceholder("player", player.getName())
                .withPlaceholder("town", rTown.getTownName())
                .build()
                .sendMessage();
        this.townRequests.put(rTown.getMayor(), Map.of(player.getUniqueId(), rTown.getIdentifier()));
    }


    /**
     * Accept a pending invitation or, if executed by a mayor, accept pending join requests.
     * <p>
     * Behavior:
     * <ul>
     *   <li>If the executor has a stored invite, they join that town.</li>
     *   <li>Otherwise, if the executor is a mayor with pending requests, the first set of requests
     *   are accepted and members added accordingly.</li>
     * </ul>
     * Boss bars are updated for affected players.
     *
     * @param player invoking player
     * @param alias  command alias
     * @param args   command arguments
     */
    public void accept(@NotNull Player player, @NotNull String alias, @NotNull String[] args) {
        // PLAYER ACTION
        if (townInvites.containsKey(player.getUniqueId())) {
            RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
            if (rPlayer == null) return;
            RTown rTown = this.plugin.getTownRepository().findByTownUUID(townInvites.get(player.getUniqueId()));
            if (rTown == null) {
                new I18n.Builder("no_town", player).build().sendMessage();
                return;
            }
            rTown.getMembers().add(player.getUniqueId().toString());
            new I18n.Builder("accepted", player).withPlaceholder("town", rTown.getTownName()).build().sendMessage();
            rPlayer.setTownUUID(rTown.getIdentifier());
            this.plugin.getPlayerRepository().update(rPlayer);
            this.plugin.getTownRepository().update(rTown);
            this.townInvites.remove(player.getUniqueId());
            notifyMembers(player, rTown);
            this.plugin.getBossBarFactory().run(
                    player,
                    player.getLocation().getChunk().getX(),
                    player.getLocation().getChunk().getZ()
            );
            return;
        }
        //MAYOR ACTION
        if (!townRequests.containsKey(player.getUniqueId())) {
            new I18n.Builder("no_pending", player).build().sendMessage();
            return;
        }
        Map<UUID, UUID> request = townRequests.get(player.getUniqueId());
        request.forEach((uuid, townUUID) -> {
            RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(uuid);
            if (rPlayer == null) return;
            RTown rTown = this.plugin.getTownRepository().findByTownUUID(townUUID);
            if (rTown == null) return;
            Player invited = this.plugin.getServer().getPlayer(uuid);
            if (invited == null) return;
            new I18n.Builder("accepted", player).withPlaceholder("town", rTown.getTownName()).build().sendMessage();
            notifyMembers(invited, rTown);
            rPlayer.setTownUUID(townUUID);
            this.plugin.getPlayerRepository().update(rPlayer);
            rTown.getMembers().add(uuid.toString());
            this.plugin.getTownRepository().update(rTown);
            // Update boss bar
            this.plugin.getBossBarFactory().run(
                    invited,
                    player.getLocation().getChunk().getX(),
                    player.getLocation().getChunk().getZ()
            );
        });
    }

    /**
     * Notify all online members of the given RTown that a player has joined.
     *
     * @param invited player who joined
     * @param rTown    RTown the player joined
     */
    private void notifyMembers(Player invited, @NotNull RTown rTown) {
        rTown.getMembers().forEach(s -> {
            Player member = this.plugin.getServer().getPlayer(UUID.fromString(s));
            if (member == null) return;
            new I18n.Builder("member_notice", member)
                    .withPlaceholder("player", invited.getName())
                    .withPlaceholder("town", rTown.getTownName())
                    .build()
                    .sendMessage();
        });
    }

    /**
     * Claim the current chunk for the player's town. Only mayors may claim, and the target chunk
     * must be adjacent to an already-claimed chunk. Cost scales with the number of existing claims
     * when the economy is present. Boss bar is refreshed on success.
     *
     * @param player invoking player (must be mayor)
     * @param alias  command alias
     * @param args   command arguments
     */
    public void claim(
            @NotNull Player player,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (rPlayer == null) return;
        if (rPlayer.getTownUUID() == null) {
            new I18n.Builder("no_town", player).build().sendMessage();
            return;
        }
        RTown rTown = this.plugin.getTownRepository().findByTownUUID(rPlayer.getTownUUID());
        if (rTown == null) return;
        // Check mayor
        if (!rTown.getMayor().equals(player.getUniqueId())) {
            new I18n.Builder("mayor_action", player).build().sendMessage();
            return;
        }
        // Check if a chunk is in RTown
        Chunk chunk = player.getLocation().getChunk();
        AtomicBoolean isInChunkInTown = new AtomicBoolean(false);
        AtomicBoolean isAdjacentChunk = new AtomicBoolean(false);
        rTown.getChunks().forEach(rChunk -> {
            if (chunk.getX() == rChunk.getX_loc() && chunk.getZ() == rChunk.getZ_loc()) isInChunkInTown.set(true);
            if (chunk.getX() + 1 == rChunk.getX_loc() && chunk.getZ() == rChunk.getZ_loc()) isAdjacentChunk.set(true);
            if (chunk.getX() - 1 == rChunk.getX_loc() && chunk.getZ() == rChunk.getZ_loc()) isAdjacentChunk.set(true);
            if (chunk.getX() == rChunk.getX_loc() && chunk.getZ() + 1 == rChunk.getZ_loc()) isAdjacentChunk.set(true);
            if (chunk.getX() == rChunk.getX_loc() && chunk.getZ() - 1 == rChunk.getZ_loc()) isAdjacentChunk.set(true);
        });
        if (isInChunkInTown.get()) {
            new I18n.Builder("chunk_owned", player).build().sendMessage();
            return;
        }
        if (!isAdjacentChunk.get()) {
            new I18n.Builder("chunk_adjacent", player).build().sendMessage();
            return;
        }
        if (rTown.getChunks().size() >= this.plugin.getDefaultConfig().getClaimLimit()) {
            new I18n.Builder("claim_limit", player).build().sendMessage();
            return;
        }
        if (this.plugin.getEco() != null) {
            double cost = (
                    rTown.getChunks().size() *
                            this.plugin.getDefaultConfig().getBaseClaimCost()  *
                            this.plugin.getDefaultConfig().getClaimRate()
            );
            if (!this.plugin.getEco().has(player, cost)) {
                new I18n.Builder("claim_cost", player).withPlaceholder("cost", cost).build().sendMessage();
                return;
            }
            this.plugin.getEco().withdrawPlayer(player, cost);
            rTown.addCon_spent(cost);
            new I18n.Builder("claim_charged", player).withPlaceholder("cost", cost).build().sendMessage();
            player.sendMessage(
                    "You have claimed " +
                            rTown.getChunks().size() +
                            " / " +
                            this.plugin.getDefaultConfig().getClaimLimit() +
                            " chunks!"
            );
        }
        rTown.getChunks().add(new RChunk(rTown, chunk.getX(), chunk.getZ(), Type.DEFAULT));
        new I18n.Builder("claimed", player)
                .withPlaceholder("chunks", rTown.getChunks().size())
                .build()
                .sendMessage();
        this.plugin.getTownRepository().update(rTown);
        // Update boss bar
        this.plugin.getBossBarFactory().run(player, chunk.getX(), chunk.getZ());
    }

    //unclaiming a chunk
    public void unclaim(
            @NotNull Player player,
            @NotNull String alias,
            @NotNull String[] args
    ){
        RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (rPlayer == null) return;
        if (rPlayer.getTownUUID() == null) {
            new I18n.Builder("no_town", player).build().sendMessage();
            return;
        }
        RTown rTown = this.plugin.getTownRepository().findByTownUUID(rPlayer.getTownUUID());
        if (rTown == null) return;
        // Check mayor
        if (!rTown.getMayor().equals(player.getUniqueId())) {
            new I18n.Builder("mayor_action", player).build().sendMessage();
            return;
        }
        // Check if a chunk is in RTown
        Chunk chunk = player.getLocation().getChunk();
        rTown.getChunks().removeIf( (rChunk) -> {
            if (chunk.getX() == rChunk.getX_loc() && chunk.getZ() == rChunk.getZ_loc() && rChunk.getType() != Type.NEXUS) {
                new I18n.Builder("unclaim_failed", player).build().sendMessage();
                return true;
            }
            return false;
        });
        if (this.plugin.getEco() != null) {
            //refund
            double cost = (
                    rTown.getChunks().size() *
                            this.plugin.getDefaultConfig().getBaseClaimCost() *
                            this.plugin.getDefaultConfig().getClaimRate()
            );
            double refund = cost * this.plugin.getDefaultConfig().getRefundRate();
            this.plugin.getEco().depositPlayer(player, cost);
            rTown.subCon_spent(cost);

            new I18n.Builder("refunded_chunk", player)
                    .withPlaceholder("refund", refund)
                    .build()
                    .sendMessage();
            new I18n.Builder("player_balance", player)
                    .withPlaceholder("balance", this.plugin.getEco().getBalance(player))
                    .build()
                    .sendMessage();
        }
        this.plugin.getBossBarFactory().run(player, chunk.getX(), chunk.getZ());
        this.plugin.getTownRepository().update(rTown);
    }

    public void deposit(
            @NotNull Player player,
            @NotNull String alias,
            @NotNull String[] args
    ){
        if (this.plugin.getEco() == null) {
            new I18n.Builder("no_eco", player).build().sendMessage();
            return;
        }
        RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (rPlayer == null) return;
        if (rPlayer.getTownUUID() == null) {
            new I18n.Builder("no_town", player).build().sendMessage();
            return;
        }
        RTown rTown = this.plugin.getTownRepository().findByTownUUID(rPlayer.getTownUUID());
        if (rTown == null) return;
        // Check mayor
        if (!rTown.getMayor().equals(player.getUniqueId())) {
            new I18n.Builder("mayor_action", player).build().sendMessage();
            return;
        }
        if (args.length != 2) {
            new I18n.Builder("bad_args.deposit", player).build().sendMessage();
            return;
        }
        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                new I18n.Builder("bad_args.positive", player).build().sendMessage();
                return;
            }
            if (this.plugin.getEco().getBalance(player) < amount) {
                new I18n.Builder("no_funds_deposit", player).build().sendMessage();
                return;
            }
            this.plugin.getEco().withdrawPlayer(player, amount);
            new I18n.Builder("town_deposit", player).withPlaceholder("amount", amount).build().sendMessage();
            new I18n.Builder("player_balance", player)
                    .withPlaceholder("balance", this.plugin.getEco().getBalance(player))
                    .build()
                    .sendMessage();
            rTown.deposit(amount);
            new I18n.Builder("town_balance", player)
                    .withPlaceholder("balance", rTown.getBank())
                    .build()
                    .sendMessage();
            this.plugin.getTownRepository().update(rTown);
            rPlayer.addContributions(amount);
            this.plugin.getPlayerRepository().update(rPlayer);
        } catch (NumberFormatException e) {
            new I18n.Builder("bad_args.number", player).build().sendMessage();
        }
    }
    public void withdraw(
            @NotNull Player player,
            @NotNull String alias,
            @NotNull String[] args
    ){
        if (this.plugin.getEco() == null) {
            new I18n.Builder("no_eco", player).build().sendMessage();
            return;
        }
        RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (rPlayer == null) return;
        if (rPlayer.getTownUUID() == null) {
            new I18n.Builder("no_town", player).build().sendMessage();
            return;
        }
        RTown rTown = this.plugin.getTownRepository().findByTownUUID(rPlayer.getTownUUID());
        if (rTown == null) return;
        // Check mayor
        if (!rTown.getMayor().equals(player.getUniqueId())) {
            new I18n.Builder("mayor_action", player).build().sendMessage();
            return;
        }
        if (args.length != 2) {
            new I18n.Builder("bad_args.withdraw", player).build().sendMessage();
            return;
        }
        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                new I18n.Builder("bad_args.amount", player).build().sendMessage();
                return;
            }
            if (rTown.getBank() < amount) {
                new I18n.Builder("no_funds_withdraw", player).build().sendMessage();
                return;
            }
            this.plugin.getEco().depositPlayer(player, amount);
            new I18n.Builder("town_withdraw", player).withPlaceholder("amount", amount).build().sendMessage();
            new I18n.Builder("player_balance", player)
                    .withPlaceholder("balance", this.plugin.getEco().getBalance(player))
                    .build()
                    .sendMessage();
            rTown.withdraw(amount);
            new I18n.Builder("town_balance", player)
                    .withPlaceholder("balance", rTown.getBank())
                    .build()
                    .sendMessage();
            this.plugin.getTownRepository().update(rTown);
            rPlayer.addWithdrew(amount);
            this.plugin.getPlayerRepository().update(rPlayer);
        } catch (NumberFormatException e) {
            new I18n.Builder("bad_args.number", player).build().sendMessage();
        }
    }
}