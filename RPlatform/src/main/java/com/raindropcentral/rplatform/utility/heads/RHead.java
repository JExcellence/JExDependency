package com.raindropcentral.rplatform.utility.heads;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class RHead {
  
  private String identifier;
  
  private UUID uuid;
  
  private String texture;
  
  private EHeadFilter filter;
  
  private String translationKey;
  
  private RHead() {
  }
  
  public RHead(
      final @NotNull String identifier,
      final @NotNull String uuid,
      final @NotNull String texture,
      final @NotNull EHeadFilter filter
  ) {
    this.identifier = identifier;
    this.uuid = UUID.fromString(uuid);
    this.texture = texture;
    this.filter = filter;
    this.translationKey = "head." + this.identifier;
  }
  
  public RHead(
      final @NotNull String identifier,
      final @NotNull String uuid,
      final @NotNull String texture
  ) {
    this(
        identifier,
        uuid,
        texture,
        EHeadFilter.INVENTORY
    );
  }
  
  public ItemStack getHead(
      final @NotNull Player player
  ) {
    return
        UnifiedBuilderFactory
            .head()
            .setCustomTexture(
                this.uuid,
                this.texture
            )
            .setName(
                    TranslationService.create(
                            TranslationKey.of(this.translationKey, "name"),
                            player
                    ).build().component()
            )
            .setLore(
                    TranslationService.create(
                            TranslationKey.of(this.translationKey, "lore"),
                            player
                    ).build().splitLines()
            )
            .build();
  }
  
  public String getIdentifier() {
    return this.identifier;
  }
  
  public UUID getUuid() {
    return this.uuid;
  }
  
  public String getTexture() {
    return this.texture;
  }
  
  public EHeadFilter getFilter() {
    return this.filter;
  }
  
  public String getTranslationKey() {
    return this.translationKey;
  }
  
}