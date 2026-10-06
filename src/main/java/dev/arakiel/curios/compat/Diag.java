/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package dev.arakiel.curios.compat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.arakiel.curios.CuriosConstants;

/**
 * One-shot diagnostics for the compatibility bridges.
 *
 * <p>Every message is emitted at most once per key, so the compatibility layers can report exactly
 * which step of the foreign lookup or render dispatch failed without spamming the log from a hot
 * path such as the entity render loop.</p>
 */
public final class Diag {

  private static final Set<String> EMITTED = ConcurrentHashMap.newKeySet();

  private Diag() {
  }

  /** Logs {@code message} once for the given key. */
  public static void once(String key, String message, Object... args) {

    if (EMITTED.add(key)) {
      CuriosConstants.LOG.info("[compat-diag] " + message, args);
    }
  }

  /** Logs {@code message} together with the throwable once for the given key. */
  public static void onceError(String key, String message, Throwable throwable) {

    if (EMITTED.add(key)) {
      CuriosConstants.LOG.warn("[compat-diag] " + message, throwable);
    }
  }
}
