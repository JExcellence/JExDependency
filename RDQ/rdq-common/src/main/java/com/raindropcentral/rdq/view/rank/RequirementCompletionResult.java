package com.raindropcentral.rdq.view.rank;

import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class RequirementCompletionResult {

	private final boolean success;
	private final String messageKey;
	private final RequirementProgressData updatedProgress;

	public RequirementCompletionResult(
		final boolean success,
		final @NotNull String messageKey,
		final @NotNull RequirementProgressData updatedProgress
	) {
		this.success = success;
		this.messageKey = Objects.requireNonNull(messageKey, "Message key cannot be null");
		this.updatedProgress = Objects.requireNonNull(updatedProgress, "Updated progress cannot be null");
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public RequirementProgressData getUpdatedProgress() {
		return updatedProgress;
	}

	public void sendMessage(final @NotNull Player player) {
		Objects.requireNonNull(player, "Player cannot be null");

		TranslationService.create(TranslationKey.of(messageKey), player).withPrefix().send();
	}
}