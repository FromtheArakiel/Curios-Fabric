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

/**
 * A minimal, loader independent item handler.
 *
 * <p>The NeoForge edition of Curios exposes NeoForge's {@code IItemHandler} in its public API. The
 * Fabric edition cannot do that, so the same contract is declared here with the exact same method
 * names and semantics. Everything that only needs to enumerate or transfer stacks keeps working
 * unchanged, and no NeoForge code is required at runtime.</p>
 */
public interface IItemHandler {

  /** The number of slots in this handler. */
  int getSlots();

  /** The stack in the given slot, never {@code null} (use {@link ItemStack#EMPTY}). */
  ItemStack getStackInSlot(int slot);

  /**
   * Inserts a stack into the given slot.
   *
   * @param slot     The slot to insert into
   * @param stack    The stack to insert
   * @param simulate True to only simulate the insertion
   * @return The remainder of the stack that did not fit
   */
  ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

  /**
   * Extracts a stack from the given slot.
   *
   * @param slot     The slot to extract from
   * @param amount   The maximum amount to extract
   * @param simulate True to only simulate the extraction
   * @return The extracted stack, possibly empty
   */
  ItemStack extractItem(int slot, int amount, boolean simulate);

  /** The maximum stack size of the given slot. */
  int getSlotLimit(int slot);

  /** True if the given stack may be placed into the given slot. */
  boolean isItemValid(int slot, ItemStack stack);
}
