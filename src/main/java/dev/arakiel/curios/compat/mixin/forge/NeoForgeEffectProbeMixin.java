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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.arakiel.curios.compat.Diag;

/**
 * Diagnostic probe for the NeoForge mob effect hook.
 *
 * <p>{@code CommonHooks.canMobEffectBeApplied} is what posts {@code MobEffectEvent.Applicable}, the
 * event that Forge accessory mods use to block effects ("immune to poison" and friends). The probe
 * records the outcome once per effect so a missing immunity can be told apart from a missing
 * listener.</p>
 */
@Mixin(targets = "net.neoforged.neoforge.common.CommonHooks")
public class NeoForgeEffectProbeMixin {

  @Inject(
      method = "canMobEffectBeApplied(Lnet/minecraft/world/entity/LivingEntity;"
          + "Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
      at = @At("RETURN"))
  private static void curiosfabric$probeEffectApplication(LivingEntity entity,
                                                          MobEffectInstance instance,
                                                          Entity source,
                                                          CallbackInfoReturnable<Boolean> cir) {
    String effect = instance == null ? "?"
        : String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value()));
    Diag.once("effect-check:" + effect + ":" + cir.getReturnValue(),
        "NeoForge checked whether {} may be applied to {} -> {}",
        effect, entity == null ? "?" : entity.getType(), cir.getReturnValue());
  }
}
