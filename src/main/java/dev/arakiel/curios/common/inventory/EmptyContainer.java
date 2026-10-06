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

import javax.annotation.Nonnull;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A {@link Container} that holds nothing.
 *
 * <p>{@code Slot} requires a container in its constructor, but {@link SlotItemHandler} never
 * delegates to it. This is the Fabric equivalent of NeoForge's private {@code emptyInventory}
 * instance.</p>
 */
public final class EmptyContainer implements Container {

  public static final EmptyContainer INSTANCE = new EmptyContainer();

  private EmptyContainer() {
  }

  @Override
  public int getContainerSize() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Nonnull
  @Override
  public ItemStack getItem(int index) {
    return ItemStack.EMPTY;
  }

  @Nonnull
  @Override
  public ItemStack removeItem(int index, int count) {
    return ItemStack.EMPTY;
  }

  @Nonnull
  @Override
  public ItemStack removeItemNoUpdate(int index) {
    return ItemStack.EMPTY;
  }

  @Override
  public void setItem(int index, @Nonnull ItemStack stack) {
    // NO-OP
  }

  @Override
  public void setChanged() {
    // NO-OP
  }

  @Override
  public boolean stillValid(@Nonnull Player player) {
    return false;
  }

  @Override
  public void clearContent() {
    // NO-OP
  }
}
