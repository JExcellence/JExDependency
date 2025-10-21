package com.raindropcentral.rplatform.utility.heads.view.pagination;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Numeric head representing the digit {@code 5} for pagination overlays and counters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Number5 extends RHead {

  /**
   * UUID string associated with the digit {@code 5} head texture.
   */
  private static final String UUID = "bc86d790-6871-4ae4-8c64-c95f56bc34eb";

  /**
   * Base64-encoded texture depicting the digit {@code 5}.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDEyMDVmZDY4Njg2MzQ3OWMyMTZiMmQ4NGM3YzYxODlkMjkyZmZiZTczMzFhYzNiNzY5NmFiZGViMjM0OTQifX19";

  /**
   * Creates the digit {@code 5} head definition with translation key {@code head.pagination.5}.
   */
  public Number5() {
    super(
      "5",
      UUID,
      TEXTURE
    );
  }
}
