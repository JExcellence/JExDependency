package com.raindropcentral.rplatform.utility.heads;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents the type API type.
 */
@SuppressWarnings("unchecked")

/**
 * Base definition for catalogued heads containing the UUID, texture payload, and translation key.
 * metadata required to render localized names and lore.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class RHead {

  /**
   * Registry identifier used to generate translation keys (e.g., {@code head.<identifier>}).
   */
  private String identifier;

  /**
   * Profile UUID associated with the head texture.
   */
  private UUID uuid;

  /**
   * Base64 encoded texture payload applied to the head.
   */
  private String texture;

  /**
   * Filter classification describing where this head should appear.
   */
  private EHeadFilter filter;

  /**
   * Computed translation namespace, prefixed with {@code head.} and the identifier.
   */
  private String translationKey;

  private RHead() {
  }

  /**
   * Creates a head definition with an explicit filter value.
   *
   * @param identifier unique identifier for the head registry entry
   * @param uuid head profile UUID string
   * @param texture base64 encoded texture payload
   * @param filter filter classification for UI catalogues
   */
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

  /**
   * Creates a head definition defaulting to the {@link EHeadFilter#INVENTORY} filter.
   *
   * @param identifier unique identifier for the head registry entry
   * @param uuid head profile UUID string
   * @param texture base64 encoded texture payload
   */
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

  /**
   * Builds an {@link ItemStack} for the provided player, resolving translated name and lore.
   *
   * @param player player requesting the head, used for locale resolution
   * @return localized head item stack with applied texture
   */
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
                    new I18n.Builder(this.translationKey + ".name", player).build().component()
            )
            .setLore(
                    new I18n.Builder(this.translationKey + ".lore", player).build().children()
            )
            .build();
  }

  /**
   * Identifier used for translation lookups and registry storage.
   *
   * @return head identifier string
   */
  public String getIdentifier() {
    return this.identifier;
  }

  /**
   * Returns the UUID associated with the head texture.
   *
   * @return head profile UUID
   */
  public UUID getUuid() {
    return this.uuid;
  }

  /**
   * Returns the base64 texture payload for the head.
   *
   * @return base64 encoded texture string
   */
  public String getTexture() {
    return this.texture;
  }

  /**
   * Filter classification for the head.
   *
   * @return head filter category
   */
  public EHeadFilter getFilter() {
    return this.filter;
  }

  /**
   * Translation namespace used to resolve localized name and lore.
   *
   * @return translation key prefix
   */
  public String getTranslationKey() {
    return this.translationKey;
  }

}
