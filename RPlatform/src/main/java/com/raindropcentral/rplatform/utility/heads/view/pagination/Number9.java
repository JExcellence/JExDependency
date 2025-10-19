package com.raindropcentral.rplatform.utility.heads.view.pagination;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Numeric head representing the digit {@code 9} for pagination overlays and counters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Number9 extends RHead {

  /**
   * UUID string associated with the digit {@code 9} head texture.
   */
  private static final String UUID = "09876543-210f-edcb-a987-6543210fedcb";

  /**
   * Base64-encoded texture depicting the digit {@code 9}.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5In19fQ==";

  /**
   * Creates the digit {@code 9} head definition with translation key {@code head.pagination.9}.
   */
  public Number9() {
    super(
      "9",
      UUID,
      TEXTURE
    );
  }
}
