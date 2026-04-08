/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.utility.heads.view.pagination;

import com.raindropcentral.rplatform.utility.heads.RHead;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Numeric head representing the digit {@code 0} for pagination overlays and counters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Number0 extends RHead {

  /**
   * UUID string associated with the digit {@code 0} head texture.
   */
  private static final String UUID = "f56125cd-447b-424d-8168-9d030c2cec8b";

  /**
   * Base64-encoded texture depicting the digit {@code 0}.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTE4OTc3Y2Y4ZjJmZjBlYTkyZTZkYjZhNTZjZWExZjVmOGFlNDRjN2NhOTM3YWZhZTdlNTI2Y2M5OGRiZDgifX19";

  /**
   * Creates the digit {@code 0} head definition with translation key {@code head.pagination.0}.
   */
  public Number0() {
    super(
      "0",
      UUID,
      TEXTURE
    );
  }

  /**
   * Gets translationKey.
   */
  @Override
  public String getTranslationKey() {
    return "head.pagination." + this.getIdentifier();
  }

  /**
   * Gets head.
   */
  @Override
  public ItemStack getHead(final @NotNull Player player) {
    final String translationKey = this.getTranslationKey();

    return
        UnifiedBuilderFactory
            .head()
            .setCustomTexture(
                this.getUuid(),
                this.getTexture()
            )
            .setName(
                    new I18n.Builder(translationKey + ".name", player).build().<Component>component()
            )
            .setLore(
                    new I18n.Builder(translationKey + ".lore", player).build().children()
            )
            .build();
  }
}
