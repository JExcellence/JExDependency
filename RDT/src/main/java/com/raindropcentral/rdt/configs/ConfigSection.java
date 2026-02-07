package com.raindropcentral.rdt.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

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