/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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
