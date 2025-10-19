package com.raindropcentral.rplatform.utility.heads.view.pagination;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Numeric head representing the digit {@code 6} for pagination overlays and counters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Number6 extends RHead {

  /**
   * UUID string associated with the digit {@code 6} head texture.
   */
  private static final String UUID = "05e27e0e-0e0d-436a-a371-8b349ed34ea7";

  /**
   * Base64-encoded texture depicting the digit {@code 6}.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmI4ODYxOGJlY2Y2MWFiYzViYzk0YzExNWJjYTEzZGNhM2UyYjliZDlkMGNjYjY4NTY2MTJiYTg0MTlmIn19fQ==";

  /**
   * Creates the digit {@code 6} head definition with translation key {@code head.pagination.6}.
   */
  public Number6() {
    super(
      "6",
      UUID,
      TEXTURE
    );
  }
}
