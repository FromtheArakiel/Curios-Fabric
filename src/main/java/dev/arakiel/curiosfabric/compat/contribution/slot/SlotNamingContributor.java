package dev.arakiel.curiosfabric.compat.contribution.slot;

import dev.arakiel.curiosfabric.api.type.SlotSource;

/** Resolves a slot's theoretical language key while preserving its source semantics. */
public interface SlotNamingContributor {

  String translationKey(String slotId, String mappingId, SlotSource source);
}
