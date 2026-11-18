package dev.arakiel.curiosfabric.compat.core.contract;

import dev.arakiel.curiosfabric.api.type.SlotSource;
import java.util.Optional;

/** Stable identity and ordering metadata for a compatibility bridge. */
public record BridgeDescriptor(
    String id,
    String displayName,
    SlotSource source,
    int queueRank,
    Optional<String> apiNamespace) {

  public BridgeDescriptor {
    if (id == null || id.isBlank() || displayName == null || displayName.isBlank()
        || source == null || apiNamespace == null) {
      throw new IllegalArgumentException("descriptor fields must not be null or blank");
    }
  }

  public static BridgeDescriptor of(String id, String displayName, SlotSource source,
                                    int queueRank) {
    return new BridgeDescriptor(id, displayName, source, queueRank, Optional.empty());
  }
}
