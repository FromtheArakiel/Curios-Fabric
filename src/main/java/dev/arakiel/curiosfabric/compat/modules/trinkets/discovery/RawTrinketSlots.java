package dev.arakiel.curiosfabric.compat.modules.trinkets.discovery;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
import dev.arakiel.curiosfabric.compat.modules.trinkets.mixin.TrinketGroupDataAccessor;
import dev.arakiel.curiosfabric.compat.modules.trinkets.mixin.TrinketSlotDataAccessor;
import dev.arakiel.curiosfabric.compat.modules.trinkets.mixin.TrinketSlotLoaderAccessor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Reads the original Trinkets slot JSON values through generated Mixin accessors. */
public final class RawTrinketSlots {

  private static final String SLOT_LOADER = "dev.emi.trinkets.data.SlotLoader";

  private RawTrinketSlots() {
  }

  public static Map<String, RawSlot> read() {
    Map<String, RawSlot> result = new LinkedHashMap<>();
    try {
      Class<?> loaderClass = CompatTargets.loadClass(SLOT_LOADER);
      if (loaderClass == null) {
        return result;
      }
      Object loader = loaderClass.getField("INSTANCE").get(null);
      if (!(loader instanceof TrinketSlotLoaderAccessor accessor)) {
        return result;
      }
      accessor.curiosfabric$getLoadedSlots().forEach((groupName, group) -> {
        if (!(group instanceof TrinketGroupDataAccessor groupAccessor)) {
          return;
        }
        groupAccessor.curiosfabric$getSlots().forEach((slotName, slot) -> {
          if (slot instanceof TrinketSlotDataAccessor slotAccessor) {
            result.put(groupName + "/" + slotName, new RawSlot(
                slotAccessor.curiosfabric$getOrder(),
                slotAccessor.curiosfabric$getAmount(),
                slotAccessor.curiosfabric$getIcon(),
                slotAccessor.curiosfabric$getDropRule(),
                Set.copyOf(slotAccessor.curiosfabric$getValidatorPredicates())));
          }
        });
      });
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug(
          "Raw Trinkets slot access unavailable; using EntitySlotLoader metadata", throwable);
    }
    return result;
  }

  public record RawSlot(int order, int amount, String icon, String dropRule,
                        Set<String> validators) {
  }
}
