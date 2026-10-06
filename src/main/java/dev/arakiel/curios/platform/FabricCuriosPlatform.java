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

package dev.arakiel.curios.platform;

import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.type.ISlotType;
import dev.arakiel.curios.platform.services.ICuriosPlatform;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Fabric implementation of the platform service.
 *
 * <p>The NeoForge build delegates to NeoForge's item extension hooks; Fabric has no equivalent, so
 * the vanilla behaviour is reproduced from item tags and from the vanilla item checks. Mods can add
 * their items to the matching tags to opt in.</p>
 */
public class FabricCuriosPlatform implements ICuriosPlatform {

  private static final TagKey<Item> PIGLIN_LOVED =
      TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("piglin_loved"));
  private static final TagKey<Item> FREEZE_IMMUNE_WEARABLES =
      TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("freeze_immune_wearables"));

  @Override
  public Map<String, ISlotType> getItemStackSlots(ItemStack stack, @Nullable LivingEntity livingEntity) {
    return CuriosApi.getItemStackSlots(stack, livingEntity != null ? livingEntity.level() : null);
  }

  @Override
  public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity livingEntity) {
    return stack.is(PIGLIN_LOVED) || stack.is(Items.GOLD_INGOT) || stack.is(Items.GOLD_NUGGET);
  }

  @Override
  public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity livingEntity) {
    return stack.is(FREEZE_IMMUNE_WEARABLES) || stack.is(Items.LEATHER_BOOTS);
  }

  @Override
  public boolean isEnderMask(ItemStack stack, Player player, EnderMan enderMan) {
    // Vanilla's only disguise is the carved pumpkin; mods opt in through ICurio#isEnderMask, which
    // is evaluated by the enderman mixin instead of this generic stack check.
    return stack.is(Items.CARVED_PUMPKIN);
  }
}
