package dev.arakiel.curiosfabric.compat.contribution.lifecycle;

import java.util.Collection;
import net.minecraft.resources.ResourceLocation;

/** Upstream reload listeners that must complete before compatibility metadata is consumed. */
public interface ResourceDependencyContributor {

  Collection<ResourceLocation> dependencies();
}
