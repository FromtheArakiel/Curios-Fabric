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

package dev.arakiel.curios.compat.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.arakiel.curios.compat.CompatTargets;

/**
 * Bans the accessory UI of the other implementations: this mod is the only accessory screen in the
 * installation.
 *
 * <p>Doing it at {@code Minecraft#setScreen} means the Forge/NeoForge Curios screen (loaded through
 * Kilt or Connector) and Trinkets' screen can never open, no matter which key binding or button
 * tries it, and without linking against any of those mods.</p>
 */
@Mixin(Minecraft.class)
public class MinecraftForeignScreensMixin {

  @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$banForeignAccessoryScreens(Screen screen, CallbackInfo ci) {

    if (screen != null && CompatTargets.isForeignAccessoryScreen(screen.getClass().getName())) {
      ci.cancel();
    }
  }
}
