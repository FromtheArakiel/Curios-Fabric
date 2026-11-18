package dev.arakiel.curiosfabric.compat.engine.registry;

import dev.arakiel.curiosfabric.compat.core.contract.BridgeAvailability;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeState;
import dev.arakiel.curiosfabric.compat.core.contract.CompatibilityBridge;
import java.util.List;
import java.util.Objects;

/** Deterministic registry of all built-in compatibility bridges. */
public final class CompatibilityBridgeRegistry {

  private final List<CompatibilityBridge> bridges;

  public CompatibilityBridgeRegistry(List<CompatibilityBridge> bridges) {
    if (bridges == null || bridges.isEmpty()) {
      this.bridges = List.of();
      return;
    }
    this.bridges = List.copyOf(bridges);
    java.util.Set<String> ids = new java.util.HashSet<>();
    for (CompatibilityBridge bridge : this.bridges) {
      Objects.requireNonNull(bridge, "bridge");
      String id = bridge.descriptor().id();
      if (!ids.add(id)) {
        throw new IllegalArgumentException("duplicate compatibility bridge id: " + id);
      }
    }
  }

  public List<CompatibilityBridge> all() {
    return bridges;
  }

  public List<CompatibilityBridge> active(BridgeContext context) {
    return bridges.stream()
        .filter(bridge -> {
          try {
            BridgeAvailability availability = bridge.availability(context);
            return availability.active();
          } catch (Throwable throwable) {
            dev.arakiel.curiosfabric.CuriosFabricConstants.LOG.warn(
                "Compatibility bridge {} failed an availability check",
                bridge.descriptor().id(), throwable);
            return false;
          }
        })
        .toList();
  }
}
