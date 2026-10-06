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

package dev.arakiel.curios.common.inventory;

import dev.arakiel.curios.api.type.inventory.IItemHandler;
import dev.arakiel.curios.api.type.inventory.IItemHandlerModifiable;
import javax.annotation.Nonnull;
import net.minecraft.world.item.ItemStack;

/**
 * Exposes several {@link IItemHandlerModifiable} instances as one contiguous handler.
 *
 * <p>Fabric replacement for NeoForge's {@code CombinedInvWrapper}.</p>
 */
public class CombinedInvWrapper implements IItemHandlerModifiable {

  protected final IItemHandlerModifiable[] itemHandler;
  protected final int[] baseIndex;
  protected final int slotCount;

  public CombinedInvWrapper(IItemHandlerModifiable... itemHandler) {
    this.itemHandler = itemHandler;
    this.baseIndex = new int[itemHandler.length];
    int index = 0;

    for (int i = 0; i < itemHandler.length; i++) {
      index += itemHandler[i].getSlots();
      this.baseIndex[i] = index;
    }
    this.slotCount = index;
  }

  protected int getIndexForSlot(int slot) {

    if (slot < 0) {
      return -1;
    }

    for (int i = 0; i < this.baseIndex.length; i++) {

      if (slot - this.baseIndex[i] < 0) {
        return i;
      }
    }
    return -1;
  }

  protected IItemHandlerModifiable getHandlerFromIndex(int index) {
    return index < 0 ? null : this.itemHandler[index];
  }

  protected int getSlotFromIndex(int slot, int index) {

    if (index <= 0 || index >= this.baseIndex.length) {
      return slot;
    }
    return slot - this.baseIndex[index - 1];
  }

  @Override
  public int getSlots() {
    return this.slotCount;
  }

  @Nonnull
  @Override
  public ItemStack getStackInSlot(int slot) {
    int index = this.getIndexForSlot(slot);
    return index < 0 ? ItemStack.EMPTY :
        this.getHandlerFromIndex(index).getStackInSlot(this.getSlotFromIndex(slot, index));
  }

  @Nonnull
  @Override
  public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
    int index = this.getIndexForSlot(slot);
    return index < 0 ? stack :
        this.getHandlerFromIndex(index)
            .insertItem(this.getSlotFromIndex(slot, index), stack, simulate);
  }

  @Nonnull
  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    int index = this.getIndexForSlot(slot);
    return index < 0 ? ItemStack.EMPTY :
        this.getHandlerFromIndex(index)
            .extractItem(this.getSlotFromIndex(slot, index), amount, simulate);
  }

  @Override
  public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
    int index = this.getIndexForSlot(slot);

    if (index >= 0) {
      this.getHandlerFromIndex(index).setStackInSlot(this.getSlotFromIndex(slot, index), stack);
    }
  }

  @Override
  public int getSlotLimit(int slot) {
    int index = this.getIndexForSlot(slot);
    return index < 0 ? 0 :
        this.getHandlerFromIndex(index).getSlotLimit(this.getSlotFromIndex(slot, index));
  }

  @Override
  public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
    int index = this.getIndexForSlot(slot);
    return index >= 0 && this.getHandlerFromIndex(index)
        .isItemValid(this.getSlotFromIndex(slot, index), stack);
  }

  /** The wrapped handlers, in order. */
  public IItemHandler[] getHandlers() {
    return this.itemHandler;
  }
}
