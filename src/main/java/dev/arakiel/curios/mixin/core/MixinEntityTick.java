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

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.arakiel.curios.common.event.CuriosEventHandler;

/**
 * Runs the curio tick logic at the end of every entity tick.
 *
 * <p>This replaces NeoForge's {@code EntityTickEvent.Post}. Fabric has no per entity tick event, and
 * the curio logic has to run for every living entity on both sides, so a mixin is the closest
 * equivalent.</p>
 */
@Mixin(Entity.class)
public class MixinEntityTick {

  @Inject(method = "tick", at = @At("TAIL"))
  private void curios$tick(CallbackInfo ci) {

    if ((Object) this instanceof LivingEntity livingEntity) {
      CuriosEventHandler.tick(livingEntity);
    }
  }
}
