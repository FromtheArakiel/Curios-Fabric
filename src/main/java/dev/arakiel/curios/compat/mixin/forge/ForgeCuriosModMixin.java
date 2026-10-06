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
import dev.arakiel.curios.CuriosConstants;

/**
 * Neutralizes the Forge/NeoForge edition of Curios.
 *
 * <p>Its mod constructor cannot be cancelled (Mixin rejects cancellable constructor injections, and
 * that mistake used to crash Kilt setups), so every registration step it performs is cancelled
 * individually instead: registry bootstrap, capabilities, network, setup, server hooks and
 * commands. The client side is handled by {@link ForgeCuriosClientMixin}. What remains is its API
 * stub layer, which is exactly what we want: Forge mods keep resolving CuriosApi and
 * {@link ForgeCuriosApiHooksMixin} makes those calls answer with this mod's data.</p>
 */
@Mixin(targets = "top.theillusivec4.curios.Curios")
public class ForgeCuriosModMixin {

  @Inject(method = "registerCaps", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noCaps(CallbackInfo ci) {
    CuriosConstants.LOG.info("Curios (Fabric) took over the accessory system: disabled the "
        + "Forge/NeoForge Curios capability registration");
    ci.cancel();
  }

  @Inject(method = "registerPayloadHandler", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noNetwork(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "setup", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noSetup(CallbackInfo ci) {
    CuriosConstants.LOG.info("Curios (Fabric) took over the accessory system: disabled the "
        + "Forge/NeoForge Curios setup");
    ci.cancel();
  }

  @Inject(method = "process", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noProcess(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "serverAboutToStart", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noServerStart(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "serverStopped", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noServerStop(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "registerCommands", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noCommands(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "reload", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noReload(CallbackInfo ci) {
    ci.cancel();
  }
}
