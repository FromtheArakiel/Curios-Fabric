package dev.arakiel.curiosfabric.common.slottype;

import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Produces the player-facing slot queue from native and third-party definitions. */
public final class SlotOrdering {

  private static final Set<String> DEFAULT_MAPPING_IDS = Set.of(
      "back", "belt", "body", "bracelet", "charm",
      "curio", "hands", "head", "necklace", "ring");

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
    return deduplicateByMapping(occupied.values());
  }

  /**
   * The fill queue starts with Curios Fabric defaults, then continues through other Curios Fabric,
   * NeoForge Curios and Trinkets definitions. Upstream definitions mapped to a default are skipped.
   */
  private static List<ISlotType> queuedSlots(Collection<ISlotType> slots) {
    List<ISlotType> defaults = new ArrayList<>();
    List<ISlotType> bridgeQueue = new ArrayList<>();
    for (ISlotType slot : slots) {
      if (usesAnchor(slot) || isIgnoredDefaultMapping(slot)) {
        continue;
      }
      if (isCuriosDefault(slot)) {
        defaults.add(slot);
      } else {
        bridgeQueue.add(slot);
      }
    }
    defaults.sort(java.util.Comparator
        .comparingInt((ISlotType slot) -> slot.getOrder().orElse(Integer.MAX_VALUE))
        .thenComparing(ISlotType::getIdentifier));
    bridgeQueue.sort(java.util.Comparator
        .comparingInt((ISlotType slot) -> slot.getSource().queueRank())
        .thenComparing(ISlotType::getIdentifier));

    defaults.addAll(bridgeQueue);
    return defaults;
  }

  private static boolean usesAnchor(ISlotType slot) {
    return !isCuriosDefault(slot)
        && !isIgnoredDefaultMapping(slot)
        && slot.getSource().usesOrderAnchors()
        && slot.getOrder().isPresent();
  }

  private static List<ISlotType> deduplicateByMapping(Collection<ISlotType> slots) {
    Map<String, ISlotType> unique = new LinkedHashMap<>();
    for (ISlotType slot : slots) {
      unique.putIfAbsent(slot.getMappingIdentifier(), slot);
    }
    return new ArrayList<>(unique.values());
  }

  private static boolean isCuriosDefault(ISlotType slot) {
    return slot.getSource() == SlotSource.CURIOSFABRIC
        && DEFAULT_MAPPING_IDS.contains(slot.getMappingIdentifier());
  }

  private static boolean isIgnoredDefaultMapping(ISlotType slot) {
    return slot.getSource() != SlotSource.CURIOSFABRIC
        && DEFAULT_MAPPING_IDS.contains(slot.getMappingIdentifier());
  }

  private static long anchorPosition(ISlotType slot) {
    int order = slot.getOrder().orElseThrow();
    return slot.getSource() == SlotSource.CURIOSFABRIC
        ? Math.max(0L, order)
        : transformedOrder(order);
  }

  static int transformedOrder(int order) {
    long transformed = Math.round((order - 20.0D) / 20.0D);
    return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, transformed));
  }

  private record Anchor(ISlotType slot, long position) {
  }
}
