package com.raindropcentral.rplatform.utility.heads.view;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Navigation control head used to return from nested menu flows.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Return extends RHead {

  /**
   * UUID string associated with the return navigation head.
   */
  public static final String UUID = "7d7075a9-1df3-485d-bf00-ffd6ce2d6244";

  /**
   * Base64-encoded texture representing the return icon.
   */
  public static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTJiYTgxYjQ3ZDVlZTA2YjQ4NGVhOWJkZjIyOTM0ZTZhYmNhNWU0Y2VkN2JlMzkwNWQ2YWU2ZWNkNmZjZWEyYSJ9fX0=";


  /**
   * Creates the return navigation head definition with translation key {@code head.return}.
   */
  public Return() {
    super(
      "return",
      UUID,
      TEXTURE
    );
  }
}
