package de.jexcellence.home.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration section for color scheme with gradient support.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@CSAlways
public class ColorSchemeSection extends AConfigSection {

    private String primaryGradient;
    private String secondaryGradient;
    private String successGradient;
    private String errorGradient;
    private String warningGradient;

    public ColorSchemeSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    public @NotNull String getPrimaryGradient() {
        return primaryGradient != null ? primaryGradient : "#1e3a8a:#60a5fa";
    }

    public @NotNull String getSecondaryGradient() {
        return secondaryGradient != null ? secondaryGradient : "#ea580c:#fb923c";
    }

    public @NotNull String getSuccessGradient() {
        return successGradient != null ? successGradient : "#059669:#10b981";
    }

    public @NotNull String getErrorGradient() {
        return errorGradient != null ? errorGradient : "#dc2626:#ef4444";
    }

    public @NotNull String getWarningGradient() {
        return warningGradient != null ? warningGradient : "#d97706:#f59e0b";
    }

    /**
     * Formats text with primary gradient.
     */
    public @NotNull String formatPrimary(@NotNull String text) {
        return "<gradient:" + getPrimaryGradient() + ">" + text + "</gradient>";
    }

    /**
     * Formats text with secondary gradient.
     */
    public @NotNull String formatSecondary(@NotNull String text) {
        return "<gradient:" + getSecondaryGradient() + ">" + text + "</gradient>";
    }

    /**
     * Formats text with success gradient.
     */
    public @NotNull String formatSuccess(@NotNull String text) {
        return "<gradient:" + getSuccessGradient() + ">" + text + "</gradient>";
    }

    /**
     * Formats text with error gradient.
     */
    public @NotNull String formatError(@NotNull String text) {
        return "<gradient:" + getErrorGradient() + ">" + text + "</gradient>";
    }

    /**
     * Formats text with warning gradient.
     */
    public @NotNull String formatWarning(@NotNull String text) {
        return "<gradient:" + getWarningGradient() + ">" + text + "</gradient>";
    }
}
