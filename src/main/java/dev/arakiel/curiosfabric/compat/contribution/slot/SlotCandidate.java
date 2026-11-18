package dev.arakiel.curiosfabric.compat.contribution.slot;

import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;

/** A bridge-owned slot definition ready for final coordination. */
public record SlotCandidate(
    SlotCandidateKey key,
    String instanceId,
    String mappingId,
    SlotSource source,
    boolean nativeSlot,
    ISlotType slotType) {

  public SlotCandidate {
    if (key == null || instanceId == null || instanceId.isBlank()
        || mappingId == null || mappingId.isBlank() || source == null || slotType == null) {
      throw new IllegalArgumentException("slot candidate fields must not be null or blank");
    }
    if (!instanceId.equals(slotType.getIdentifier())) {
      throw new IllegalArgumentException("instanceId does not match the slot type identifier");
    }
    if (!mappingId.equals(slotType.getMappingIdentifier())) {
      throw new IllegalArgumentException("mappingId does not match the slot type mapping");
    }
    if (source != slotType.getSource()) {
      throw new IllegalArgumentException("source does not match the slot type source");
    }
  }
}
