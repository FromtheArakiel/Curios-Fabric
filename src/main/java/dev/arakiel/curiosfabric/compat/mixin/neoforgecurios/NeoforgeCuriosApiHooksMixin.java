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

package dev.arakiel.curiosfabric.compat.mixin.neoforgecurios;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.compat.bridge.NeoforgeCuriosApiBridge;

/**
 * Redirects the NeoForge edition's lookup helpers to this mod's data.
 *
 * <p>The NeoForge API classes of Curios are stubs whose bodies are filled in by that mod's own mixins,
 * so injecting into its hook class makes its CuriosApi answer with our inventory, our curios and
 * our slot assignment. That is what lets a NeoForge mod written against Curios see the accessories the
 * player actually wears in this mod.</p>
 */
@Mixin(targets = "top.theillusivec4.curios.mixin.CuriosImplMixinHooks")
public class NeoforgeCuriosApiHooksMixin {

  @Inject(method = "getCuriosInventory", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$getCuriosInventory(LivingEntity livingEntity,
                                                      CallbackInfoReturnable<Optional<?>> cir) {
    Object bridged = NeoforgeCuriosApiBridge.inventory(livingEntity);

    if (bridged != null) {
      cir.setReturnValue(Optional.of(bridged));
    }
  }

  @Inject(method = "getCurio", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$getCurio(ItemStack stack,
                                            CallbackInfoReturnable<Optional<?>> cir) {
    Object bridged = NeoforgeCuriosApiBridge.curio(stack);

    if (bridged != null) {
      cir.setReturnValue(Optional.of(bridged));
    }
  }

  @Inject(
      method = "getItemStackSlots(Lnet/minecraft/world/item/ItemStack;Z)Ljava/util/Map;",
      at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$getItemStackSlotsClient(ItemStack stack, boolean isClient,
                                                           CallbackInfoReturnable<Map<?, ?>> cir) {
    cir.setReturnValue(slotTypes(CuriosApi.getItemStackSlots(stack, isClient)));
  }

  @Inject(
      method = "getItemStackSlots(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Map;",
      at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$getItemStackSlotsEntity(ItemStack stack,
                                                           LivingEntity livingEntity,
                                                           CallbackInfoReturnable<Map<?, ?>> cir) {
    cir.setReturnValue(slotTypes(CuriosApi.getItemStackSlots(stack, livingEntity)));
  }

  private static Map<String, Object> slotTypes(Map<String, ISlotType> ours) {
    Map<String, Object> bridged = new LinkedHashMap<>();
    ours.forEach((id, slotType) -> bridged.put(id, NeoforgeCuriosApiBridge.slotType(slotType)));
    return bridged;
  }
}
