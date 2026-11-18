package dev.arakiel.curiosfabric.compat.modules.trinkets.lifecycle;

import dev.arakiel.curiosfabric.compat.contribution.lifecycle.ResourceDependencyContributor;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
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
    return List.of(
        ResourceLocation.fromNamespaceAndPath("trinkets", "slots"),
        ResourceLocation.fromNamespaceAndPath("trinkets", "entities"));
  }
}
