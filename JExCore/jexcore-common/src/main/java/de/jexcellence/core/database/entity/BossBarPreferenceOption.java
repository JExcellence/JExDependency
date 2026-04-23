package de.jexcellence.core.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;

/**
 * A provider-specific option value attached to a {@link BossBarPreference}.
 */
@Entity
@Table(
        name = "jexcore_boss_bar_preference_option",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jexcore_boss_bar_pref_option",
                columnNames = {"preference_id", "option_key"}
        )
)
public class BossBarPreferenceOption extends LongIdEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preference_id", nullable = false)
    private BossBarPreference preference;

    @Column(name = "option_key", nullable = false, length = 64)
    private String optionKey;

    @Column(name = "option_value", nullable = false, length = 128)
    private String optionValue;

    protected BossBarPreferenceOption() {
    }

    public BossBarPreferenceOption(
            @NotNull BossBarPreference preference,
            @NotNull String optionKey,
            @NotNull String optionValue
    ) {
        this.preference = preference;
        this.optionKey = optionKey;
        this.optionValue = optionValue;
    }

    public @NotNull BossBarPreference getPreference() {
        return this.preference;
    }

    public @NotNull String getOptionKey() {
        return this.optionKey;
    }

    public @NotNull String getOptionValue() {
        return this.optionValue;
    }

    public void setOptionValue(@NotNull String optionValue) {
        this.optionValue = optionValue;
    }
}
