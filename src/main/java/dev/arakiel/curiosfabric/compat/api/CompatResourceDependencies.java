package dev.arakiel.curiosfabric.compat.api;

import dev.arakiel.curiosfabric.compat.CompatTargets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Resource-reload listeners that must finish before compatibility metadata is consumed. */
public final class CompatResourceDependencies {

  private CompatResourceDependencies() {
  }

  public static Collection<ResourceLocation> current() {
    if (!CompatTargets.isTrinketsLoaded()) {
      return List.of();
    }
    List<ResourceLocation> result = new ArrayList<>(2);
    add(result, "dev.emi.trinkets.data.SlotLoader", "INSTANCE");
    add(result, "dev.emi.trinkets.data.EntitySlotLoader", "SERVER");
    return result;
  }

  private static void add(List<ResourceLocation> dependencies, String className,
                           String fieldName) {
    try {
      Class<?> type = CompatTargets.loadClass(className);
      if (type == null) {
        return;
      }
      Object listener = type.getField(fieldName).get(null);
      Object id = type.getMethod("getFabricId").invoke(listener);
      if (id instanceof ResourceLocation location) {
        dependencies.add(location);
      }
    } catch (Throwable ignored) {
      // A missing accessor only means Curios falls back to its own merge lifecycle.
    }
  }
}
