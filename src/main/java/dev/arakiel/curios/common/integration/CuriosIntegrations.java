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

package dev.arakiel.curios.common.integration;

/**
 * Optional third party integrations.
 *
 * <p>The NeoForge edition hooked EMI through the NeoForge event bus. On Fabric the equivalent EMI
 * entrypoint is available, but EMI is not one of the compile time dependencies of this project, so
 * the hook is not shipped. JEI is still fully supported through the {@code jei_mod_plugin}
 * entrypoint, and REI/EMI users only lose the extra "exclusion area" around the Curios panel.</p>
 */
public class CuriosIntegrations {

  private CuriosIntegrations() {
  }

  /** Called once during mod initialization. */
  public static void setup() {
    // NO-OP: the JEI plugin is discovered through its entrypoint.
  }
}
