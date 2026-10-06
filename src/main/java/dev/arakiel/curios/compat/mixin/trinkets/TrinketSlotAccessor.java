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

package dev.arakiel.curios.compat.mixin.trinkets;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the compatibility layer move a foreign slot out of the visible screen area. */
@Mixin(Slot.class)
public interface TrinketSlotAccessor {

  @Mutable
  @Accessor("x")
  void curiosfabric$setX(int x);

  @Mutable
  @Accessor("y")
  void curiosfabric$setY(int y);
}
