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

package dev.arakiel.curios.mixin.core;

import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.arakiel.curios.common.event.CuriosEventHandler;

/**
 * Mirrors NeoForge's {@code EnderManAngerEvent}: a worn curio can act as an ender mask and stop the
 * enderman from getting angry.
 */
@Mixin(EnderMan.class)
public class MixinEnderMan {

  @Inject(method = "isLookingAtMe", at = @At("HEAD"), cancellable = true)
  private void curios$isEnderMask(Player player, CallbackInfoReturnable<Boolean> cir) {

    if (CuriosEventHandler.hasEnderMask(player, (EnderMan) (Object) this)) {
      cir.setReturnValue(false);
    }
  }
}
