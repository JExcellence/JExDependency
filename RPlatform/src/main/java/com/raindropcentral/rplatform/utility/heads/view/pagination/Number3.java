package com.raindropcentral.rplatform.utility.heads.view.pagination;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Numeric head representing the digit {@code 3} for pagination overlays and counters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Number3 extends RHead {

  /**
   * UUID string associated with the digit {@code 3} head texture.
   */
  private static final String UUID = "4d4388dc-e76d-4c62-9b0c-58f96837a2e5";

  /**
   * Base64-encoded texture depicting the digit {@code 3}.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTA3OWM5ZWU5ODhiNTdiMmQyMzI2ODBmOGJjYTk2MDZjNDlhZGZkYzljYzE4ZTBmYzhjMjI4ZjkzY2NmIn19fQ==";

  /**
   * Creates the digit {@code 3} head definition with translation key {@code head.pagination.3}.
   */
  public Number3() {
    super(
      "3",
      UUID,
      TEXTURE
    );
  }
}
