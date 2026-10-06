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

import dev.arakiel.curios.common.capability.CurioInventory;
import dev.arakiel.curios.common.capability.CuriosInventoryHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the curios inventory storage to every living entity and persists it in the entity's NBT.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntityInventory implements CuriosInventoryHolder {

  @Unique
  private CurioInventory curios$inventory;

  @Override
  public CurioInventory curios$getInventory() {

    if (this.curios$inventory == null) {
      this.curios$inventory = new CurioInventory();
    }
    return this.curios$inventory;
  }

  @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
  private void curios$saveInventory(CompoundTag tag, CallbackInfo ci) {

    if (this.curios$inventory != null) {
      tag.put("CuriosInventory",
          this.curios$inventory.serializeNBT(((LivingEntity) (Object) this).registryAccess()));
    }
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void curios$readInventory(CompoundTag tag, CallbackInfo ci) {

    if (tag.contains("CuriosInventory", Tag.TAG_COMPOUND)) {
      this.curios$getInventory()
          .deserializeNBT(((LivingEntity) (Object) this).registryAccess(),
              tag.getCompound("CuriosInventory"));
    }
  }
}
