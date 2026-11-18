package dev.arakiel.curiosfabric.compat.engine.state;

import dev.arakiel.curiosfabric.compat.core.contract.BridgeContribution;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeGeneration;
import java.util.Map;

/** One immutable side-scoped set of bridge contributions. */
public record BridgeSideSnapshot(
    BridgeGeneration generation,
    Map<String, BridgeContribution> contributions) {

  public BridgeSideSnapshot {
    if (generation == null || contributions == null) {
      throw new IllegalArgumentException("snapshot fields must not be null");
    }
    contributions = Map.copyOf(contributions);
  }
}
