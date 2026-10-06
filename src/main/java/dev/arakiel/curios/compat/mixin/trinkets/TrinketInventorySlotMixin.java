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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves Trinkets' survival inventory slots out of the visible inventory.
 *
 * <p>Trinkets injects those slots into the vanilla player screen handler, so the vanilla slot loop
 * draws them even when the Trinkets panel itself is neutralized. Pushing them far off screen makes
 * them invisible and non hoverable.</p>
 */
@Mixin(targets = "dev.emi.trinkets.SurvivalTrinketSlot")
public class TrinketInventorySlotMixin {

  @Inject(method = "<init>", at = @At("TAIL"))
  private void curiosfabric$hideSlot(CallbackInfo ci) {
    TrinketSlotAccessor accessor = (TrinketSlotAccessor) (Slot) (Object) this;
    accessor.curiosfabric$setX(-10000);
    accessor.curiosfabric$setY(-10000);
  }
}
