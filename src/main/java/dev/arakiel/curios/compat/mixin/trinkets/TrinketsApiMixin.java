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

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.arakiel.curios.compat.bridge.TrinketsApiBridge;

/**
 * Redirects Trinkets' component lookup to this mod's inventory.
 *
 * <p>Trinkets is handled data driven here: its item tags make an item a trinket for us, its GUI is
 * banned and its items are equipped through our right click handler. This mixin additionally makes
 * {@code TrinketsApi.getTrinketComponent} report the accessories that are actually worn, so mods
 * that only ask Trinkets whether something is equipped keep working.</p>
 */
@Mixin(targets = "dev.emi.trinkets.api.TrinketsApi")
public class TrinketsApiMixin {

  @Inject(method = "getTrinketComponent", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$getTrinketComponent(LivingEntity entity,
                                                       CallbackInfoReturnable<Optional<?>> cir) {
    Object bridged = TrinketsApiBridge.component(entity);

    if (bridged != null) {
      cir.setReturnValue(Optional.of(bridged));
    }
  }
}
