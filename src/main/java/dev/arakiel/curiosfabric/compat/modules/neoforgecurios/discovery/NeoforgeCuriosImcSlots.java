package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.discovery;

import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.common.slottype.SlotType;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Reads slot definitions registered through NeoForge Curios' inter-mod API. */
public final class NeoforgeCuriosImcSlots {

  private NeoforgeCuriosImcSlots() {
  }

  public static List<ISlotType> read() {
    if (!CompatTargets.isNeoforgeCuriosLoaded()) {
      return List.of();
    }
    try {
      Class<?> manager = CompatTargets.loadClass(
          "top.theillusivec4.curios.common.slottype.LegacySlotManager");
      if (manager == null) {
        return List.of();
      }
      Object rawBuilders = manager.getMethod("getImcBuilders").invoke(null);
      if (!(rawBuilders instanceof java.util.Map<?, ?> builders)) {
        return List.of();
      }
      List<ISlotType> result = new ArrayList<>();
      for (java.util.Map.Entry<?, ?> entry : builders.entrySet()) {
        String upstreamId = String.valueOf(entry.getKey());
        Object slot = entry.getValue().getClass().getMethod("build").invoke(entry.getValue());
        String nativeId = NeoforgeCuriosNativeMappings.nativeId(upstreamId);
        String instanceId = nativeId != null ? nativeId : "neoforgecurios_" + upstreamId;
        String mappingId = nativeId != null ? nativeId : upstreamId;
        SlotType.Builder builder = new SlotType.Builder(instanceId)
            .source(SlotSource.NEOFORGE_CURIOS)
            .mappingIdentifier(mappingId)
            .order(readInt(slot, "getOrder"), true)
            .size(readInt(slot, "getSize"), "SET", true)
            .useNativeGui(readBoolean(slot, "useNativeGui"), true)
            .hasCosmetic(readBoolean(slot, "hasCosmetic"), true)
            .renderToggle(readBoolean(slot, "canToggleRendering"), true);
        Object icon = slot.getClass().getMethod("getIcon").invoke(slot);
        if (icon instanceof ResourceLocation location) {
          builder.icon(location);
        }
        Object dropRule = slot.getClass().getMethod("getDropRule").invoke(slot);
        if (dropRule instanceof Enum<?> value) {
          try {
            builder.dropRule(ICurio.DropRule.valueOf(value.name()));
          } catch (IllegalArgumentException ignored) {
            // Unknown upstream drop rules intentionally use the CuriosFabric default.
          }
        }
        result.add(builder.build());
      }
      return List.copyOf(result);
    } catch (Throwable throwable) {
      return List.of();
    }
  }

  private static int readInt(Object target, String method) throws ReflectiveOperationException {
    return (Integer) target.getClass().getMethod(method).invoke(target);
  }

  private static boolean readBoolean(Object target, String method)
      throws ReflectiveOperationException {
    return (Boolean) target.getClass().getMethod(method).invoke(target);
  }
}
