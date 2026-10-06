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

package dev.arakiel.curios.compat.mixin.forge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips the Forge edition's registry bootstrap: entity attachment, capabilities, the Curios menu,
 * the loot function, the criterion trigger and its data component. Without it the foreign edition
 * registers no ids at all and never owns any accessory data.
 */
@Mixin(targets = "top.theillusivec4.curios.common.CuriosRegistry")
public class ForgeCuriosRegistryMixin {

  @Inject(method = "init", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noRegistryInit(CallbackInfo ci) {
    ci.cancel();
  }
}
