package com.raindropcentral.rdq.database.entity.perk.event;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.EventTriggeredPerk;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

/** Prevents loss of experience on death for active players. */
@Entity
@DiscriminatorValue("KEEP_EXPERIENCE")
public class KeepExperiencePerk extends EventTriggeredPerk {

    protected KeepExperiencePerk() { super(); }

    public KeepExperiencePerk(
            final @NotNull String identifier,
            final @NotNull PerkSection perkSection,
            final @NotNull RDQ rdq
    ) {
        super(identifier, perkSection, rdq);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(final @NotNull PlayerDeathEvent event) {
        final Player player = event.getEntity();
        if (!this.shouldProcessEventForPlayer(player.getUniqueId())) return;

        final int totalExp = player.getTotalExperience();
        final int level = player.getLevel();
        final float exp = player.getExp();

        event.setDroppedExp(0);

        Bukkit.getScheduler().runTask(this.getRdq().getPlugin(), () -> {
            if (player.isOnline()) {
                player.setTotalExperience(totalExp);
                player.setLevel(level);
                player.setExp(exp);
            }
        });
    }
}
