/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package dev.arakiel.curios.compat.bridge;

import java.lang.reflect.Method;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.api.SlotContext;
import dev.arakiel.curios.api.type.capability.ICurioItem;

/**
 * Runs a Trinkets item's code hooks through this mod.
 *
 * <p>Trinkets content (for example Potion Ring) puts its behavior in {@code Trinket#tick},
 * {@code onEquip} and {@code onUnequip}. Items tagged for a slot only carry their attributes, so
 * those hooks have to be invoked explicitly while the item lives in a Curios slot. They are called
 * through reflection; the {@code SlotReference} argument is passed as {@code null}, which is fine
 * for the common implementations that only look at the stack and the wearer, and any failure is
 * caught so a broken hook cannot break the tick loop.</p>
 */
public class TrinketItemAdapter implements ICurioItem {

  private static final String TRINKET = "dev.emi.trinkets.api.Trinket";
  private static final String TRINKET_ITEM = "dev.emi.trinkets.api.TrinketItem";

  private final Item item;

  public TrinketItemAdapter(Item item) {
    this.item = item;
  }

  /** {@code true} when the item is a Trinkets trinket. */
  public static boolean isTrinket(Item item) {
    Class<?> trinket = dev.arakiel.curios.compat.CompatTargets.loadClass(TRINKET);

    if (trinket != null && trinket.isInstance(item)) {
      return true;
    }
    Class<?> trinketItem = dev.arakiel.curios.compat.CompatTargets.loadClass(TRINKET_ITEM);
    return trinketItem != null && trinketItem.isInstance(item);
  }

  private Object call(String name, Object... args) {

    try {

      for (Method method : this.item.getClass().getMethods()) {

        if (method.getName().equals(name) && method.getParameterCount() == args.length) {
          return method.invoke(this.item, args);
        }
      }
    } catch (Throwable throwable) {
      CuriosConstants.LOG.debug("Bridged Trinkets hook {} failed for {}", name, this.item,
          throwable);
    }
    return null;
  }

  @Override
  public void curioTick(SlotContext slotContext, ItemStack stack) {
    call("tick", stack, reference(slotContext), slotContext.entity());
  }

  @Override
  public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    call("onEquip", stack, reference(slotContext), slotContext.entity());
  }

  @Override
  public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    call("onUnequip", stack, reference(slotContext), slotContext.entity());
  }

  @Override
  public boolean canEquip(SlotContext slotContext, ItemStack stack) {
    Object result = call("canEquip", stack, reference(slotContext), slotContext.entity());
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
    Object result = call("canUnequip", stack, reference(slotContext), slotContext.entity());
    return !(result instanceof Boolean value) || value;
  }

  /** The real Trinkets slot reference of this slot, so hooks that inspect it keep working. */
  private Object reference(SlotContext slotContext) {
    return TrinketsApiBridge.slotReference(slotContext.identifier(), slotContext.index(),
        slotContext.entity());
  }

  @Override
  public boolean hasCurioCapability(ItemStack stack) {
    return true;
  }
}
