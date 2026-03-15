package com.raindropcentral.rdq.view.admin;

import com.raindropcentral.rplatform.api.luckperms.IRank;

/**
 * Represents the IRRank API type.
 */
public record IRRank(
	String id,
	int weight,
	String displayName
) implements IRank {

}
