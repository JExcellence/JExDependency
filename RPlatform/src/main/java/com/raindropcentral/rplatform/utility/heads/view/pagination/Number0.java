package com.raindropcentral.rplatform.utility.heads.view.pagination;


import com.raindropcentral.rplatform.utility.heads.RHead;

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
}
