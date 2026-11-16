package dev.arakiel.curiosfabric.compat.api;

import dev.arakiel.curiosfabric.api.SlotContext;
import net.minecraft.world.item.ItemStack;

/** Invokes a renderer registered with a foreign accessory API. */
public interface RendererBridge {

  boolean supports(ItemStack stack);

  boolean render(ItemStack stack, SlotContext slotContext, Object[] renderArguments);
}
