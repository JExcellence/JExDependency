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
 * Pagination control head representing navigation to a lower page index.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
public class Down extends RHead {

  /**
   * Identifier used to resolve pagination translations.
   */
  private static final String IDENTIFIER = "pagination.down";

  /**
   * UUID string associated with the downward navigation head.
   */
  private static final String UUID = "7f39ccb5-a5cd-9bab-b130-97b4f8dbc9f1";

  /**
   * Base64-encoded texture representing the down arrow icon.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2YzOWNjYjVhNWNkOWJhYmIxMzA5N2I0ZjhkYmM5ZjE0ZGMzOGFjMTcwMmEzMGQ5N2UyYWFiZjkwN2JmMTVjIn19fQ==";

  /**
   * Creates the down navigation head definition with translation key {@code head.pagination.down}.
   */
  public Down() {
    super(
      IDENTIFIER,
      UUID,
      TEXTURE
    );
  }
}
