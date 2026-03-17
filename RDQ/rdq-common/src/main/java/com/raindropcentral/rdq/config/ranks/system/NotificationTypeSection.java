/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Represents the configuration section for a specific notification type in the rank system.
 *
 * <p>This class encapsulates settings related to how notifications are displayed and delivered,
 * such as enabling/disabling, broadcast options, display methods (title, action bar),
 * translation keys for text, and sound type. Default values are provided for all fields if unset.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
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
	 * @return true if enabled or null, false otherwise
	 */
	public Boolean getEnabled() {
		return
			this.isEnabled == null ||
			this.isEnabled;
	}
	
	/**
	 * Returns whether this notification should be broadcast to all players. If the value is null, returns true.
	 *
	 * @return true if broadcast or null, false otherwise
	 */
	public Boolean getBroadcast() {
		return
			this.broadcast == null ||
			this.broadcast;
	}
	
	/**
	 * Returns whether this notification should be sent to the specific player. If the value is null, returns true.
	 *
	 * @return true if sent to player or null, false otherwise
	 */
	public Boolean getSendToPlayer() {
		return
			this.sendToPlayer == null ||
			this.sendToPlayer;
	}
	
	/**
	 * Returns whether this notification should be shown as a title. If the value is null, returns true.
	 *
	 * @return true if shown as title or null, false otherwise
	 */
	public Boolean getShowTitle() {
		return
			this.showTitle == null ||
			this.showTitle;
	}
	
	/**
	 * Returns whether this notification should be shown as an action bar message. If the value is null, returns true.
	 *
	 * @return true if shown as action bar or null, false otherwise
	 */
	public Boolean getShowActionBar() {
		return
			this.showActionBar == null ||
			this.showActionBar;
	}
	
	/**
	 * Returns the translation key for the title text. If the value is null, returns a fallback key.
	 *
	 * @return the title text translation key
	 */
	public String getTitleTextKey() {
		return
			this.titleTextKey == null ?
			"rank.notification.fallback.title" :
			this.titleTextKey;
	}
	
	/**
	 * Returns the translation key for the subtitle text. If the value is null, returns a fallback key.
	 *
	 * @return the subtitle text translation key
	 */
	public String getSubtitleTextKey() {
		return
			this.subtitleTextKey == null ?
			"rank.notification.fallback.subtitle" :
			this.subtitleTextKey;
	}
	
	/**
	 * Returns the translation key for the action bar text. If the value is null, returns a fallback key.
	 *
	 * @return the action bar text translation key
	 */
	public String getActionBarTextKey() {
		return
			this.actionBarTextKey == null ?
			"rank.notification.fallback.action_bar" :
			this.actionBarTextKey;
	}
	
	/**
	 * Returns the sound type to play for the notification. If the value is null, returns "ENTITY_PLAYER_LEVELUP".
	 *
	 * @return the sound type
	 */
	public String getSoundType() {
		return
			this.soundType == null ?
			"ENTITY_PLAYER_LEVELUP" :
			this.soundType;
	}
	
}
