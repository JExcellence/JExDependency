package com.raindropcentral.rds.items;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface Item {
	
	@NotNull String getTypeId();
	
	@NotNull
	CompletableFuture<Boolean> grant(@NotNull Player player);
	
	double getEstimatedValue();
	
	@JsonIgnore
	@NotNull String getDescriptionKey();
	
}
