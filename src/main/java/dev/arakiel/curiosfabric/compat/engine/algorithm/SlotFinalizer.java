package dev.arakiel.curiosfabric.compat.engine.algorithm;

import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.common.slottype.SlotOrdering;
import java.util.Collection;
import java.util.List;

/** Applies the global slot ordering and de-duplication policy. */
public final class SlotFinalizer {

  private SlotFinalizer() {
  }

  public static List<ISlotType> finalizeOrder(Collection<ISlotType> slots) {
    return SlotOrdering.order(slots);
  }
}
