package dev.arakiel.curiosfabric.common.inventory;

import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curiosfabric.api.type.inventory.ICurioStacksHandler;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;
import dev.arakiel.curiosfabric.common.inventory.container.CuriosTabLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;

/**
 * Flattens the visible Curios slots in the same queue order used by the normal Curios menu.
 *
 * <p>This is intentionally source-agnostic. Native Curios, NeoForge Curios, Trinkets and
 * Accessories slots have already been merged before this point.</p>
 */
public final class CuriosVisibleSlots {

  private CuriosVisibleSlots() {
  }

  public static List<Entry> collect(Player player) {
    ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).orElse(null);
    if (handler == null) {
      return List.of();
    }
    if (handler.getCurios().isEmpty()) {
      // A menu can be constructed before the first inventory payload is applied. Rebuilding from
      // the already-synchronised slot definitions keeps the tabbed menu from opening empty.
      handler.reset();
    }

    List<Entry> result = new ArrayList<>();
    // The normal Curios menu also iterates this map directly. Reusing it preserves the finalized
    // slot-kind order, including anchors and queue placement, without a second ordering pass.
    handler.getCurios().forEach((identifier, stacksHandler) ->
        append(result, identifier, stacksHandler));
    return List.copyOf(result);
  }

  public static List<Entry> page(List<Entry> slots, int page) {
    int start = Math.min(slots.size(), CuriosTabLayout.pageStart(page));
    int end = Math.min(slots.size(), start + CuriosTabLayout.PAGE_SIZE);
    return slots.subList(start, end);
  }

  private static void append(List<Entry> result, String identifier,
                             ICurioStacksHandler stacksHandler) {
    if (!stacksHandler.isVisible()) {
      return;
    }
    IDynamicStackHandler stacks = stacksHandler.getStacks();
    for (int index = 0; index < stacks.getSlots(); index++) {
      result.add(new Entry(identifier, stacksHandler, index));
    }
  }

  public record Entry(String identifier, ICurioStacksHandler stacksHandler, int index) {
    public IDynamicStackHandler stacks() {
      return stacksHandler.getStacks();
    }
  }
}
