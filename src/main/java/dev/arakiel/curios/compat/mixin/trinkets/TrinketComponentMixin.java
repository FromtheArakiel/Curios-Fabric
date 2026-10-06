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

import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disconnects Trinkets' own inventories from the accessory slots.
 *
 * <p>Trinkets' {@code PlayerScreenHandler} mixin builds its slots from
 * {@code TrinketComponent.getGroups()} and only adds a group when
 * {@code TrinketComponent.getInventory()} reports inventories for it. Reporting both as empty is
 * what actually removes the trinket slots from the vanilla inventory screen: the manager that draws
 * and handles them was already neutralised, but the slots themselves were still created from the
 * component and therefore still took part in shift-clicking and its own auto-equip logic.</p>
 *
 * <p>The values are freshly allocated mutable maps because foreign code is known to clear the
 * collections it receives, which previously caused {@code UnsupportedOperationException} with the
 * immutable collection factories.</p>
 */
@Mixin(targets = "dev.emi.trinkets.api.LivingEntityTrinketComponent")
public class TrinketComponentMixin {

  @Inject(method = "getGroups", at = @At("HEAD"), cancellable = true, remap = false)
  private void curiosfabric$emptyGroups(CallbackInfoReturnable<Map<String, Object>> cir) {
    cir.setReturnValue(new HashMap<>());
  }

  @Inject(method = "getInventory", at = @At("HEAD"), cancellable = true, remap = false)
  private void curiosfabric$emptyInventory(CallbackInfoReturnable<Map<String, Object>> cir) {
    cir.setReturnValue(new HashMap<>());
  }
}
