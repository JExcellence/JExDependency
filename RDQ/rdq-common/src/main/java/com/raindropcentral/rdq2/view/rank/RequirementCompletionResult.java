package com.raindropcentral.rdq2.view.rank;

import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record RequirementCompletionResult(
		boolean success,
		@NotNull String messageKey,
		@NotNull RequirementProgressData updatedProgress
) {

	public RequirementCompletionResult {
		Objects.requireNonNull(messageKey, "Message key cannot be null");
		Objects.requireNonNull(updatedProgress, "Updated progress cannot be null");
	}

	public void sendMessage(@NotNull Player player) {
		Objects.requireNonNull(player, "Player cannot be null");
		TranslationService.create(TranslationKey.of(messageKey), player).withPrefix().send();
	}
	
	// Compatibility method
	public boolean isSuccess() { return success; }
}