package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.duck;

import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Strongly typed access to NeoForge Curios' fixed LegacySlotManager slot implementation. */
public interface NeoforgeCuriosLegacySlotDuck {

  int curiosfabric$order();

  int curiosfabric$size();

  boolean curiosfabric$useNativeGui();

  boolean curiosfabric$hasCosmetic();

  boolean curiosfabric$renderToggle();

  ResourceLocation curiosfabric$icon();

  Set<ResourceLocation> curiosfabric$validators();

  ICurio.DropRule curiosfabric$dropRule();
}
