package dev.arakiel.curiosfabric.compat.contribution.slot;

import dev.arakiel.curiosfabric.api.type.SlotSource;

/** Stable identity of an upstream slot before final CuriosFabric de-duplication. */
public record SlotCandidateKey(SlotSource source, String upstreamPath) {

  public SlotCandidateKey {
    if (source == null || upstreamPath == null || upstreamPath.isBlank()) {
      throw new IllegalArgumentException("slot candidate key fields must not be blank");
    }
  }
}
