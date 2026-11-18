package dev.arakiel.curiosfabric.compat.modules.trinkets.lifecycle;

import dev.arakiel.curiosfabric.compat.contribution.lifecycle.ResourceDependencyContributor;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Trinkets listeners that must finish before its dynamic slot metadata is consumed. */
public final class TrinketsResourceDependencies implements ResourceDependencyContributor {

  @Override
  public Collection<ResourceLocation> dependencies() {
    if (!CompatTargets.isTrinketsLoaded()) {
      return List.of();
    }
    List<ResourceLocation> result = new ArrayList<>(2);
    add(result, "dev.emi.trinkets.data.SlotLoader", "INSTANCE");
    add(result, "dev.emi.trinkets.data.EntitySlotLoader", "SERVER");
    return List.copyOf(result);
  }

  private void add(List<ResourceLocation> dependencies, String className, String fieldName) {
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
      // The bridge lifecycle will merge again after the data-pack reload if needed.
    }
  }
}
