package dev.arakiel.curiosfabric.compat.contribution.render;

import dev.arakiel.curiosfabric.api.type.SlotSource;
import net.minecraft.world.item.ItemStack;

/** One renderer that claims support for a query. */
public record RendererCapability(SlotSource source, ItemStack stack) {

  public RendererCapability {
    if (source == null || stack == null) {
      throw new IllegalArgumentException("renderer capability fields must not be null");
    }
  }
}
