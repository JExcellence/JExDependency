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
 * View control head representing a cancellation action within confirmation prompts.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Cancel extends RHead {

  /**
   * UUID string associated with the cancel head texture profile.
   */
  private static final String UUID = "2066a7f4-f2a3-43b3-8425-dfeeb939d334";

  /**
   * Base64-encoded texture used for the cancel head rendering.
   */
  private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjc1NDgzNjJhMjRjMGZhODQ1M2U0ZDkzZTY4YzU5NjlkZGJkZTU3YmY2NjY2YzAzMTljMWVkMWU4NGQ4OTA2NSJ9fX0=";

  /**
   * Creates the cancel head definition with translation key {@code head.cancel}.
   */
  public Cancel() {
    super(
      "cancel",
      UUID,
      TEXTURE
    );
  }
}
