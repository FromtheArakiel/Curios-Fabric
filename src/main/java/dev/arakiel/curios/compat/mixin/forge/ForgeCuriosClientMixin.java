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

/** Disables the Forge edition's client side: key binding, menu screen and render layers. */
@Mixin(targets = "top.theillusivec4.curios.Curios$ClientProxy")
public class ForgeCuriosClientMixin {

  @Inject(method = "registerKeys", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noKeys(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "setupClient", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noClientSetup(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "registerMenuScreens", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noScreens(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "addLayers", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noLayers(CallbackInfo ci) {
    ci.cancel();
  }
}
