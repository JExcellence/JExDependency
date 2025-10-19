package com.raindropcentral.rplatform.utility.heads.view.pagination;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Numeric head representing the digit {@code 8} for pagination overlays and counters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Number8 extends RHead {

  /**
   * UUID string associated with the digit {@code 8} head texture.
   */
  private static final String UUID = "fedcba98-7654-3210-fedc-ba9876543210";

  /**
   * Base64-encoded texture depicting the digit {@code 8}.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5In19fQ==";

  /**
   * Creates the digit {@code 8} head definition with translation key {@code head.pagination.8}.
   */
  public Number8() {
    super(
      "8",
      UUID,
      TEXTURE
    );
  }
}
