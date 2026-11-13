package dev.arakiel.curiosfabric.api;

import net.minecraft.world.item.ItemStack;

public record SlotResult(SlotContext slotContext, ItemStack stack) {
}
