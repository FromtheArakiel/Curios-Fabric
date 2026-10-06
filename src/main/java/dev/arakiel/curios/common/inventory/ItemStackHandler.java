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

import dev.arakiel.curios.api.type.inventory.IItemHandlerModifiable;
import javax.annotation.Nonnull;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * A simple fixed size {@link IItemHandlerModifiable} backed by a {@link NonNullList}.
 *
 * <p>This is the Fabric replacement for NeoForge's {@code ItemStackHandler}, including the same
 * {@code Items}/{@code Size} NBT layout that Curios has always written.</p>
 */
public class ItemStackHandler implements IItemHandlerModifiable {

  protected NonNullList<ItemStack> stacks;

  public ItemStackHandler() {
    this(1);
  }

  public ItemStackHandler(int size) {
    this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
  }

  public ItemStackHandler(NonNullList<ItemStack> stacks) {
    this.stacks = stacks;
  }

  public void setSize(int size) {
    this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
  }

  /** The backing list, for serialization purposes. */
  public NonNullList<ItemStack> getStacks() {
    return this.stacks;
  }

  @Override
  public int getSlots() {
    return this.stacks.size();
  }

  @Nonnull
  @Override
  public ItemStack getStackInSlot(int slot) {
    this.validateSlotIndex(slot);
    return this.stacks.get(slot);
  }

  @Nonnull
  @Override
  public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {

    if (stack.isEmpty()) {
      return ItemStack.EMPTY;
    }

    if (!this.isItemValid(slot, stack)) {
      return stack;
    }
    this.validateSlotIndex(slot);
    ItemStack existing = this.stacks.get(slot);
    int limit = this.getStackLimit(slot, stack);

    if (!existing.isEmpty()) {

      if (!ItemStack.isSameItemSameComponents(stack, existing)) {
        return stack;
      }
      limit -= existing.getCount();
    }

    if (limit <= 0) {
      return stack;
    }
    boolean reachedLimit = stack.getCount() > limit;

    if (!simulate) {

      if (existing.isEmpty()) {
        this.stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
      } else {
        existing.grow(reachedLimit ? limit : stack.getCount());
      }
      this.onContentsChanged(slot);
    }
    return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
  }

  @Nonnull
  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {

    if (amount == 0) {
      return ItemStack.EMPTY;
    }
    this.validateSlotIndex(slot);
    ItemStack existing = this.stacks.get(slot);

    if (existing.isEmpty()) {
      return ItemStack.EMPTY;
    }
    int toExtract = Math.min(amount, existing.getMaxStackSize());

    if (existing.getCount() <= toExtract) {

      if (!simulate) {
        this.stacks.set(slot, ItemStack.EMPTY);
        this.onContentsChanged(slot);
        return existing;
      }
      return existing.copy();
    }

    if (!simulate) {
      this.stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
      this.onContentsChanged(slot);
    }
    return existing.copyWithCount(toExtract);
  }

  @Override
  public int getSlotLimit(int slot) {
    return 64;
  }

  protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
    return Math.min(this.getSlotLimit(slot), stack.getMaxStackSize());
  }

  @Override
  public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
    return true;
  }

  @Override
  public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
    this.validateSlotIndex(slot);

    if (ItemStack.matches(this.stacks.get(slot), stack)) {
      return;
    }
    this.stacks.set(slot, stack);
    this.onContentsChanged(slot);
  }

  protected void validateSlotIndex(int slot) {

    if (slot < 0 || slot >= this.stacks.size()) {
      throw new RuntimeException(
          "Slot " + slot + " not in valid range - [0," + this.stacks.size() + ")");
    }
  }

  /** Called whenever the contents of a slot change. */
  protected void onContentsChanged(int slot) {
    // NO-OP
  }

  /** Called after the handler has been deserialized. */
  protected void onLoad() {
    // NO-OP
  }

  public CompoundTag serializeNBT(HolderLookup.Provider provider) {
    ListTag nbtTagList = new ListTag();

    for (int i = 0; i < this.stacks.size(); i++) {

      if (!this.stacks.get(i).isEmpty()) {
        CompoundTag itemTag = new CompoundTag();
        itemTag.putInt("Slot", i);
        // 1.21 encodes an ItemStack through its codec. The two argument overload takes the tag as
        // the prefix to merge into and returns the resulting tag, so the return value is the one
        // that actually carries the item data. Adding the prefix again would persist a bare
        // {"Slot":n}, which is unreadable and made every equipped curio disappear on load.
        nbtTagList.add(this.stacks.get(i).save(provider, itemTag));
      }
    }
    CompoundTag nbt = new CompoundTag();
    nbt.put("Items", nbtTagList);
    nbt.putInt("Size", this.stacks.size());
    return nbt;
  }

  public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
    this.setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : this.stacks.size());
    ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);

    for (int i = 0; i < tagList.size(); i++) {
      CompoundTag itemTags = tagList.getCompound(i);
      int slot = itemTags.getInt("Slot");

      if (slot >= 0 && slot < this.stacks.size()) {
        ItemStack.parse(provider, itemTags).ifPresent(stack -> this.stacks.set(slot, stack));
      }
    }
    this.onLoad();
  }
}
