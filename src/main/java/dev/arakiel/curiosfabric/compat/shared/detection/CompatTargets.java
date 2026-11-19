package dev.arakiel.curiosfabric.compat.shared.detection;

import dev.arakiel.gemini.integration.GeminiIntegrations;

/**
 * Detection of the accessory implementations that this mod takes over: the NeoForge edition of
 * Curios and Trinkets.
 *
 * <p>Nothing here links against those mods: everything is done through mod ids and class names, so
 * the class is always loadable and simply reports "absent" when a target is missing.</p>
 */
public final class CompatTargets {

  /** The mod id of the NeoForge edition of Curios. */
  public static final String NEOFORGE_CURIOS = "curios";
  /** A class that only the NeoForge edition of Curios has. */
  public static final String NEOFORGE_CURIOS_CLASS = "top.theillusivec4.curios.api.CuriosApi";
  /** Package of the NeoForge client GUI. */
  public static final String NEOFORGE_CURIOS_CLIENT_PACKAGE = "top.theillusivec4.curios.client";
  /** The mod id of Trinkets. */
  public static final String TRINKETS = "trinkets";
  /** Package of Trinkets' client side. */
  public static final String TRINKETS_CLIENT_PACKAGE = "dev.emi.trinkets";
  /** The mod id of Accessories. */
  public static final String ACCESSORIES = "accessories";
  /** Package of the Accessories client side. */
  public static final String ACCESSORIES_CLIENT_PACKAGE = "io.wispforest.accessories.client";
  /** The mod id of Aether. */
  public static final String AETHER = "aether";

  private CompatTargets() {
  }

  /** {@code true} when the NeoForge edition of Curios is present. */
  public static boolean isNeoforgeCuriosLoaded() {
    return GeminiIntegrations.hasActiveIntegration(NEOFORGE_CURIOS)
        || GeminiIntegrations.isModLoaded(NEOFORGE_CURIOS)
        || classExists(NEOFORGE_CURIOS_CLASS);
  }

  /** {@code true} when Trinkets is present. */
  public static boolean isTrinketsLoaded() {
    return GeminiIntegrations.hasActiveIntegration(TRINKETS)
        || GeminiIntegrations.isModLoaded(TRINKETS)
        || classExists("dev.emi.trinkets.api.TrinketsApi");
  }

  /** {@code true} when Accessories is present. */
  public static boolean isAccessoriesLoaded() {
    return GeminiIntegrations.isModLoaded(ACCESSORIES)
        || classExists("io.wispforest.accessories.api.AccessoriesAPI");
  }

  /** {@code true} when Aether is present. */
  public static boolean isAetherLoaded() {
    return GeminiIntegrations.isModLoaded(AETHER)
        || classExists("com.aetherteam.aether.Aether");
  }

  /** {@code true} when a NeoForge runtime (Kilt, Connector, ...) provides the NeoForge hooks. */
  public static boolean isNeoforgeLoaded() {
    return GeminiIntegrations.isModLoaded("neoforge")
        || classExists("net.neoforged.neoforge.common.NeoForge");
  }

  /**
   * {@code true} when the screen belongs to another accessory implementation and should therefore
   * never be shown: Curios is the only accessory UI in this installation.
   *
   * @param className The fully qualified class name of the screen
   */
  public static boolean isForeignAccessoryScreen(String className) {

    if (className == null || className.startsWith("dev.arakiel.curiosfabric")) {
      return false;
    }
    return className.startsWith(NEOFORGE_CURIOS_CLIENT_PACKAGE)
        || className.startsWith(TRINKETS_CLIENT_PACKAGE)
        || className.startsWith(ACCESSORIES_CLIENT_PACKAGE);
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
