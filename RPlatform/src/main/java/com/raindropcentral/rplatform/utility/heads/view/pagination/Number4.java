package com.raindropcentral.rplatform.utility.heads.view.pagination;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Numeric head representing the digit {@code 4} for pagination overlays and counters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Number4 extends RHead {

  /**
   * UUID string associated with the digit {@code 4} head texture.
   */
  private static final String UUID = "934f184a-cf55-4fa6-9ba5-10aa7916d24f";

  /**
   * Base64-encoded texture depicting the digit {@code 4}.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGYzYzU1ZTc5NTFiNDg0ZDE5NGY2ZjRhZjE2ZjVhNmFkZjU4NmIyNTg1NzlkZjNiOGJlMmIzODU2OTFlNTUyNyJ9fX0=";

  /**
   * Creates the digit {@code 4} head definition with translation key {@code head.pagination.4}.
   */
  public Number4() {
    super(
      "4",
      UUID,
      TEXTURE
    );
  }
}
