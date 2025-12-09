package com.raindropcentral.rdq.utility.requirement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.requirement.*;
import com.raindropcentral.rdq.database.entity.RRequirement;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.json.requirement.RequirementParser;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.configmapper.sections.AConfigSection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for converting BaseRequirementSection configurations into concrete AbstractRequirement implementations.
 * <p>
 * This factory handles the creation of specific requirement types based on the requirement section type,
 * then uses the existing RequirementParser infrastructure for serialization and storage.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RequirementFactory {
	
	private final Logger       LOGGER        = CentralLogger.getLogger(RequirementFactory.class);
	private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	private final RDQ rdq;
	
	public RequirementFactory(
		final @NotNull RDQ rdq
	) {
		
		this.rdq = rdq;
	}
	
	/**
	 * Parses requirements from configuration and returns a list of RRankUpgradeRequirement entities.
	 * This method does NOT add requirements to the rank - that's handled by the caller.
	 *
	 * @param rank         The rank these requirements belong to
	 * @param requirements The map of requirement configurations
	 *
	 * @return List of RRankUpgradeRequirement entities (not yet persisted)
	 */
	public List<RRankUpgradeRequirement> parseRequirements(
		final @NotNull RRank rank,
		final @NotNull Map<String, ? extends BaseRequirementSection> requirements
	) {
		
		List<RRankUpgradeRequirement> upgradeRequirements = new ArrayList<>();
		
		if (requirements.isEmpty()) {
			LOGGER.info("No requirements configured for rank: " + rank.getIdentifier());
			return upgradeRequirements;
		}
		
		AtomicInteger displayOrder = new AtomicInteger(0);
		
		requirements.forEach((requirementKey, baseRequirementSection) -> {
			if (baseRequirementSection == null) {
				LOGGER.warning("Null requirement section found for key: " + requirementKey);
				return;
			}
			
			AConfigSection specificRequirement = getSpecificRequirementSection(baseRequirementSection);
			if (specificRequirement == null) {
				LOGGER.warning("No valid requirement section found for key: " + requirementKey + " in rank: " + rank.getIdentifier());
				return;
			}
			
			try {
				if (isInvalidRequirement(specificRequirement)) {
					LOGGER.info("Skipping invalid requirement '" + requirementKey + "' (type: " + specificRequirement.getClass().getSimpleName() + ") for rank '" + rank.getIdentifier() + "' - missing required data");
					return;
				}
				
				AbstractRequirement abstractRequirement = createRequirementFromSection(specificRequirement);
				
				RRequirement rRequirement = new RRequirement(
					abstractRequirement,
					baseRequirementSection.getIcon()
				);
				
				RRankUpgradeRequirement upgradeRequirement = new RRankUpgradeRequirement(
					rank,
					rRequirement,
					rRequirement.getShowcase()
				);
				
				Integer configuredOrder = baseRequirementSection.getDisplayOrder();
				if (configuredOrder != null && configuredOrder > 0) {
					upgradeRequirement.setDisplayOrder(configuredOrder);
				} else {
					upgradeRequirement.setDisplayOrder(displayOrder.getAndIncrement());
				}
				
				upgradeRequirements.add(upgradeRequirement);
				
				LOGGER.info("Successfully parsed requirement '" + requirementKey + "' (type: " + specificRequirement.getClass().getSimpleName() + ") for rank '" + rank.getIdentifier() + "'");
			} catch (final Exception exception) {
				LOGGER.log(
					Level.SEVERE,
					"Failed to parse requirement '" + requirementKey + "' (type: " + specificRequirement.getClass().getSimpleName() + ") for rank '" + rank.getIdentifier() + "'",
					exception
				);
			}
		});
		
		return upgradeRequirements;
	}
	
	/**
	 * Parses and assigns upgrade requirements from configuration to a rank.
	 * This method directly adds requirements to the rank and handles persistence.
	 *
	 * @param rank         The rank to assign requirements to
	 * @param requirements The map of requirement configurations
	 */
	public void parseAndAssignRequirements(
		RRank rank,
		Map<String, ? extends BaseRequirementSection> requirements
	) {
		
		if (requirements == null || requirements.isEmpty()) {
			LOGGER.info("No requirements configured for rank: " + rank.getIdentifier());
			return;
		}
		
		AtomicInteger displayOrder = new AtomicInteger(0);
		
		requirements.forEach((requirementKey, baseRequirementSection) -> {
			if (baseRequirementSection == null) {
				LOGGER.warning("Null requirement section found for key: " + requirementKey);
				return;
			}
			
			LOGGER.info("Processing requirement: " + requirementKey + " for rank: " + rank.getIdentifier());
			LOGGER.info("BaseRequirementSection type: " + baseRequirementSection.getType());
			
			AConfigSection specificRequirement = getSpecificRequirementSection(baseRequirementSection);
			if (specificRequirement == null) {
				LOGGER.warning("No valid requirement section found for key: " + requirementKey + " in rank: " + rank.getIdentifier());
				return;
			}
			
			LOGGER.info("Found specific requirement of type: " + specificRequirement.getClass().getSimpleName());
			
			try {
				if (isInvalidRequirement(specificRequirement)) {
					LOGGER.warning("Skipping invalid requirement '" + requirementKey + "' (type: " + specificRequirement.getClass().getSimpleName() + ") for rank '" + rank.getIdentifier() + "' - missing required data");
					return;
				}
				
				AbstractRequirement abstractRequirement = createRequirementFromSection(specificRequirement);
				
				RRequirement rRequirement = new RRequirement(
					abstractRequirement,
					baseRequirementSection.getIcon()
				);
				
				try {
					rRequirement = this.rdq.getRequirementRepository().createAsync(rRequirement).join();
					LOGGER.info("Successfully persisted RRequirement with ID: " + rRequirement.getId() + " for requirement: " + requirementKey);
				} catch (Exception e) {
					LOGGER.log(
						Level.SEVERE,
						"Failed to persist RRequirement for requirement '" + requirementKey + "'",
						e
					);
					return;
				}
				
				RRankUpgradeRequirement upgradeRequirement = new RRankUpgradeRequirement(
					rank,
					rRequirement,
					rRequirement.getShowcase()
				);
				
				Integer configuredOrder = baseRequirementSection.getDisplayOrder();
				if (configuredOrder != null && configuredOrder > 0) {
					upgradeRequirement.setDisplayOrder(configuredOrder);
				} else {
					upgradeRequirement.setDisplayOrder(displayOrder.getAndIncrement());
				}
				
				rank.addUpgradeRequirement(upgradeRequirement);
				
				LOGGER.info("Successfully parsed and added requirement '" + requirementKey + "' (type: " + specificRequirement.getClass().getSimpleName() + ") to rank '" + rank.getIdentifier() + "'");
			} catch (final Exception exception) {
				LOGGER.log(
					Level.SEVERE,
					"Failed to parse requirement '" + requirementKey + "' (type: " + specificRequirement.getClass().getSimpleName() + ") for rank '" + rank.getIdentifier() + "'",
					exception
				);
			}
		});
		
		LOGGER.info("Finished processing requirements for rank: " + rank.getIdentifier() + ". Total requirements added: " + rank.getUpgradeRequirements().size());
	}
	
	/**
	 * Gets the specific requirement section from a BaseRequirementSection based on its type.
	 * This method determines which specific requirement type is configured and returns it.
	 *
	 * @param baseRequirementSection The base requirement section
	 *
	 * @return The specific requirement section, or null if none found
	 */
	private AConfigSection getSpecificRequirementSection(BaseRequirementSection baseRequirementSection) {
		
		String type = baseRequirementSection.getType();
		
		if (type == null || type.equals("not_defined")) {
			if (baseRequirementSection.getItemRequirement() != null &&
			    ! baseRequirementSection.getItemRequirement().getRequiredItemsList().isEmpty()) {
				return baseRequirementSection.getItemRequirement();
			}
			if (baseRequirementSection.getCurrencyRequirement() != null &&
			    baseRequirementSection.getCurrencyRequirement().getRequiredCurrencies() != null &&
			    ! baseRequirementSection.getCurrencyRequirement().getRequiredCurrencies().isEmpty()) {
				return baseRequirementSection.getCurrencyRequirement();
			}
			if (baseRequirementSection.getExperienceRequirement() != null &&
			    baseRequirementSection.getExperienceRequirement().getRequiredLevel() > 0) {
				return baseRequirementSection.getExperienceRequirement();
			}
			if (baseRequirementSection.getPlaytimeRequirement() != null &&
			    baseRequirementSection.getPlaytimeRequirement().getRequiredPlaytimeSeconds() > 0) {
				return baseRequirementSection.getPlaytimeRequirement();
			}
			if (baseRequirementSection.getPermissionRequirement() != null &&
			    baseRequirementSection.getPermissionRequirement().getRequiredPermissions() != null &&
			    ! baseRequirementSection.getPermissionRequirement().getRequiredPermissions().isEmpty()) {
				return baseRequirementSection.getPermissionRequirement();
			}
			if (baseRequirementSection.getLocationRequirement() != null) {
				return baseRequirementSection.getLocationRequirement();
			}
			if (baseRequirementSection.getCompositeRequirement() != null &&
			    baseRequirementSection.getCompositeRequirement().getCompositeRequirements() != null &&
			    ! baseRequirementSection.getCompositeRequirement().getCompositeRequirements().isEmpty()) {
				return baseRequirementSection.getCompositeRequirement();
			}
			if (baseRequirementSection.getChoiceRequirement() != null &&
			    baseRequirementSection.getChoiceRequirement().getChoices() != null &&
			    ! baseRequirementSection.getChoiceRequirement().getChoices().isEmpty()) {
				return baseRequirementSection.getChoiceRequirement();
			}
			if (baseRequirementSection.getAchievementRequirement() != null &&
			    baseRequirementSection.getAchievementRequirement().getRequiredAchievements() != null &&
			    ! baseRequirementSection.getAchievementRequirement().getRequiredAchievements().isEmpty()) {
				return baseRequirementSection.getAchievementRequirement();
			}
			if (baseRequirementSection.getSkillRequirement() != null &&
			    baseRequirementSection.getSkillRequirement().getRequiredSkills() != null &&
			    ! baseRequirementSection.getSkillRequirement().getRequiredSkills().isEmpty()) {
				return baseRequirementSection.getSkillRequirement();
			}
			if (baseRequirementSection.getJobRequirement() != null &&
			    baseRequirementSection.getJobRequirement().getRequiredJobs() != null &&
			    ! baseRequirementSection.getJobRequirement().getRequiredJobs().isEmpty()) {
				return baseRequirementSection.getJobRequirement();
			}
			if (baseRequirementSection.getTimeBasedRequirement() != null &&
			    baseRequirementSection.getTimeBasedRequirement().getTimeConstraintSeconds() > 0) {
				return baseRequirementSection.getTimeBasedRequirement();
			}
		} else {
			return switch (type.toUpperCase()) {
				case "ITEM" -> baseRequirementSection.getItemRequirement();
				case "CURRENCY" -> baseRequirementSection.getCurrencyRequirement();
				case "EXPERIENCE_LEVEL" -> baseRequirementSection.getExperienceRequirement();
				case "PLAYTIME" -> baseRequirementSection.getPlaytimeRequirement();
				case "TIME_BASED" -> baseRequirementSection.getTimeBasedRequirement();
				case "PERMISSION" -> baseRequirementSection.getPermissionRequirement();
				case "LOCATION" -> baseRequirementSection.getLocationRequirement();
				case "COMPOSITE" -> baseRequirementSection.getCompositeRequirement();
				case "CHOICE" -> baseRequirementSection.getChoiceRequirement();
				case "ACHIEVEMENT",
				     "CUSTOM" -> baseRequirementSection.getAchievementRequirement();
				case "SKILLS" -> baseRequirementSection.getSkillRequirement();
				case "JOBS" -> baseRequirementSection.getJobRequirement();
				default -> null;
			};
		}
		
		return null;
	}
	
	/**
	 * Validates if a requirement section has the minimum required data to create a valid requirement.
	 *
	 * @param requirementSection The requirement section to validate
	 *
	 * @return true if the requirement has sufficient data, false otherwise
	 */
	private boolean isInvalidRequirement(
		final @NotNull AConfigSection requirementSection
	) {
		
		return ! switch (requirementSection) {
			case ItemRequirementSection itemSection -> itemSection.getRequiredItemsList() != null && ! itemSection.getRequiredItemsList().isEmpty();
			
			case CurrencyRequirementSection currencySection ->
				currencySection.getRequiredCurrencies() != null && ! currencySection.getRequiredCurrencies().isEmpty();
			
			case ExperienceLevelRequirementSection expSection -> expSection.getRequiredLevel() > 0;
			
			case PlaytimeRequirementSection playtimeSection -> playtimeSection.getRequiredPlaytimeSeconds() > 0 ||
			                                                   (playtimeSection.getTime() != null && playtimeSection.getTime() > 0);
			
			case PermissionRequirementSection permissionSection ->
				permissionSection.getRequiredPermissions() != null && ! permissionSection.getRequiredPermissions().isEmpty();
			
			case LocationRequirementSection locationSection ->
				(locationSection.getRequiredWorld() != null && ! locationSection.getRequiredWorld().trim().isEmpty()) ||
				(locationSection.getRequiredRegion() != null && ! locationSection.getRequiredRegion().trim().isEmpty()) ||
				(locationSection.getRequiredCoordinates() != null && ! locationSection.getRequiredCoordinates().isEmpty()) ||
				locationSection.getRequiredDistance() > 0;
			
			case CompositeRequirementSection compositeSection ->
				compositeSection.getCompositeRequirements() != null && ! compositeSection.getCompositeRequirements().isEmpty();
			
			case ChoiceRequirementSection choiceSection -> choiceSection.getChoices() != null && ! choiceSection.getChoices().isEmpty();
			
			case AchievementRequirementSection achievementSection ->
				achievementSection.getRequiredAchievements() != null && ! achievementSection.getRequiredAchievements().isEmpty();
			
			case SkillRequirementSection skillSection -> skillSection.getRequiredSkills() != null && ! skillSection.getRequiredSkills().isEmpty();
			
			case JobRequirementSection jobSection -> jobSection.getRequiredJobs() != null && ! jobSection.getRequiredJobs().isEmpty();
			
			case TimeBasedRequirementSection timeSection -> timeSection.getTimeConstraintSeconds() > 0 ||
			                                                (timeSection.getStartTime() != null && timeSection.getEndTime() != null) ||
			                                                (timeSection.getActiveDays() != null && ! timeSection.getActiveDays().isEmpty()) ||
			                                                (timeSection.getActiveDates() != null && ! timeSection.getActiveDates().isEmpty());
			
			default -> {
				LOGGER.warning("Unknown requirement section type for validation: " + requirementSection.getClass().getSimpleName());
				yield false;
			}
		};
	}
	
	/**
	 * Creates an AbstractRequirement from an individual requirement section.
	 *
	 * @param requirementSection The individual requirement section
	 *
	 * @return The created AbstractRequirement
	 *
	 * @throws IOException If creation fails
	 */
	public AbstractRequirement createRequirementFromSection(AConfigSection requirementSection) throws IOException {
		
		String requirementJson = convertSectionToJson(requirementSection);
		
		LOGGER.info("Generated JSON for requirement: " + requirementJson);
		
		return RequirementParser.parse(requirementJson);
	}
	
	/**
	 * Converts an individual requirement section to JSON string for parsing.
	 *
	 * @param requirementSection The requirement section to convert
	 *
	 * @return JSON string representation
	 *
	 * @throws IOException If conversion fails
	 */
	private String convertSectionToJson(
		final @NotNull AConfigSection requirementSection
	) throws IOException {
		
		Map<String, Object> jsonMap = new HashMap<>();
		
		String type = determineRequirementType(requirementSection);
		jsonMap.put(
			"type",
			type
		);
		
		switch (requirementSection) {
			case ItemRequirementSection itemSection -> addItemProperties(
				jsonMap,
				itemSection
			);
			case CurrencyRequirementSection currencySection -> addCurrencyProperties(
				jsonMap,
				currencySection
			);
			case ExperienceLevelRequirementSection expSection -> addExperienceProperties(
				jsonMap,
				expSection
			);
			case PlaytimeRequirementSection playtimeSection -> addPlaytimeProperties(
				jsonMap,
				playtimeSection
			);
			case PermissionRequirementSection permissionSection -> addPermissionProperties(
				jsonMap,
				permissionSection
			);
			case LocationRequirementSection locationSection -> addLocationProperties(
				jsonMap,
				locationSection
			);
			case CompositeRequirementSection compositeSection -> addCompositeProperties(
				jsonMap,
				compositeSection
			);
			case ChoiceRequirementSection choiceSection -> addChoiceProperties(
				jsonMap,
				choiceSection
			);
			case AchievementRequirementSection achievementSection -> addAchievementProperties(
				jsonMap,
				achievementSection
			);
			case SkillRequirementSection skillSection -> addSkillProperties(
				jsonMap,
				skillSection
			);
			case JobRequirementSection jobSection -> addJobProperties(
				jsonMap,
				jobSection
			);
			case TimeBasedRequirementSection timeSection -> addTimeBasedProperties(
				jsonMap,
				timeSection
			);
			default -> LOGGER.warning("Unknown requirement section type: " + requirementSection.getClass().getSimpleName());
		}
		
		return OBJECT_MAPPER.writeValueAsString(jsonMap);
	}
	
	/**
	 * Determines the requirement type based on the section class.
	 */
	private String determineRequirementType(AConfigSection requirementSection) {
		
		return switch (requirementSection) {
			case ItemRequirementSection ignored -> "ITEM";
			case CurrencyRequirementSection ignored -> "CURRENCY";
			case ExperienceLevelRequirementSection ignored -> "EXPERIENCE_LEVEL";
			case PlaytimeRequirementSection ignored -> "PLAYTIME";
			case PermissionRequirementSection ignored -> "PERMISSION";
			case LocationRequirementSection ignored -> "LOCATION";
			case CompositeRequirementSection ignored -> "COMPOSITE";
			case ChoiceRequirementSection ignored -> "CHOICE";
			case AchievementRequirementSection ignored -> "CUSTOM";
			case SkillRequirementSection ignored -> "SKILLS";
			case JobRequirementSection ignored -> "JOBS";
			case TimeBasedRequirementSection ignored -> "TIME_BASED";
			default -> "UNKNOWN";
		};
	}
	
	/**
	 * Adds item-specific properties to the JSON map.
	 */
	private void addItemProperties(
		final @NotNull Map<String, Object> jsonMap,
		final @NotNull ItemRequirementSection section
	) {
		
		jsonMap.put(
			"consumeOnComplete",
			section.getConsumeOnComplete()
		);
		jsonMap.put(
			"exactMatch",
			false
		);
		jsonMap.put(
			"description",
			"Item requirement"
		);
		
		if (section.getRequiredItemsList() != null && ! section.getRequiredItemsList().isEmpty()) {
			List<Map<String, Object>> itemStackList = new ArrayList<>();
			
			for (ItemStack itemStack : section.getRequiredItemsList()) {
				Map<String, Object> itemMap = new HashMap<>();
				itemMap.put(
					"type",
					itemStack.getType().name()
				);
				itemMap.put(
					"amount",
					itemStack.getAmount()
				);
				
				if (itemStack.hasItemMeta()) {
					Map<String, Object> metaMap = new HashMap<>();
					if (itemStack.getItemMeta().hasDisplayName()) {
						metaMap.put(
							"displayName",
							itemStack.getItemMeta().getDisplayName()
						);
					}
					if (itemStack.getItemMeta().hasLore()) {
						metaMap.put(
							"lore",
							itemStack.getItemMeta().getLore()
						);
					}
					if (! metaMap.isEmpty()) {
						itemMap.put(
							"meta",
							metaMap
						);
					}
				}
				
				itemStackList.add(itemMap);
			}
			
			jsonMap.put(
				"requiredItems",
				itemStackList
			);
			jsonMap.put(
				"itemBuilders",
				new ArrayList<>()
			);
			
			LOGGER.info("Added " + itemStackList.size() + " items to requirement JSON:");
			for (int i = 0; i < itemStackList.size(); i++) {
				Map<String, Object> item = itemStackList.get(i);
				LOGGER.info("  Item " + i + ": " + item.get("type") + " x" + item.get("amount"));
			}
		}
	}
	
	/**
	 * Adds currency-specific properties to the JSON map.
	 */
	private void addCurrencyProperties(
		Map<String, Object> jsonMap,
		CurrencyRequirementSection section
	) {
		
		jsonMap.put(
			"consumeOnComplete",
			section.getConsumeOnComplete()
		);
		
		String currencyPlugin = section.getCurrencyPlugin();
		if (currencyPlugin == null || currencyPlugin.trim().isEmpty()) {
			currencyPlugin = "vault";
		}
		jsonMap.put(
			"currencyPlugin",
			currencyPlugin
		);
		
		Map<String, Double> requiredCurrencies = section.getRequiredCurrencies();
		if (requiredCurrencies != null && ! requiredCurrencies.isEmpty()) {
			jsonMap.put(
				"requiredCurrencies",
				requiredCurrencies
			);
		} else {
			Map<String, Double> defaultCurrency = new HashMap<>();
			defaultCurrency.put(
				"default",
				100.0
			);
			jsonMap.put(
				"requiredCurrencies",
				defaultCurrency
			);
		}
	}
	
	/**
	 * Adds experience-specific properties to the JSON map.
	 */
	private void addExperienceProperties(
		Map<String, Object> jsonMap,
		ExperienceLevelRequirementSection section
	) {
		
		jsonMap.put(
			"consumeOnComplete",
			section.getConsumeOnComplete()
		);
		jsonMap.put(
			"requiredLevel",
			Math.max(
				1,
				section.getRequiredLevel()
			)
		);
		
		String experienceType = section.getExperienceType();
		if (experienceType == null || experienceType.trim().isEmpty()) {
			experienceType = "LEVEL";
		}
		jsonMap.put(
			"experienceType",
			experienceType
		);
		
		if (section.getDescription() != null && ! section.getDescription().trim().isEmpty()) {
			jsonMap.put(
				"description",
				section.getDescription()
			);
		}
	}
	
	/**
	 * Adds playtime-specific properties to the JSON map.
	 */
	private void addPlaytimeProperties(
		Map<String, Object> jsonMap,
		PlaytimeRequirementSection section
	) {
		
		long requiredPlaytimeSeconds = section.getRequiredPlaytimeSeconds();
		
		if (requiredPlaytimeSeconds <= 0) {
			requiredPlaytimeSeconds = 3600;
			LOGGER.warning("No valid playtime requirement found, defaulting to 1 hour (3600 seconds)");
		}
		
		jsonMap.put("requiredPlaytimeSeconds", requiredPlaytimeSeconds);
		
		Boolean useTotalPlaytime = section.getUseTotalPlaytime();
		if (useTotalPlaytime == null) {
			useTotalPlaytime = !section.hasWorldSpecificConfiguration();
		}
		jsonMap.put("useTotalPlaytime", useTotalPlaytime);
		
		Map<String, Long> worldPlaytimeRequirements = section.getWorldPlaytimeRequirements();
		if (!worldPlaytimeRequirements.isEmpty()) {
			jsonMap.put("worldPlaytimeRequirements", worldPlaytimeRequirements);
			
			LOGGER.info("Added world-specific playtime requirements:");
			for (Map.Entry<String, Long> entry : worldPlaytimeRequirements.entrySet()) {
				long hours = entry.getValue() / 3600;
				long minutes = (entry.getValue() % 3600) / 60;
				long seconds = entry.getValue() % 60;
				LOGGER.info("  " + entry.getKey() + ": " +
				            (hours > 0 ? hours + "h " : "") +
				            (minutes > 0 ? minutes + "m " : "") +
				            (seconds > 0 ? seconds + "s" : ""));
			}
		} else {
			jsonMap.put("worldPlaytimeRequirements", new HashMap<String, Long>());
		}
		
		String description = section.getDescription();
		if (description != null && !description.trim().isEmpty()) {
			jsonMap.put("description", description);
		} else {
			if (useTotalPlaytime) {
				long hours = requiredPlaytimeSeconds / 3600;
				long minutes = (requiredPlaytimeSeconds % 3600) / 60;
				String timeDesc = "";
				if (hours > 0) {
					timeDesc += hours + " hour" + (hours > 1 ? "s" : "");
					if (minutes > 0) {
						timeDesc += " and " + minutes + " minute" + (minutes > 1 ? "s" : "");
					}
				} else if (minutes > 0) {
					timeDesc = minutes + " minute" + (minutes > 1 ? "s" : "");
				} else {
					timeDesc = requiredPlaytimeSeconds + " second" + (requiredPlaytimeSeconds > 1 ? "s" : "");
				}
				description = "Play for a total of " + timeDesc;
			} else {
				description = "Meet world-specific playtime requirements";
			}
			jsonMap.put("description", description);
		}
		
		if (useTotalPlaytime) {
			long hours = requiredPlaytimeSeconds / 3600;
			long minutes = (requiredPlaytimeSeconds % 3600) / 60;
			LOGGER.info("Configured total playtime requirement: " +
			            (hours > 0 ? hours + "h " : "") +
			            (minutes > 0 ? minutes + "m " : "") +
			            (requiredPlaytimeSeconds % 60 > 0 ? (requiredPlaytimeSeconds % 60) + "s" : ""));
		} else {
			LOGGER.info("Configured world-specific playtime requirements for " +
			            worldPlaytimeRequirements.size() + " world(s)");
		}
	}
	
	/**
	 * Adds permission-specific properties to the JSON map.
	 */
	private void addPermissionProperties(
		Map<String, Object> jsonMap,
		PermissionRequirementSection section
	) {
		
		jsonMap.put(
			"requireAll",
			section.getRequireAll()
		);
		jsonMap.put(
			"checkNegation",
			section.getCheckNegation()
		);
		
		List<String> requiredPermissions = section.getRequiredPermissions();
		if (requiredPermissions != null && ! requiredPermissions.isEmpty()) {
			jsonMap.put(
				"requiredPermissions",
				requiredPermissions
			);
		} else {
			jsonMap.put(
				"requiredPermissions",
				List.of("default.permission")
			);
		}
	}
	
	/**
	 * Adds location-specific properties to the JSON map.
	 */
	private void addLocationProperties(
		Map<String, Object> jsonMap,
		LocationRequirementSection section
	) {
		
		jsonMap.put(
			"exactLocation",
			section.getExactLocation()
		);
		
		if (section.getRequiredWorld() != null && ! section.getRequiredWorld().trim().isEmpty()) {
			jsonMap.put(
				"requiredWorld",
				section.getRequiredWorld()
			);
		}
		
		if (section.getRequiredRegion() != null && ! section.getRequiredRegion().trim().isEmpty()) {
			jsonMap.put(
				"requiredRegion",
				section.getRequiredRegion()
			);
		}
		
		if (section.getRequiredCoordinates() != null && ! section.getRequiredCoordinates().isEmpty()) {
			jsonMap.put(
				"requiredCoordinates",
				section.getRequiredCoordinates()
			);
		}
		
		if (section.getRequiredDistance() > 0) {
			jsonMap.put(
				"requiredDistance",
				section.getRequiredDistance()
			);
		}
		
		if ((section.getRequiredWorld() == null || section.getRequiredWorld().trim().isEmpty()) &&
		    (section.getRequiredRegion() == null || section.getRequiredRegion().trim().isEmpty()) &&
		    (section.getRequiredCoordinates() == null || section.getRequiredCoordinates().isEmpty()) &&
		    section.getRequiredDistance() <= 0) {
			jsonMap.put(
				"requiredWorld",
				"world"
			);
		}
	}
	
	/**
	 * Adds composite-specific properties to the JSON map.
	 */
	private void addCompositeProperties(
		Map<String, Object> jsonMap,
		CompositeRequirementSection section
	) throws IOException {
		
		jsonMap.put(
			"operator",
			section.getOperator()
		);
		jsonMap.put(
			"minimumRequired",
			Math.max(
				0,
				section.getMinimumRequired()
			)
		);
		jsonMap.put(
			"maximumRequired",
			Math.max(
				0,
				section.getMaximumRequired()
			)
		);
		jsonMap.put(
			"allowPartialProgress",
			section.getAllowPartialProgress()
		);
		
		if (section.getDescription() != null && ! section.getDescription().trim().isEmpty()) {
			jsonMap.put(
				"description",
				section.getDescription()
			);
		}
		
		List<BaseRequirementSection> compositeRequirements = section.getCompositeRequirements();
		if (compositeRequirements != null && ! compositeRequirements.isEmpty()) {
			List<Map<String, Object>> subRequirements = new ArrayList<>();
			for (BaseRequirementSection subReq : compositeRequirements) {
				String subJson = convertSectionToJson(subReq);
				@SuppressWarnings("unchecked")
				Map<String, Object> subMap = OBJECT_MAPPER.readValue(
					subJson,
					Map.class
				);
				subRequirements.add(subMap);
			}
			jsonMap.put(
				"requirements",
				subRequirements
			);
		} else {
			jsonMap.put(
				"requirements",
				new ArrayList<>()
			);
		}
	}
	
	/**
	 * Adds choice-specific properties to the JSON map.
	 */
	private void addChoiceProperties(
		Map<String, Object> jsonMap,
		ChoiceRequirementSection section
	) throws IOException {
		
		jsonMap.put(
			"minimumRequired",
			Math.max(
				1,
				section.getMinimumRequired()
			)
		);
		jsonMap.put(
			"maximumRequired",
			Math.max(
				0,
				section.getMaximumRequired()
			)
		);
		jsonMap.put(
			"allowPartialProgress",
			section.getAllowPartialProgress()
		);
		jsonMap.put(
			"mutuallyExclusive",
			section.getMutuallyExclusive()
		);
		jsonMap.put(
			"allowChoiceChange",
			section.getAllowChoiceChange()
		);
		
		if (section.getDescription() != null && ! section.getDescription().trim().isEmpty()) {
			jsonMap.put(
				"description",
				section.getDescription()
			);
		}
		
		List<BaseRequirementSection> choices = section.getChoices();
		if (choices != null && ! choices.isEmpty()) {
			List<Map<String, Object>> choicesList = new ArrayList<>();
			for (BaseRequirementSection choice : choices) {
				String choiceJson = convertSectionToJson(choice);
				@SuppressWarnings("unchecked")
				Map<String, Object> choiceMap = OBJECT_MAPPER.readValue(
					choiceJson,
					Map.class
				);
				choicesList.add(choiceMap);
			}
			jsonMap.put(
				"choices",
				choicesList
			);
		} else {
			jsonMap.put(
				"choices",
				new ArrayList<>()
			);
		}
	}
	
	/**
	 * Adds achievement-specific properties to the JSON map.
	 */
	private void addAchievementProperties(
		Map<String, Object> jsonMap,
		AchievementRequirementSection section
	) {
		
		jsonMap.put(
			"requireAll",
			section.getRequireAll()
		);
		
		String achievementPlugin = section.getAchievementPlugin();
		if (achievementPlugin == null || achievementPlugin.trim().isEmpty()) {
			achievementPlugin = "advancedachievements";
		}
		jsonMap.put(
			"achievementPlugin",
			achievementPlugin
		);
		
		List<String> requiredAchievements = section.getRequiredAchievements();
		if (requiredAchievements != null && ! requiredAchievements.isEmpty()) {
			jsonMap.put(
				"requiredAchievements",
				requiredAchievements
			);
		} else {
			jsonMap.put(
				"requiredAchievements",
				List.of("default_achievement")
			);
		}
	}
	
	/**
	 * Adds skill-specific properties to the JSON map.
	 */
	private void addSkillProperties(
		Map<String, Object> jsonMap,
		SkillRequirementSection section
	) {
		
		jsonMap.put(
			"consumeOnComplete",
			section.getConsumeOnComplete()
		);
		
		String skillPlugin = section.getSkillPlugin();
		if (skillPlugin != null) {
			skillPlugin = skillPlugin.toUpperCase();
			switch (skillPlugin) {
				case "MCMMO" -> skillPlugin = "MCMMO";
				case "ECO_SKILLS",
				     "ECOSKILLS" -> skillPlugin = "ECO_SKILLS";
				case "AURA_SKILLS",
				     "AURASKILLS" -> skillPlugin = "AURA_SKILLS";
				default -> skillPlugin = "AUTO";
			}
		} else {
			skillPlugin = "AUTO";
		}
		jsonMap.put(
			"skillPlugin",
			skillPlugin
		);
		
		Map<String, Integer> requiredSkills = section.getRequiredSkills();
		if (requiredSkills != null && ! requiredSkills.isEmpty()) {
			jsonMap.put(
				"requiredSkills",
				requiredSkills
			);
		} else {
			Map<String, Integer> defaultSkills = new HashMap<>();
			defaultSkills.put(
				"mining",
				10
			);
			jsonMap.put(
				"requiredSkills",
				defaultSkills
			);
		}
	}
	
	/**
	 * Adds job-specific properties to the JSON map.
	 */
	private void addJobProperties(
		Map<String, Object> jsonMap,
		JobRequirementSection section
	) {
		
		jsonMap.put(
			"consumeOnComplete",
			section.getConsumeOnComplete()
		);
		jsonMap.put(
			"requireAll",
			section.getRequireAll()
		);
		
		String jobPlugin = section.getJobPlugin();
		if (jobPlugin != null) {
			jobPlugin = jobPlugin.toUpperCase();
			switch (jobPlugin) {
				case "JOBS",
				     "JOBS_REBORN",
				     "JOBSREBORN" -> jobPlugin = "JOBS_REBORN";
				case "ECO_JOBS",
				     "ECOJOBS" -> jobPlugin = "ECO_JOBS";
				default -> jobPlugin = "AUTO";
			}
		} else {
			jobPlugin = "AUTO";
		}
		jsonMap.put(
			"jobPlugin",
			jobPlugin
		);
		
		Map<String, Integer> requiredJobs = section.getRequiredJobs();
		if (requiredJobs != null && ! requiredJobs.isEmpty()) {
			jsonMap.put(
				"requiredJobs",
				requiredJobs
			);
		} else {
			Map<String, Integer> defaultJobs = new HashMap<>();
			defaultJobs.put(
				"miner",
				10
			);
			jsonMap.put(
				"requiredJobs",
				defaultJobs
			);
		}
	}
	
	/**
	 * Adds time-based-specific properties to the JSON map.
	 */
	private void addTimeBasedProperties(
		Map<String, Object> jsonMap,
		TimeBasedRequirementSection section
	) {
		
		long timeConstraint = Math.max(
			1,
			section.getTimeConstraintSeconds()
		);
		jsonMap.put(
			"timeConstraintSeconds",
			timeConstraint
		);
		jsonMap.put(
			"cooldownSeconds",
			Math.max(
				0,
				section.getCooldownSeconds()
			)
		);
		
		String timeZone = section.getTimeZone();
		if (timeZone == null || timeZone.trim().isEmpty()) {
			timeZone = "UTC";
		}
		jsonMap.put(
			"timeZone",
			timeZone
		);
		
		jsonMap.put(
			"recurring",
			section.getRecurring()
		);
		jsonMap.put(
			"useRealTime",
			section.getUseRealTime()
		);
		
		if (section.getStartTime() != null) {
			jsonMap.put(
				"startTime",
				section.getStartTime()
			);
		}
		
		if (section.getEndTime() != null) {
			jsonMap.put(
				"endTime",
				section.getEndTime()
			);
		}
		
		if (section.getActiveDays() != null && ! section.getActiveDays().isEmpty()) {
			jsonMap.put(
				"activeDays",
				section.getActiveDays()
			);
		}
		
		if (section.getActiveDates() != null && ! section.getActiveDates().isEmpty()) {
			jsonMap.put(
				"activeDates",
				section.getActiveDates()
			);
		}
	}
	
	/**
	 * Determines the showcase material for the requirement.
	 */
	private Material determineShowcaseMaterial(BaseRequirementSection requirementSection) {
		
		try {
			return Material.valueOf(requirementSection.getIcon().getMaterial());
		} catch (final Exception exception) {
			String type = requirementSection.getType();
			if (type != null) {
				return switch (type.toUpperCase()) {
					case "ITEM" -> Material.CHEST;
					case "CURRENCY" -> Material.GOLD_INGOT;
					case "EXPERIENCE_LEVEL" -> Material.EXPERIENCE_BOTTLE;
					case "PERMISSION" -> Material.NAME_TAG;
					case "LOCATION" -> Material.COMPASS;
					case "CUSTOM" -> Material.COMMAND_BLOCK;
					case "COMPOSITE" -> Material.REDSTONE;
					case "CHOICE" -> Material.CROSSBOW;
					case "TIME_BASED" -> Material.CLOCK;
					case "JOBS" -> Material.IRON_PICKAXE;
					case "SKILLS" -> Material.ENCHANTED_BOOK;
					case "ACHIEVEMENT" -> Material.DIAMOND;
					case "PLAYTIME" -> Material.CLOCK;
					case "PREVIOUS_LEVEL" -> Material.LADDER;
					default -> Material.PAPER;
				};
			}
			
			return Material.PAPER;
		}
	}
	
}