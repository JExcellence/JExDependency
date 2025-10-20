package com.raindropcentral.rplatform.utility.heads.view.pagination;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Numeric head representing the digit {@code 1} for pagination overlays and counters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Number1 extends RHead {

  /**
   * UUID string associated with the digit {@code 1} head texture.
   */
  private static final String UUID = "e4e58ac7-b628-40f1-a7eb-44f281a5e336";

  /**
   * Base64-encoded texture depicting the digit {@code 1}.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmY5Njk0YTUyMjEyNzE5ZTExM2RjN2U2YWY2OThhOWZjM2FiNjNjNzQ5OTVmZmFkYzU3ZDM0NmZhY2U0ZTc1In19fQ==";

  /**
   * Creates the digit {@code 1} head definition with translation key {@code head.pagination.1}.
   */
  public Number1() {
    super(
      "1",
      UUID,
      TEXTURE
    );
  }
}
