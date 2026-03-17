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

package com.raindropcentral.rplatform.utility.heads.view.pagination;


import com.raindropcentral.rplatform.utility.heads.RHead;

/**
 * Numeric head representing the digit {@code 2} for pagination overlays and counters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Number2 extends RHead {

  /**
   * UUID string associated with the digit {@code 2} head texture.
   */
  private static final String UUID = "6df784cf-bebc-4f6a-968d-452d6a4d3344";

  /**
   * Base64-encoded texture depicting the digit {@code 2}.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDBlYmMzMzUyYzI1MmQyMzU3MTNkNWNiY2JjYTg3OTAyNTIyMWNhYWFlOWM0YWUwY2FiNzkyZDk3NGU2NSJ9fX0=";

  /**
   * Creates the digit {@code 2} head definition with translation key {@code head.pagination.2}.
   */
  public Number2() {
    super(
      "2",
      UUID,
      TEXTURE
    );
  }
}
