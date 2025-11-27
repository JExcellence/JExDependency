package com.raindropcentral.rdq.view.rank;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record RequirementProgressData(
		@NotNull String requirementId,
		@NotNull String requirementType,
		@NotNull String descriptionKey,
		boolean isCompleted,
		double progressPercentage,
		@NotNull RequirementStatus status,
		@NotNull String statusMessage,
		int displayOrder
) {

	public RequirementProgressData {
		Objects.requireNonNull(requirementId, "Requirement ID cannot be null");
		Objects.requireNonNull(requirementType, "Requirement type cannot be null");
		Objects.requireNonNull(descriptionKey, "Description key cannot be null");
		Objects.requireNonNull(status, "Status cannot be null");
		Objects.requireNonNull(statusMessage, "Status message cannot be null");
		progressPercentage = Math.max(0.0, Math.min(1.0, progressPercentage));
	}

	public int getProgressAsPercentage() {
		return (int) Math.round(progressPercentage * 100);
	}

	public boolean hasProgress() {
		return progressPercentage > 0.0;
	}

	public String getFormattedProgress() {
		return getProgressAsPercentage() + "%";
	}

	public boolean canBeCompleted() {
		return status == RequirementStatus.READY_TO_COMPLETE && !isCompleted;
	}
	
	// Compatibility methods for existing code
	public RequirementStatus getStatus() { return status; }
	public double getProgressPercentage() { return progressPercentage * 100; } // Convert to percentage
	public String getRequirementType() { return requirementType; }
}