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
 * View control head representing entry into leaderboard or ranking menus.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class Leaderboard extends RHead {

  /**
   * UUID string associated with the leaderboard head profile.
   */
  public static final String UUID = "f84c6a79-0a4e-45e0-879b-cd49ebd4c4e2";

  /**
   * Base64-encoded texture representing the leaderboard icon.
   */
  public static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTJlODMwNTg5YTJkZGY3ZjE2ZGE5YjI4NzNkZjk2YzQzNzU4NzNkZGY5YzQzNzU4NzNkZGY5YzQzNzU4NzNkZiJ9fX0=";

  /**
   * Creates the leaderboard head definition with translation key {@code head.leaderboard}.
   */
  public Leaderboard() {
    super(
      "leaderboard",
      UUID,
      TEXTURE
    );
  }
}