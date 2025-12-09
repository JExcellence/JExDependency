package com.raindropcentral.rdq2.config.ranks.system;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Represents the configuration section for a specific notification type in the rank system.
 * <p>
 * This class encapsulates settings related to how notifications are displayed and delivered,
 * such as enabling/disabling, broadcast options, display methods (title, action bar),
 * translation keys for text, and sound type. Default values are provided for all fields if unset.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class NotificationTypeSection extends AConfigSection {

    /**
     * Whether this notification type is enabled. If null, defaults to true.
     */
    private Boolean isEnabled;

    /**
     * Whether to broadcast the notification to all players. If null, defaults to true.
     */
    private Boolean broadcast;

    /**
     * Whether to send the notification to the specific player. If null, defaults to true.
     */
    private Boolean sendToPlayer;

    /**
     * Whether to show the notification as a title. If null, defaults to true.
     */
    private Boolean showTitle;

    /**
     * Whether to show the notification as an action bar message. If null, defaults to true.
     */
    private Boolean showActionBar;

    /**
     * The translation key for the title text. If null, a fallback key is used.
     */
    private String titleTextKey;

    /**
     * The translation key for the subtitle text. If null, a fallback key is used.
     */
    private String subtitleTextKey;

    /**
     * The translation key for the action bar text. If null, a fallback key is used.
     */
    private String actionBarTextKey;

    /**
     * The sound type to play for the notification. If null, defaults to "ENTITY_PLAYER_LEVELUP".
     */
    private String soundType;

    /**
     * Constructs a new NotificationTypeSection with the given evaluation environment.
     *
     * @param baseEnvironment the base evaluation environment for this section
     */
    public NotificationTypeSection(final EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Returns whether this notification type is enabled. If the value is null, returns true.
     *
     * @return {@code true} if the notification type is enabled or unset, {@code false} otherwise
     */
    public Boolean getEnabled() {
        return this.isEnabled == null || this.isEnabled;
    }

    /**
     * Returns whether this notification should be broadcast to all players. If the value is null, returns true.
     *
     * @return {@code true} if broadcasting is enabled or unset, {@code false} otherwise
     */
    public Boolean getBroadcast() {
        return this.broadcast == null || this.broadcast;
    }

    /**
     * Returns whether this notification should be sent to the specific player. If the value is null, returns true.
     *
     * @return {@code true} if sending to the player is enabled or unset, {@code false} otherwise
     */
    public Boolean getSendToPlayer() {
        return this.sendToPlayer == null || this.sendToPlayer;
    }

    /**
     * Returns whether this notification should be shown as a title. If the value is null, returns true.
     *
     * @return {@code true} if the title display is enabled or unset, {@code false} otherwise
     */
    public Boolean getShowTitle() {
        return this.showTitle == null || this.showTitle;
    }

    /**
     * Returns whether this notification should be shown as an action bar message. If the value is null, returns true.
     *
     * @return {@code true} if the action bar display is enabled or unset, {@code false} otherwise
     */
    public Boolean getShowActionBar() {
        return this.showActionBar == null || this.showActionBar;
    }

    /**
     * Returns the translation key for the title text. If the value is null, returns a fallback key.
     *
     * @return the title text translation key or {@code rank.notification.fallback.title} if unset
     */
    public String getTitleTextKey() {
        return this.titleTextKey == null ? "rank.notification.fallback.title" : this.titleTextKey;
    }

    /**
     * Returns the translation key for the subtitle text. If the value is null, returns a fallback key.
     *
     * @return the subtitle text translation key or {@code rank.notification.fallback.subtitle} if unset
     */
    public String getSubtitleTextKey() {
        return this.subtitleTextKey == null ? "rank.notification.fallback.subtitle" : this.subtitleTextKey;
    }

    /**
     * Returns the translation key for the action bar text. If the value is null, returns a fallback key.
     *
     * @return the action bar text translation key or {@code rank.notification.fallback.action_bar} if unset
     */
    public String getActionBarTextKey() {
        return this.actionBarTextKey == null ? "rank.notification.fallback.action_bar" : this.actionBarTextKey;
    }

    /**
     * Returns the sound type to play for the notification. If the value is null, returns "ENTITY_PLAYER_LEVELUP".
     *
     * @return the sound type or {@code ENTITY_PLAYER_LEVELUP} if unset
     */
    public String getSoundType() {
        return this.soundType == null ? "ENTITY_PLAYER_LEVELUP" : this.soundType;
    }

}
