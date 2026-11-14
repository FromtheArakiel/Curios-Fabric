package dev.arakiel.curiosfabric.common.slottype;

import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Produces the player-facing slot queue from native and third-party definitions. */
public final class SlotOrdering {

  private static final Comparator<ISlotType> SLOT_COMPARATOR =
      Comparator.comparingInt(ISlotType::getOrder)
          .thenComparing(ISlotType::getMappingIdentifier)
          .thenComparing(ISlotType::getIdentifier);

  private SlotOrdering() {
  }

  public static List<ISlotType> order(Collection<ISlotType> slots) {
    List<ISlotType> nativeSlots = new ArrayList<>();
    List<ISlotType> thirdPartySlots = new ArrayList<>();
    for (ISlotType slot : slots) {
      if (slot.getSource() == SlotSource.CURIOSFABRIC) {
        nativeSlots.add(slot);
      } else {
        thirdPartySlots.add(slot);
      }
    }
    nativeSlots.sort(SLOT_COMPARATOR);
    thirdPartySlots.sort(SLOT_COMPARATOR);

    List<ISlotType> ordered = new ArrayList<>(nativeSlots);
    Map<String, Integer> lastByMappingId = new LinkedHashMap<>();
    for (int index = 0; index < ordered.size(); index++) {
      lastByMappingId.put(ordered.get(index).getMappingIdentifier(), index);
    }

    for (ISlotType slot : thirdPartySlots) {
      Integer anchor = lastByMappingId.get(slot.getMappingIdentifier());
      int insertion = anchor == null ? ordered.size() : anchor + 1;
      ordered.add(insertion, slot);
      lastByMappingId.put(slot.getMappingIdentifier(), insertion);
      for (int index = insertion + 1; index < ordered.size(); index++) {
        lastByMappingId.merge(ordered.get(index).getMappingIdentifier(), index, Math::max);
      }
    }
    return ordered;
  }
}
