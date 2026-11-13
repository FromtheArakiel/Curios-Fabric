package dev.arakiel.curiosfabric.api.type;

/**
 * Used for Curios menu implementations as identification for third-party mods without referencing
 * the implementation directly
 */
public interface ICuriosMenu {

  /**
   * Resets the containing slots to reflect updated data
   */
  void resetSlots();
}
