package com.raindropcentral.rds.items;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = ShopItem.class, name = "ITEM"),
})
@JsonIgnoreProperties(value = {"typeId", "estimatedValue", "descriptionKey"}, allowGetters = true)
public abstract class AbstractItem implements Item {
	
	@Override
	public abstract @NotNull String getTypeId();
	
	@Override
	public abstract @NotNull CompletableFuture<Boolean> grant(@NotNull Player player);
	
	@Override
	public abstract double getEstimatedValue();
	
	@Override
	public @NotNull String getDescriptionKey() {
		return "reward." + getTypeId().toLowerCase() + ".description";
	}
	
	public void validate() {
	}
	
}
