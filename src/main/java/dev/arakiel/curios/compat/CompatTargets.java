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

import net.fabricmc.loader.api.FabricLoader;

/**
 * Detection of the accessory implementations that this mod takes over: the Forge/NeoForge edition
 * of Curios (loaded by a cross loader layer such as Kilt or Connector) and Trinkets.
 *
 * <p>Nothing here links against those mods: everything is done through mod ids and class names, so
 * the class is always loadable and simply reports "absent" when a target is missing.</p>
 */
public final class CompatTargets {

  /** The mod id of the Forge/NeoForge edition of Curios. */
  public static final String FORGE_CURIOS = "curios";
  /** A class that only the Forge/NeoForge edition of Curios has. */
  public static final String FORGE_CURIOS_CLASS = "top.theillusivec4.curios.api.CuriosApi";
  /** Package of the Forge/NeoForge client GUI. */
  public static final String FORGE_CURIOS_CLIENT_PACKAGE = "top.theillusivec4.curios.client";
  /** The mod id of Trinkets. */
  public static final String TRINKETS = "trinkets";
  /** Package of Trinkets' client side. */
  public static final String TRINKETS_CLIENT_PACKAGE = "dev.emi.trinkets";

  private CompatTargets() {
  }

  /** {@code true} when the Forge/NeoForge edition of Curios is present. */
  public static boolean isForgeCuriosLoaded() {
    return FabricLoader.getInstance().isModLoaded(FORGE_CURIOS)
        || classExists(FORGE_CURIOS_CLASS);
  }

  /** {@code true} when Trinkets is present. */
  public static boolean isTrinketsLoaded() {
    return FabricLoader.getInstance().isModLoaded(TRINKETS)
        || classExists("dev.emi.trinkets.api.TrinketsApi");
  }

  /** {@code true} when a NeoForge runtime (Kilt, Connector, ...) provides the NeoForge hooks. */
  public static boolean isNeoForgeLoaded() {
    return FabricLoader.getInstance().isModLoaded("neoforge")
        || classExists("net.neoforged.neoforge.common.NeoForge");
  }

  /**
   * {@code true} when the screen belongs to another accessory implementation and should therefore
   * never be shown: Curios is the only accessory UI in this installation.
   *
   * @param className The fully qualified class name of the screen
   */
  public static boolean isForeignAccessoryScreen(String className) {

    if (className == null || className.startsWith("dev.arakiel.curios")) {
      return false;
    }
    return className.startsWith(FORGE_CURIOS_CLIENT_PACKAGE)
        || className.startsWith(TRINKETS_CLIENT_PACKAGE);
  }

  /** {@code true} when a class is present on the current class loader. */
  public static boolean classExists(String className) {

    try {
      Class.forName(className, false, CompatTargets.class.getClassLoader());
      return true;
    } catch (Throwable throwable) {
      return false;
    }
  }

  /** Loads a class without initializing it, or {@code null} when it is absent. */
  public static Class<?> loadClass(String className) {

    try {
      return Class.forName(className, false, CompatTargets.class.getClassLoader());
    } catch (Throwable throwable) {
      return null;
    }
  }
}
