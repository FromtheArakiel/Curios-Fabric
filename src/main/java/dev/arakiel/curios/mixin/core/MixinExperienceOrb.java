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

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.arakiel.curios.mixin.CuriosUtilMixinHooks;

/**
 * Lets the Mending enchantment repair curios.
 *
 * <p>Vanilla's experience orb pickup only repairs items found in the player's own inventory, so the
 * curio slots are handled here. This replaces NeoForge's {@code PlayerXpEvent.PickupXp}, which the
 * NeoForge edition cancelled to run the same logic.</p>
 */
@Mixin(ExperienceOrb.class)
public class MixinExperienceOrb {

  @Shadow
  private int value;

  @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
  private void curios$curioMending(Player player, CallbackInfo ci) {

    if (!(player instanceof ServerPlayer serverPlayer) || player.takeXpDelay != 0) {
      return;
    }
    Optional<ItemStack> maybeStack = CuriosUtilMixinHooks.findMendingCurio(player);

    if (maybeStack.isEmpty()) {
      return;
    }
    ItemStack stack = maybeStack.get();
    ExperienceOrb orb = (ExperienceOrb) (Object) this;
    player.takeXpDelay = 2;
    player.take(orb, 1);
    int toRepair = Math.min(this.value * 2, stack.getDamageValue());
    this.value -= toRepair / 2;
    stack.setDamageValue(stack.getDamageValue() - toRepair);

    if (this.value > 0) {
      serverPlayer.giveExperiencePoints(this.value);
    }
    orb.discard();
    ci.cancel();
  }
}
