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
import dev.arakiel.curios.CuriosConstants;

/**
 * Keeps Trinkets from registering its own client side: key binding, screens and network receivers.
 *
 * <p>Trinkets draws its slots inside the vanilla inventory screen, so banning the screen swap alone
 * is not enough - the panel is neutralized in {@link TrinketScreenManagerMixin} and its key binding
 * is never registered here.</p>
 */
@Mixin(targets = "dev.emi.trinkets.TrinketsClient")
public class TrinketsClientMixin {

  @Inject(method = "onInitializeClient", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$disableTrinketsClient(CallbackInfo ci) {
    CuriosConstants.LOG.info(
        "Curios (Fabric) took over the accessory system: Trinkets' own client side is disabled");
    ci.cancel();
  }
}
