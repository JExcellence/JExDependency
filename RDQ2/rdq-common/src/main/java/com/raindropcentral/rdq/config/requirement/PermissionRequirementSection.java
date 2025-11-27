package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section for permission-based requirements.
 * <p>
 * This section handles all configuration options specific to {@link com.raindropcentral.rdq.requirement.PermissionRequirement},
 * including required permissions and permission checking modes used during evaluation.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class PermissionRequirementSection extends AConfigSection {
	
	// ~~~ PERMISSION-SPECIFIC PROPERTIES ~~~
	
	/**
	 * Single required permission.
	 * YAML key: "requiredPermission"
	 */
	private String requiredPermission;
	
	/**
	 * Alternative permission field name.
	 * YAML key: "permission"
	 */
	private String permission;
	
	/**
	 * List of required permissions.
	 * YAML key: "requiredPermissions"
	 */
	private List<String> requiredPermissions;
	
	/**
	 * Alternative permissions list field name.
	 * YAML key: "permissions"
	 */
	private List<String> permissions;
	
	/**
	 * Whether all permissions must be present (AND) or just one (OR).
	 * YAML key: "requireAll"
	 */
	private Boolean requireAll;
	
	/**
	 * Whether to check for permission negation.
	 * YAML key: "checkNegation"
	 */
	private Boolean checkNegation;
	
        /**
         * Constructs a new permission configuration section backed by the provided evaluation builder.
         *
         * @param evaluationEnvironmentBuilder the evaluation environment builder used to wire requirement expressions
         */    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected PermissionRequirementSection() {
        super(new EvaluationEnvironmentBuilder());
    }

        public PermissionRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
                super(evaluationEnvironmentBuilder);
        }

        // ~~~ GETTERS ~~~

        /**
         * Determines whether all configured permissions must be satisfied.
         *
         * @return {@code true} when all permissions are required, {@code false} when any permission suffices
         */
        public Boolean getRequireAll() {
                return this.requireAll != null ? this.requireAll : true;
        }

        /**
         * Indicates whether permission negation checks should be performed.
         *
         * @return {@code true} if negated permissions should be evaluated, {@code false} otherwise
         */
        public Boolean getCheckNegation() {
                return this.checkNegation != null ? this.checkNegation : false;
        }
	
	/**
	 * Gets the single required permission, trying multiple field names.
	 *
	 * @return the required permission
	 */
	public String getRequiredPermission() {
		if (this.requiredPermission != null) {
			return this.requiredPermission;
		}
		if (this.permission != null) {
			return this.permission;
		}
		return "";
	}
	
	/**
	 * Gets the complete list of required permissions from all sources.
	 *
	 * @return combined list of all required permissions
	 */
	public List<String> getRequiredPermissions() {
		List<String> permissionList = new ArrayList<>();
		
		// Add permissions from requiredPermissions list
		if (this.requiredPermissions != null) {
			permissionList.addAll(this.requiredPermissions);
		}
		
		// Add permissions from alternative permissions list
		if (this.permissions != null) {
			permissionList.addAll(this.permissions);
		}
		
		// Add single permission if specified
		String singlePermission = getRequiredPermission();
		if (!singlePermission.isEmpty() && !permissionList.contains(singlePermission)) {
			permissionList.add(singlePermission);
		}
		
		return permissionList;
	}
}