/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 */

package dev.arakiel.curios.compat.trinkets;

import dev.arakiel.curios.api.SlotContext;
import dev.arakiel.curios.api.type.capability.ICurioItem;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ICurioItem} bridge used for items that were written against Trinkets.
 *
 * <p>Only the slot validity has to be translated: Trinkets items store their bonuses in the vanilla
 * {@code minecraft:attribute_modifiers} component (Trinkets applies them through the same component),
 * and Curios merges that component into the worn attributes as well, so attributes carry over
 * without extra work. Everything else (ticking, equip/unequip hooks, drop rules) has no Trinkets
 * counterpart and keeps the Curios defaults.</p>
 */
public enum TrinketCurio implements ICurioItem {
  INSTANCE;

  @Override
  public boolean canEquip(SlotContext slotContext, ItemStack stack) {
    return mapsTo(slotContext.identifier(), stack);
  }

  @Override
  public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
    return mapsTo(slotContext.identifier(), stack);
  }

  /** {@code true} when the stack is registered for the given Curios slot through Trinkets' tags. */
  public static boolean mapsTo(String curiosSlot, ItemStack stack) {
    String group = TrinketSlots.toTrinkets(curiosSlot);
    return group != null && TrinketsCompat.isTrinketFor(stack, group);
  }
}
