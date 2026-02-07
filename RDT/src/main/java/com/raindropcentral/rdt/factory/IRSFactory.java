package com.raindropcentral.rdt.factory;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class IRSFactory {

    private final RDT plugin;

    public IRSFactory(RDT plugin) {
        this.plugin = plugin;
    }

    public void runAll() {
        // Get all towns
        List<RTown> rTowns = this.plugin.getTownRepository().findAll(0, Integer.MAX_VALUE);
        rTowns.forEach(rTown -> {
            if (rTown == null) return;
            run(rTown);
        });
    }

    public void run(RTown rTown) {
        if (rTown == null) return;
        if (rTown.getLast_taxed() == 0) {
            if (this.plugin.getDefaultConfig().getGracePeriod() > 0) {
                rTown.getMembers().forEach((uuid) -> {
                    Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                    if (player != null) {
                        new I18n.Builder("grace_period", player)
                                .withPlaceholder("time", this.plugin.getDefaultConfig().getGracePeriod() * 20)
                                .build()
                                .sendMessage();
                    }
                });
            }
            // Town has never been taxed

            this.plugin.getPlatform().getScheduler().runRepeating(
                    () -> tax(rTown),
                    this.plugin.getDefaultConfig().getGracePeriod(),
                    this.plugin.getDefaultConfig().getTaxInterval().longValue()
            );
        }
        this.plugin.getScheduler().runRepeating(
                () -> tax(rTown),
                System.currentTimeMillis() >
                        (rTown.getLast_taxed() + this.plugin.getDefaultConfig().getTaxInterval()) ?
                        0 :
                        System.currentTimeMillis() - rTown.getLast_taxed(),
                this.plugin.getDefaultConfig().getTaxInterval().longValue()
        );
    }

    public void tax(RTown rTown) {
        if (rTown == null) return;
        if (rTown.getId() == null) return;
        // Get updated town from cache
        RTown rTownCurrent = this.plugin.getTownRepository().findById(rTown.getId()).orElse(null);
        // Town was deleted or db corrupted
        if (rTownCurrent == null) return;
        // Take the taxes

        rTownCurrent.withdraw(
                this.plugin.getDefaultConfig().getTaxBase() *
                        this.plugin.getDefaultConfig().getTaxRate() *
                        rTownCurrent.getChunks().size()
        );
        // Update last taxed time
        rTownCurrent.setLast_Taxed(System.currentTimeMillis());
        // Notify town members
        rTownCurrent.getMembers().forEach(uuid -> {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if (player != null) {
                new I18n.Builder("taxed", player)
                        .withPlaceholder("time", this.plugin.getDefaultConfig().getGracePeriod() * 20)
                        .build()
                        .sendMessage();
            }
        });
        // Update the entity
        this.plugin.getTownRepository().update(rTownCurrent);
    }
}