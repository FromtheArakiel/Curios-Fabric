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

package dev.arakiel.curios.common.util;

import dev.arakiel.curios.api.type.inventory.IItemHandler;
import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Utility methods for moving stacks in and out of an {@link IItemHandler}. */
public final class ItemHandlerHelper {

  private ItemHandlerHelper() {
  }

  /** True if the two stacks can be merged into a single stack. */
  public static boolean canItemStacksStack(@Nonnull ItemStack a, @Nonnull ItemStack b) {
    return !a.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
  }

  /** A copy of the stack with the given size, or an empty stack when the size is zero. */
  public static ItemStack copyStackWithSize(@Nonnull ItemStack stack, int size) {
    return size == 0 ? ItemStack.EMPTY : stack.copyWithCount(size);
  }

  /**
   * Inserts a stack into the first slot that accepts it.
   *
   * @param dest     The handler to insert into
   * @param stack    The stack to insert
   * @param simulate True to only simulate the insertion
   * @return The remainder that did not fit
   */
  @Nonnull
  public static ItemStack insertItem(IItemHandler dest, @Nonnull ItemStack stack,
                                     boolean simulate) {

    if (dest == null || stack.isEmpty()) {
      return stack;
    }

    for (int i = 0; i < dest.getSlots(); i++) {
      stack = dest.insertItem(i, stack, simulate);

      if (stack.isEmpty()) {
        return ItemStack.EMPTY;
      }
    }
    return stack;
  }

  /**
   * Inserts a stack, merging into existing stacks before using empty slots.
   *
   * @param inventory The handler to insert into
   * @param stack     The stack to insert
   * @param simulate  True to only simulate the insertion
   * @return The remainder that did not fit
   */
  @Nonnull
  public static ItemStack insertItemStacked(IItemHandler inventory, @Nonnull ItemStack stack,
                                            boolean simulate) {

    if (inventory == null || stack.isEmpty()) {
      return stack;
    }

    for (int i = 0; i < inventory.getSlots(); i++) {
      ItemStack existing = inventory.getStackInSlot(i);

      if (!existing.isEmpty() && canItemStacksStack(stack, existing)) {
        stack = inventory.insertItem(i, stack, simulate);

        if (stack.isEmpty()) {
          return ItemStack.EMPTY;
        }
      }
    }
    return stack;
  }

  /**
   * Adds a stack into the player's inventory, dropping the remainder in front of the player when it
   * does not fit.
   *
   * @param player The player to give the stack to
   * @param stack  The stack
   */
  public static void giveItemToPlayer(Player player, @Nonnull ItemStack stack) {

    if (stack.isEmpty()) {
      return;
    }
    ItemStack copy = stack.copy();
    player.getInventory().add(copy);

    if (!copy.isEmpty()) {
      player.drop(copy, false);
    }
  }
}
