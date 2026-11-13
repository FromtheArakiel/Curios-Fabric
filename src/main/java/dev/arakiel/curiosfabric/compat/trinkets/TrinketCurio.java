package dev.arakiel.curiosfabric.compat.trinkets;

import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
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
    String path = TrinketsCompat.trinketsPathOf(curiosSlot);
    return path != null && TrinketsCompat.isTrinketFor(stack, path);
  }
}
