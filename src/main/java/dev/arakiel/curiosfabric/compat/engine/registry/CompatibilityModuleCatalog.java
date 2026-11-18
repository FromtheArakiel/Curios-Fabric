package dev.arakiel.curiosfabric.compat.engine.registry;

import dev.arakiel.curiosfabric.compat.core.contract.CompatibilityBridge;
import dev.arakiel.curiosfabric.compat.modules.accessories.AccessoriesBridge;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.NeoforgeCuriosBridge;
import dev.arakiel.curiosfabric.compat.modules.trinkets.TrinketsBridge;
import java.util.List;

/** Explicit composition root for built-in compatibility modules. */
public final class CompatibilityModuleCatalog {

  private CompatibilityModuleCatalog() {
  }

  public static List<CompatibilityBridge> builtInBridges() {
    return List.of(
        new TrinketsBridge(),
        new NeoforgeCuriosBridge(),
        new AccessoriesBridge());
  }
}
