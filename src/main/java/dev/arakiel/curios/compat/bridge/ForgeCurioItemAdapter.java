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

package dev.arakiel.curios.compat.bridge;

import com.google.common.collect.Multimap;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.api.SlotContext;
import dev.arakiel.curios.api.type.capability.ICurio;
import dev.arakiel.curios.api.type.capability.ICurioItem;

/**
 * Runs a Forge/NeoForge {@code ICurioItem}'s code hooks through this mod.
 *
 * <p>This is the part a data driven bridge cannot cover: a Forge mod's curio usually implements the
 * Forge {@code ICurioItem} interface and puts its logic in {@code curioTick}, {@code onEquip},
 * {@code canEquip}, drop rules and so on. Those methods are invoked here through reflection, with
 * the slot context translated between the two APIs, so the behavior of such an item keeps working
 * while its data lives in this mod's slots.</p>
 */
public class ForgeCurioItemAdapter implements ICurioItem {

  private static final String FOREIGN = "top.theillusivec4.curios.api.type.capability.ICurioItem";

  private final Item item;

  public ForgeCurioItemAdapter(Item item) {
    this.item = item;
  }

  /** {@code true} when the item implements the Forge edition's curio interface. */
  public static boolean isForgeCurioItem(Item item) {
    Class<?> type = dev.arakiel.curios.compat.CompatTargets.loadClass(FOREIGN);
    return type != null && type.isInstance(item);
  }

  private Object call(String name, int parameterCount, Object... args) {

    try {

      for (Method method : this.item.getClass().getMethods()) {

        if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
          return method.invoke(this.item, args);
        }
      }
    } catch (Throwable throwable) {
      CuriosConstants.LOG.debug("Bridged curio hook {} failed for {}",
          name, this.item, throwable);
    }
    return null;
  }

  private Object context(SlotContext slotContext) {
    return ForgeApiBridge.foreignContext(slotContext);
  }

  @Override
  public boolean hasCurioCapability(ItemStack stack) {
    Object result = call("hasCurioCapability", 1, stack);
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public void curioTick(SlotContext slotContext, ItemStack stack) {
    call("curioTick", 2, context(slotContext), stack);
  }

  @Override
  public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    call("onEquip", 3, context(slotContext), prevStack, stack);
  }

  @Override
  public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    call("onUnequip", 3, context(slotContext), newStack, stack);
  }

  @Override
  public boolean canEquip(SlotContext slotContext, ItemStack stack) {
    Object result = call("canEquip", 2, context(slotContext), stack);
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
    Object result = call("canUnequip", 2, context(slotContext), stack);
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public void onEquipFromUse(SlotContext slotContext, ItemStack stack) {
    call("onEquipFromUse", 2, context(slotContext), stack);
  }

  @Override
  public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
    Object result = call("canEquipFromUse", 2, context(slotContext), stack);
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public void curioBreak(SlotContext slotContext, ItemStack stack) {
    call("curioBreak", 2, context(slotContext), stack);
  }

  @Override
  public boolean canSync(SlotContext slotContext, ItemStack stack) {
    return Boolean.TRUE.equals(call("canSync", 2, context(slotContext), stack));
  }

  @Override
  public CompoundTag writeSyncData(SlotContext slotContext, ItemStack stack) {
    Object result = call("writeSyncData", 2, context(slotContext), stack);
    return result instanceof CompoundTag tag ? tag : new CompoundTag();
  }

  @Override
  public void readSyncData(SlotContext slotContext, CompoundTag compound, ItemStack stack) {
    call("readSyncData", 3, context(slotContext), compound, stack);
  }

  @Override
  public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                     int lootingLevel, boolean recentlyHit, ItemStack stack) {
    Object result = call("getDropRule", 5, context(slotContext), source, lootingLevel, recentlyHit,
        stack);

    if (result instanceof Enum<?> value) {

      try {
        return ICurio.DropRule.valueOf(value.name());
      } catch (IllegalArgumentException exception) {
        return ICurio.DropRule.DEFAULT;
      }
    }
    return ICurio.DropRule.DEFAULT;
  }

  @Override
  public int getFortuneLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
    Object result = call("getFortuneLevel", 3, context(slotContext), lootContext, stack);
    return result instanceof Integer value ? value : 0;
  }

  @Override
  public int getLootingLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
    Object result = call("getLootingLevel", 3, context(slotContext), lootContext, stack);
    return result instanceof Integer value ? value : 0;
  }

  @Override
  public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
      SlotContext slotContext, ResourceLocation id, ItemStack stack) {
    Object result = call("getAttributeModifiers", 3, context(slotContext), id, stack);

    if (result instanceof Multimap<?, ?> multimap) {
      @SuppressWarnings("unchecked")
      Multimap<Holder<Attribute>, AttributeModifier> cast = (Multimap<Holder<Attribute>, AttributeModifier>) multimap;
      return cast;
    }
    return com.google.common.collect.LinkedHashMultimap.create();
  }

  @Override
  public boolean makesPiglinsNeutral(SlotContext slotContext, ItemStack stack) {
    return Boolean.TRUE.equals(call("makesPiglinsNeutral", 2, context(slotContext), stack));
  }

  @Override
  public boolean canWalkOnPowderedSnow(SlotContext slotContext, ItemStack stack) {
    return Boolean.TRUE.equals(call("canWalkOnPowderedSnow", 2, context(slotContext), stack));
  }

  @Override
  public boolean isEnderMask(SlotContext slotContext, EnderMan enderMan, ItemStack stack) {
    return Boolean.TRUE.equals(call("isEnderMask", 3, context(slotContext), enderMan, stack));
  }

  @Override
  public List<net.minecraft.network.chat.Component> getSlotsTooltip(
      List<net.minecraft.network.chat.Component> tooltips, ItemStack stack) {
    Object result = call("getSlotsTooltip", 2, tooltips, stack);
    return result instanceof List<?> list ? castComponents(list) : tooltips;
  }

  private static List<net.minecraft.network.chat.Component> castComponents(List<?> list) {
    List<net.minecraft.network.chat.Component> result = new ArrayList<>();

    for (Object element : list) {

      if (element instanceof net.minecraft.network.chat.Component component) {
        result.add(component);
      }
    }
    return result;
  }
}
