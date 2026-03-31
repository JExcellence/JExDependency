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
 * Numeric head representing the value {@code 11} for pagination overlays and counters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Number11 extends RHead {

  /**
   * UUID string associated with the value {@code 11} head texture.
   */
  private static final String UUID = "e1682163-9280-44dd-a1a0-dad6d2b4fb05";

  /**
   * Base64-encoded texture depicting the value {@code 11}.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjhiMzQ3YTA3NjUxM2Q1OWYwZDZiNDNjMzEwM2ZjY2NmMTJhNjI4MjZjYzViZDVlYzEzNWU2NTczMWExNjIxIn19fQ==";

  /**
   * Creates the value {@code 11} head definition with translation key {@code head.pagination.11}.
   */
  public Number11() {
    super(
      "11",
      UUID,
      TEXTURE
    );
  }
}
