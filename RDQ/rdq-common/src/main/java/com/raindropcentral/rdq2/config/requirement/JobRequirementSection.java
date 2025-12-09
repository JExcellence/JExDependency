package com.raindropcentral.rdq2.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for job-based requirements.
 * <p>
 * Provides accessors for multiple legacy and modern field names so that
 * configuration files created for different RDQ releases continue to resolve
 * correctly. The section also exposes sensible defaults for optional
 * properties, allowing downstream requirement handlers to treat missing values
 * consistently.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class JobRequirementSection extends AConfigSection {
	
	// ~~~ JOB-SPECIFIC PROPERTIES ~~~
	
        /**
         * Single required job name.
         * <p>
         * YAML key: {@code requiredJob}.
         * </p>
         */
	private String requiredJob;
	
        /**
         * Alternative job field name used by legacy configurations.
         * <p>
         * YAML key: {@code job}.
         * </p>
         */
	private String job;
	
        /**
         * Required job level for the single job slot.
         * <p>
         * YAML key: {@code requiredJobLevel}.
         * </p>
         */
	private Integer requiredJobLevel;
	
        /**
         * Alternative job level field name recognised for backward compatibility.
         * <p>
         * YAML key: {@code jobLevel}.
         * </p>
         */
	private Integer jobLevel;
	
        /**
         * Map of required jobs with their corresponding minimum levels.
         * <p>
         * YAML key: {@code requiredJobs}.
         * </p>
         */
	private Map<String, Integer> requiredJobs;
	
        /**
         * Alternative jobs map field name that mirrors historical configuration.
         * <p>
         * YAML key: {@code jobs}.
         * </p>
         */
	private Map<String, Integer> jobs;
	
        /**
         * Job plugin identifier (for example {@code jobs} or {@code jobsreborn}).
         * <p>
         * YAML key: {@code jobPlugin}.
         * </p>
         */
	private String jobPlugin;
	
        /**
         * Whether this requirement should consume job levels when completed.
         * <p>
         * YAML key: {@code consumeOnComplete}.
         * </p>
         */
	private Boolean consumeOnComplete;
	
        /**
         * Whether all jobs must be at the required level (logical AND) or just
         * one needs to match (logical OR).
         * <p>
         * YAML key: {@code requireAll}.
         * </p>
         */
	private Boolean requireAll;
	
        /**
         * Constructs a new {@link JobRequirementSection}.
         *
         * @param evaluationEnvironmentBuilder the evaluation environment builder
         *                                      used to resolve dynamic values
         */    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected JobRequirementSection() {
        super(new EvaluationEnvironmentBuilder());
    }

	public JobRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
		super(evaluationEnvironmentBuilder);
	}
	
	// ~~~ GETTERS ~~~
	
        /**
         * Gets the single required job, trying multiple field names.
         *
         * @return the resolved required job name or an empty string when none is
         *         provided
         */
	public String getRequiredJob() {
		if (this.requiredJob != null) {
			return this.requiredJob;
		}
		if (this.job != null) {
			return this.job;
		}
		return "";
	}
	
        /**
         * Gets the required job level, trying multiple field names.
         *
         * @return the resolved required job level, defaulting to {@code 1} when
         *         unspecified
         */
	public Integer getRequiredJobLevel() {
		if (this.requiredJobLevel != null) {
			return this.requiredJobLevel;
		}
		if (this.jobLevel != null) {
			return this.jobLevel;
		}
		return 1;
	}
	
        /**
         * Gets the complete map of required jobs from all sources.
         *
         * @return a mutable map combining single job entries and map-based
         *         definitions
         */
	public Map<String, Integer> getRequiredJobs() {
		Map<String, Integer> jobMap = new HashMap<>();
		
		// Add jobs from requiredJobs map
		if (this.requiredJobs != null) {
			jobMap.putAll(this.requiredJobs);
		}
		
		// Add jobs from alternative jobs map
		if (this.jobs != null) {
			jobMap.putAll(this.jobs);
		}
		
		// Add single job if specified
		String singleJob = getRequiredJob();
		if (!singleJob.isEmpty()) {
			jobMap.put(singleJob, getRequiredJobLevel());
		}
		
		return jobMap;
	}
	
        /**
         * Gets the job plugin identifier used to satisfy this requirement.
         *
         * @return the configured job plugin identifier or {@code jobs} when a
         *         specific plugin is not supplied
         */
	public String getJobPlugin() {
		return this.jobPlugin != null ? this.jobPlugin : "jobs";
	}
	
        /**
         * Gets whether job levels should be consumed on completion.
         *
         * @return {@code true} if job levels should be consumed, otherwise the
         *         value defaults to {@code false}
         */
	public Boolean getConsumeOnComplete() {
		return this.consumeOnComplete != null ? this.consumeOnComplete : false;
	}
	
        /**
         * Gets whether all jobs must be at the required level.
         *
         * @return {@code true} if every configured job must meet the level
         *         requirement; defaults to {@code true}
         */
	public Boolean getRequireAll() {
		return this.requireAll != null ? this.requireAll : true;
	}
}