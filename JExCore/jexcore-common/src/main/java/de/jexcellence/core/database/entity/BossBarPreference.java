package de.jexcellence.core.database.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One player's preference row for one registered boss-bar provider. Owns a
 * collection of {@link BossBarPreferenceOption} child rows for
 * provider-specific settings.
 */
@Entity
@Table(
        name = "jexcore_boss_bar_preference",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jexcore_boss_bar_player_provider",
                columnNames = {"player_uuid", "provider_key"}
        )
)
public class BossBarPreference extends LongIdEntity {

    @Column(name = "player_uuid", nullable = false)
    private UUID playerUuid;

    @Column(name = "provider_key", nullable = false, length = 64)
    private String providerKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @OneToMany(
            mappedBy = "preference",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("optionKey ASC")
    private Set<BossBarPreferenceOption> options = new LinkedHashSet<>();

    protected BossBarPreference() {
    }

    public BossBarPreference(@NotNull UUID playerUuid, @NotNull String providerKey, boolean enabled) {
        this.playerUuid = playerUuid;
        this.providerKey = providerKey;
        this.enabled = enabled;
    }

    public @NotNull UUID getPlayerUuid() {
        return this.playerUuid;
    }

    public @NotNull String getProviderKey() {
        return this.providerKey;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public @NotNull Set<BossBarPreferenceOption> getOptions() {
        return this.options;
    }

    @Override
    public String toString() {
        return "BossBarPreference[" + this.playerUuid + "/" + this.providerKey + "/" + this.enabled + "]";
    }
}
