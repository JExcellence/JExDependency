package com.raindropcentral.rdt.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@CSAlways
@SuppressWarnings("unused")
public class ConfigSection extends AConfigSection {

    private Double founding_cost;
    private Double refund_rate;

    private Double base_claim_cost;

    private Double claim_rate;

    private Integer claim_limit;

    private Double tax_base;

    private Double tax_rate;

    private Double tax_interval;

    private String first_tax_tick;

    private long tax_grace_period;

    public ConfigSection(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Loads the RDT root config from disk.
     *
     * @param configFile config file to parse
     * @return parsed config section
     */
    public static @NotNull ConfigSection fromFile(final @NotNull File configFile) {
        return fromConfiguration(YamlConfiguration.loadConfiguration(configFile));
    }

    /**
     * Loads the RDT root config from a bundled resource stream.
     *
     * @param inputStream config resource stream
     * @return parsed config section
     */
    public static @NotNull ConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        final InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
    }

    /**
     * Creates a default config section with field-level fallback values.
     *
     * @return default config section
     */
    public static @NotNull ConfigSection createDefault() {
        return new ConfigSection(new EvaluationEnvironmentBuilder());
    }

    private static @NotNull ConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        final ConfigSection section = createDefault();
        if (configuration.contains("founding_cost")) {
            section.founding_cost = configuration.getDouble("founding_cost");
        }
        if (configuration.contains("refund_rate")) {
            section.refund_rate = configuration.getDouble("refund_rate");
        }
        if (configuration.contains("base_claim_cost")) {
            section.base_claim_cost = configuration.getDouble("base_claim_cost");
        }
        if (configuration.contains("claim_rate")) {
            section.claim_rate = configuration.getDouble("claim_rate");
        }
        if (configuration.contains("claim_limit")) {
            section.claim_limit = configuration.getInt("claim_limit");
        }
        if (configuration.contains("tax_base")) {
            section.tax_base = configuration.getDouble("tax_base");
        }
        if (configuration.contains("tax_rate")) {
            section.tax_rate = configuration.getDouble("tax_rate");
        }
        if (configuration.contains("tax_interval")) {
            section.tax_interval = configuration.getDouble("tax_interval");
        }
        if (configuration.contains("first_tax_tick")) {
            section.first_tax_tick = configuration.getString("first_tax_tick");
        }
        if (configuration.contains("tax_grace_period")) {
            section.tax_grace_period = configuration.getLong("tax_grace_period");
        }
        return section;
    }

    public Double getFoundingCost() {
        return founding_cost == null ? 1000.0 : founding_cost;
    }

    public Double getRefundRate() {
        return refund_rate == null ? 1.0 : refund_rate;
    }

    public Double getBaseClaimCost() {
        return base_claim_cost == null ? 100.0 : base_claim_cost;
    }

    public Double getClaimRate() {
        return claim_rate == null ? 1.5 : claim_rate;
    }

    public Integer getClaimLimit() {
        return claim_limit == null ? 20 : claim_limit;
    }

    public Double getTaxBase()  {
        return tax_base == null ? 100 : tax_base;
    }

    public Double getTaxRate()  {
        return tax_rate == null ? 0.7 : tax_rate;
    }
    //in sec
    public Double getTaxInterval()  {
        return tax_interval == null ? 86400 : tax_interval;
    }

    public String getFirstTaxTick() {
        return first_tax_tick;
    }

    public long getGracePeriod() {
        return tax_grace_period;
    }

}
