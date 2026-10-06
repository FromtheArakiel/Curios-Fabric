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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes Trinkets' panel from the vanilla inventory screen.
 *
 * <p>Trinkets hooks its slot groups into {@code InventoryScreen} through this manager instead of
 * opening a screen of its own, which is why a screen swap check alone does not hide it. Every entry
 * point (layout, drawing, ticking and the click bounds) is neutralized here, so nothing of its UI
 * is visible or clickable while this mod's own button and screen stay untouched.</p>
 */
@Mixin(targets = "dev.emi.trinkets.TrinketScreenManager")
public class TrinketScreenManagerMixin {

  @Inject(method = "init", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noInit(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "update", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noUpdate(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noTick(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "drawActiveGroup", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noDrawActive(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "drawExtraGroups", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noDrawExtra(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "drawGroup", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noDrawGroup(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "removeSelections", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noSelections(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "isClickInsideTrinketBounds", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noClickBounds(CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(false);
  }
}
