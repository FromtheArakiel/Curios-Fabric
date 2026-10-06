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

import com.mojang.datafixers.util.Pair;
import dev.arakiel.curios.api.type.inventory.IItemHandler;
import dev.arakiel.curios.api.type.inventory.IItemHandlerModifiable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A {@link Slot} backed by an {@link IItemHandler}.
 *
 * <p>Fabric replacement for NeoForge's {@code SlotItemHandler}. Besides the handler plumbing it also
 * carries the "empty slot" background sprite, which NeoForge exposes as
 * {@code Slot#setBackground}.</p>
 */
public class SlotItemHandler extends Slot {

  private final IItemHandler itemHandler;
  protected final int index;

  @Nullable
  private Pair<ResourceLocation, ResourceLocation> background;

  public SlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
    super(EmptyContainer.INSTANCE, index, xPosition, yPosition);
    this.itemHandler = itemHandler;
    this.index = index;
  }

  /**
   * Sets the sprite that is drawn when this slot is empty, matching NeoForge's method of the same
   * name.
   *
   * @param atlas   The texture atlas of the sprite
   * @param texture The sprite location inside the atlas
   */
  public void setBackground(ResourceLocation atlas, ResourceLocation texture) {
    this.background = Pair.of(atlas, texture);
  }

  @Nullable
  @Override
  public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
    return this.background;
  }

  /** The index of this slot inside its {@link IItemHandler}. */
  public int getSlotIndex() {
    return this.index;
  }

  @Override
  public boolean mayPlace(@Nonnull ItemStack stack) {
    return !stack.isEmpty() && this.itemHandler.isItemValid(this.index, stack);
  }

  @Nonnull
  @Override
  public ItemStack getItem() {
    return this.itemHandler.getStackInSlot(this.index);
  }

  @Override
  public boolean hasItem() {
    return !this.getItem().isEmpty();
  }

  @Override
  public void set(@Nonnull ItemStack stack) {

    if (this.itemHandler instanceof IItemHandlerModifiable modifiable) {
      modifiable.setStackInSlot(this.index, stack);
    }
    this.setChanged();
  }

  @Override
  public void setChanged() {
    // NO-OP: the handler notifies its own listeners
  }

  @Nonnull
  @Override
  public ItemStack remove(int amount) {
    return this.itemHandler.extractItem(this.index, amount, false);
  }

  @Override
  public int getMaxStackSize() {
    return this.itemHandler.getSlotLimit(this.index);
  }

  @Override
  public int getMaxStackSize(@Nonnull ItemStack stack) {
    return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
  }

  @Override
  public boolean mayPickup(@Nonnull Player player) {
    return true;
  }

  @Override
  public boolean allowModification(@Nonnull Player player) {
    return true;
  }
}
