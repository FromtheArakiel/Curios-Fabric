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

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.arakiel.curios.common.event.CuriosEventHandler;

/**
 * Mirrors NeoForge's {@code LivingEquipmentChangeEvent}.
 *
 * <p>Fabric only exposes the equipment change event on the server and only for players, while Curios
 * needs it for every living entity on both sides (armour can carry curio slot modifiers). Therefore
 * the vanilla {@code onEquipItem} hook is used instead.</p>
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntityEquipment {

  @Inject(method = "onEquipItem", at = @At("TAIL"))
  private void curios$onEquipItem(EquipmentSlot slot, ItemStack from, ItemStack to,
                                  CallbackInfo ci) {
    CuriosEventHandler.livingEquipmentChange((LivingEntity) (Object) this, slot, from, to);
  }
}
