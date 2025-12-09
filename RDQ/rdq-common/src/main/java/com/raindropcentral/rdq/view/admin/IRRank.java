package com.raindropcentral.rdq.view.admin;

import com.raindropcentral.rplatform.api.luckperms.IRank;

public record IRRank(
	String id,
	String displayName,
	int weight
) implements IRank {

}
