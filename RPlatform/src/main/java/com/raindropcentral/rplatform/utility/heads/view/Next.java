package com.raindropcentral.rplatform.utility.heads.view;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Pagination control head representing navigation to the next page.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Next extends RHead {

  /**
   * UUID string associated with the next-page navigation head.
   */
  private static final String UUID = "e0b1b4fd-ec3c-46ca-96ff-85f0073cbfb8";

  /**
   * Base64-encoded texture representing the next arrow icon.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTk5YWI0NDFmYzk3ZjYwOTA4MWFkM2NlMzNkNTk4MjkxZDUxYmVmOGNiN2FkMjQ4NGI1YzEzODdjN2E4NCJ9fX0=";

  /**
   * Creates the next navigation head definition with translation key {@code head.next}.
   */
  public Next() {
    super(
      "next",
      UUID,
      TEXTURE
    );
  }
}
