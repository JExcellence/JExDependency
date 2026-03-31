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
 * Numeric head representing the value {@code 10} for pagination overlays and counters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Number10 extends RHead {

  /**
   * UUID string associated with the value {@code 10} head texture.
   */
  private static final String UUID = "0abcdef1-2345-6789-0abc-def123456789";

  /**
   * Base64-encoded texture depicting the value {@code 10}.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5YzY5In19fQ==";

  /**
   * Creates the value {@code 10} head definition with translation key {@code head.pagination.10}.
   */
  public Number10() {
    super(
      "10",
      UUID,
      TEXTURE
    );
  }
}
