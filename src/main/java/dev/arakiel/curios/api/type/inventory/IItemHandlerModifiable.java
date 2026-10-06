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

package dev.arakiel.curios.api.type.inventory;

import net.minecraft.world.item.ItemStack;

/** An {@link IItemHandler} whose contents can be replaced directly. */
public interface IItemHandlerModifiable extends IItemHandler {

  /**
   * Overrides the stack in the given slot. Implementations must not copy the stack.
   *
   * @param slot  The slot to change
   * @param stack The new stack, may be empty
   */
  void setStackInSlot(int slot, ItemStack stack);
}
