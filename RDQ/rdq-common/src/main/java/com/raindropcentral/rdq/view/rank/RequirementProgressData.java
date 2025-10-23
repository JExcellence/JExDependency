package com.raindropcentral.rdq.view.rank;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class RequirementProgressData {

	private final String requirementId;
	private final String requirementType;
	private final String descriptionKey;
	private final boolean isCompleted;
	private final double progressPercentage;
	private final RequirementStatus status;
	private final String statusMessage;
	private final int displayOrder;

	public RequirementProgressData(
		final @NotNull String requirementId,
		final @NotNull String requirementType,
		final @NotNull String descriptionKey,
		final boolean isCompleted,
		final double progressPercentage,
		final @NotNull RequirementStatus status,
		final @NotNull String statusMessage,
		final int displayOrder
	) {
		this.requirementId = Objects.requireNonNull(requirementId, "Requirement ID cannot be null");
		this.requirementType = Objects.requireNonNull(requirementType, "Requirement type cannot be null");
		this.descriptionKey = Objects.requireNonNull(descriptionKey, "Description key cannot be null");
		this.isCompleted = isCompleted;
		this.progressPercentage = Math.max(0.0, Math.min(1.0, progressPercentage));
		this.status = Objects.requireNonNull(status, "Status cannot be null");
		this.statusMessage = Objects.requireNonNull(statusMessage, "Status message cannot be null");
		this.displayOrder = displayOrder;
	}

	public String getRequirementId() {
		return requirementId;
	}

	public String getRequirementType() {
		return requirementType;
	}

	public String getDescriptionKey() {
		return descriptionKey;
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	public double getProgressPercentage() {
		return progressPercentage;
	}

	public RequirementStatus getStatus() {
		return status;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public int getDisplayOrder() {
		return displayOrder;
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

	@Override
	public String toString() {
		return "RequirementProgressData{" +
			"id='" + requirementId + '\'' +
			", type='" + requirementType + '\'' +
			", completed=" + isCompleted +
			", progress=" + getProgressAsPercentage() + "%" +
			", status=" + status +
			'}';
	}
}