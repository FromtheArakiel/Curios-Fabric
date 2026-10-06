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

package dev.arakiel.curios.api;

/**
 * A three valued result: {@link #TRUE}, {@link #FALSE} and {@link #DEFAULT}, where {@code DEFAULT}
 * means "let the normal rules decide".
 *
 * <p>The NeoForge edition uses NeoForge's {@code TriState} for its equip/unequip events. Since that
 * class is not available on Fabric, the equivalent enum lives here.</p>
 */
public enum TriState {
  TRUE,
  FALSE,
  DEFAULT
}
