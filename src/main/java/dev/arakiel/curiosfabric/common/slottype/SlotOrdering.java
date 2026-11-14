package dev.arakiel.curiosfabric.common.slottype;

import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.compat.NativeSlotMappings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Produces the player-facing slot queue from native and third-party definitions. */
public final class SlotOrdering {

  private static final java.util.Comparator<Anchor> ANCHOR_COMPARATOR =
      java.util.Comparator.comparingLong(Anchor::position)
          .thenComparing(anchor -> anchor.slot().getIdentifier());

  private SlotOrdering() {
  }

  public static List<ISlotType> order(Collection<ISlotType> slots) {
    List<Anchor> anchors = new ArrayList<>();
    for (ISlotType slot : slots) {
      if (usesAnchor(slot)) {
        anchors.add(new Anchor(slot, anchorPosition(slot)));
      }
    }
    anchors.sort(ANCHOR_COMPARATOR);

    Map<Long, ISlotType> occupied = new TreeMap<>();
    long previous = -1L;
    for (Anchor anchor : anchors) {
      long position = Math.max(anchor.position(), previous + 1L);
      while (occupied.containsKey(position)) {
        position++;
      }
      occupied.put(position, anchor.slot());
      previous = position;
    }

    long nextFree = 0L;
    for (ISlotType slot : queuedSlots(slots)) {
      while (occupied.containsKey(nextFree)) {
        nextFree++;
      }
      occupied.put(nextFree, slot);
      nextFree++;
    }
    return new ArrayList<>(occupied.values());
  }

  /**
   * Remaining definitions retain their discovery order. Trinkets slots are deliberately appended
   * after native and NeoForge queue entries; a slot that maps to a native identifier is inserted
   * after the last definition with that mapping.
   */
  private static List<ISlotType> queuedSlots(Collection<ISlotType> slots) {
    List<ISlotType> nativeQueue = new ArrayList<>();
    List<ISlotType> trinketsQueue = new ArrayList<>();
    for (ISlotType slot : slots) {
      if (usesAnchor(slot)) {
        continue;
      }
      if (slot.getSource() == SlotSource.TRINKETS) {
        trinketsQueue.add(slot);
      } else {
        nativeQueue.add(slot);
      }
    }
    nativeQueue.sort(java.util.Comparator.comparingInt(ISlotType::getOrder)
        .thenComparing(ISlotType::getIdentifier));

    // TODO(low): Preserve Trinkets group and slot order if its ordering policy becomes required.
    for (ISlotType slot : trinketsQueue) {
      int insertion = nativeQueue.size();
      for (int index = nativeQueue.size() - 1; index >= 0; index--) {
        if (nativeQueue.get(index).getMappingIdentifier()
            .equals(slot.getMappingIdentifier())) {
          insertion = index + 1;
          break;
        }
      }
      nativeQueue.add(insertion, slot);
    }
    return nativeQueue;
  }

  private static boolean usesAnchor(ISlotType slot) {
    return slot.getSource() == SlotSource.CURIOSFABRIC
        || slot.getSource() == SlotSource.NEOFORGE_CURIOS && !isNeoForgeNative(slot);
  }

  private static boolean isNeoForgeNative(ISlotType slot) {
    return slot.getIdentifier().equals(slot.getMappingIdentifier())
        && NativeSlotMappings.neoforgeNative(slot.getIdentifier()) != null;
  }

  private static long anchorPosition(ISlotType slot) {
    return slot.getSource() == SlotSource.CURIOSFABRIC
        ? Math.max(0L, slot.getOrder())
        : transformedOrder(slot.getOrder());
  }

  static int transformedOrder(int order) {
    long transformed = Math.round((order - 20.0D) / 20.0D);
    return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, transformed));
  }

  private record Anchor(ISlotType slot, long position) {
  }
}
