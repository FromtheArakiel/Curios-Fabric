package dev.arakiel.curiosfabric.compat.contribution.render;

import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import net.minecraft.world.item.ItemStack;

/** Standard renderer query handed to the compatibility arbiter. */
public record RendererQuery(
    SlotSource slotSource,
    ItemStack stack,
    SlotContext slotContext,
    Object[] arguments,
    boolean client) {

  public RendererQuery {
    if (slotSource == null || stack == null || slotContext == null || arguments == null) {
      throw new IllegalArgumentException("renderer query fields must not be null");
    }
    if (arguments.length < 12) {
      throw new IllegalArgumentException("renderer query requires the standard 12 arguments");
    }
  }
}
