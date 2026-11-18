package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.discovery;

import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.common.slottype.SlotType;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.duck.NeoforgeCuriosLegacySlotDuck;
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
        if (!(slot instanceof NeoforgeCuriosLegacySlotDuck legacy)) {
          continue;
        }
        String nativeId = NeoforgeCuriosNativeMappings.nativeId(upstreamId);
        String instanceId = nativeId != null ? nativeId : "neoforgecurios_" + upstreamId;
        String mappingId = nativeId != null ? nativeId : upstreamId;
        SlotType.Builder builder = new SlotType.Builder(instanceId)
            .source(SlotSource.NEOFORGE_CURIOS)
            .mappingIdentifier(mappingId)
            .order(legacy.curiosfabric$order(), true)
            .size(legacy.curiosfabric$size(), "SET", true)
            .useNativeGui(legacy.curiosfabric$useNativeGui(), true)
            .hasCosmetic(legacy.curiosfabric$hasCosmetic(), true)
            .renderToggle(legacy.curiosfabric$renderToggle(), true)
            .dropRule(legacy.curiosfabric$dropRule());
        builder.icon(legacy.curiosfabric$icon());
        for (ResourceLocation validator : legacy.curiosfabric$validators()) {
          builder.validator(validator);
        }
        result.add(builder.build());
      }
      return List.copyOf(result);
    } catch (Throwable throwable) {
      return List.of();
    }
  }

}
