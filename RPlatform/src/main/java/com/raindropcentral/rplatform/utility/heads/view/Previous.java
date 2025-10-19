package com.raindropcentral.rplatform.utility.heads.view;

import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Pagination control head representing navigation to the previous page.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Previous extends RHead {

  /**
   * UUID string associated with the previous-page navigation head.
   */
  private static final String UUID = "2903c9aa-6ea6-43f9-9601-75f0a50c49ca";

  /**
   * Base64-encoded texture representing the previous arrow icon.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmE2Y2FhMWUxZTlkOWFlZjU5Mjc4NzExNDIyNzg3YTAxNzk5M2M1YjI5MjUxOGM5ZjYzMmQ0MTJmNWE2NTkifX19";

  /**
   * Creates the previous navigation head definition with translation key {@code head.previous}.
   */
  public Previous() {
    super(
      "previous",
      UUID,
      TEXTURE
    );
  }
}
