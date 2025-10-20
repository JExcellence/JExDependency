package com.raindropcentral.rplatform.utility.heads.view;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Pagination control head representing navigation to a lower page index.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Down extends RHead {

  /**
   * UUID string associated with the downward navigation head.
   */
  private static final String UUID = "7f39ccb5-a5cd-9bab-b130-97b4f8dbc9f1";

  /**
   * Base64-encoded texture representing the down arrow icon.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2YzOWNjYjVhNWNkOWJhYmIxMzA5N2I0ZjhkYmM5ZjE0ZGMzOGFjMTcwMmEzMGQ5N2UyYWFiZjkwN2JmMTVjIn19fQ==";

  /**
   * Creates the down navigation head definition with translation key {@code head.down}.
   */
  public Down() {
    super(
      "down",
      UUID,
      TEXTURE
    );
  }
}
