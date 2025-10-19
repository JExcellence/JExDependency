package com.raindropcentral.rplatform.utility.heads.view;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * View control head representing a positive confirmation choice.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Proceed extends RHead {

  /**
   * UUID string associated with the proceed head texture profile.
   */
  private static final String UUID = "afb405c1-16ea-4a23-883f-97867e7db3f9";

  /**
   * Base64-encoded texture used for the proceed head rendering.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTc5YTVjOTVlZTE3YWJmZWY0NWM4ZGMyMjQxODk5NjQ5NDRkNTYwZjE5YTQ0ZjE5ZjhhNDZhZWYzZmVlNDc1NiJ9fX0=";

  /**
   * Creates the proceed head definition with translation key {@code head.proceed}.
   */
  public Proceed() {
    super(
      "proceed",
      UUID,
      TEXTURE
    );
  }
}
