package com.raindropcentral.rds.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

@CSAlways
@SuppressWarnings("unused")
public class BossBarSection extends AConfigSection {

    private Integer update_period_ticks;
    private Integer view_distance;

    public BossBarSection(
            final EvaluationEnvironmentBuilder baseEnvironment
    ) {
        super(baseEnvironment);
    }

    public long getUpdatePeriodTicks() {
        if (this.update_period_ticks == null) {
            return 10L;
        }

        return Math.max(1L, this.update_period_ticks.longValue());
    }

    public int getViewDistance() {
        if (this.view_distance == null) {
            return 12;
        }

        return Math.max(1, this.view_distance);
    }
}
