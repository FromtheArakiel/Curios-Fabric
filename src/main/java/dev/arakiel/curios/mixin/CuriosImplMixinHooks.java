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

package dev.arakiel.curios.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.api.CurioAttributeModifiers;
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.CuriosTags;
import dev.arakiel.curios.api.SlotAttribute;
import dev.arakiel.curios.api.SlotContext;
import dev.arakiel.curios.api.SlotResult;
import dev.arakiel.curios.api.event.CurioAttributeModifierEvent;
import dev.arakiel.curios.api.event.CuriosEventBus;
import dev.arakiel.curios.api.type.ISlotType;
import dev.arakiel.curios.api.type.capability.ICurio;
import dev.arakiel.curios.api.type.capability.ICurioItem;
import dev.arakiel.curios.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curios.common.CuriosRegistry;
import dev.arakiel.curios.common.capability.CurioInventoryCapability;
import dev.arakiel.curios.common.capability.ItemizedCurioCapability;
import dev.arakiel.curios.common.data.CuriosEntityManager;
import dev.arakiel.curios.common.data.CuriosSlotManager;
import dev.arakiel.curios.common.network.NetworkHandler;
import dev.arakiel.curios.common.network.server.SPacketBreak;
import dev.arakiel.curios.compat.forge.ForgeCuriosCompat;
import dev.arakiel.curios.compat.trinkets.TrinketsCompat;

/**
 * Holds the actual implementations of the {@link CuriosApi} methods.
 *
 * <p>The class name is a leftover of the multiloader layout, where the API was a stub that these
 * hooks filled in through a mixin. The Fabric build calls them directly, but the name is kept so
 * that third party patches (for example the compatibility layers) keep working.</p>
 */
public class CuriosImplMixinHooks {

  private static final Map<Item, ICurioItem> REGISTRY = new ConcurrentHashMap<>();

  public static void registerCurio(Item item, ICurioItem icurio) {
    REGISTRY.put(item, icurio);
  }

  public static Optional<ICurioItem> getCurioFromRegistry(Item item) {
    return Optional.ofNullable(REGISTRY.get(item));
  }

  public static Map<String, ISlotType> getSlots(boolean isClient) {
    CuriosSlotManager slotManager = isClient ? CuriosSlotManager.CLIENT : CuriosSlotManager.SERVER;
    return slotManager.getSlots();
  }

  public static Map<String, ISlotType> getEntitySlots(EntityType<?> type, boolean isClient) {
    CuriosEntityManager entityManager =
        isClient ? CuriosEntityManager.CLIENT : CuriosEntityManager.SERVER;
    return entityManager.getEntitySlots(type);
  }

  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack, boolean isClient) {
    Map<String, ISlotType> slots = filteredSlots(slotType -> {
      SlotContext slotContext = new SlotContext(slotType.getIdentifier(), null, 0, false, true);
      SlotResult slotResult = new SlotResult(slotContext, stack);
      return CuriosApi.testCurioPredicates(slotType.getValidators(), slotResult);
    }, CuriosApi.getSlots(isClient));
    addCompatSlots(stack, slots, CuriosApi.getSlots(isClient));
    return slots;
  }

  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack,
                                                         LivingEntity livingEntity) {
    Map<String, ISlotType> allSlots = CuriosApi.getEntitySlots(livingEntity);
    Map<String, ISlotType> slots = filteredSlots(slotType -> {
      SlotContext slotContext =
          new SlotContext(slotType.getIdentifier(), livingEntity, 0, false, true);
      SlotResult slotResult = new SlotResult(slotContext, stack);
      return CuriosApi.testCurioPredicates(slotType.getValidators(), slotResult);
    }, allSlots);
    addCompatSlots(stack, slots, allSlots);
    return slots;
  }

  /**
   * Adds the slots that the compatibility bridges map an item to.
   *
   * <p>Items from Trinkets or from the legacy Forge edition are not listed in the
   * {@code #curios:<slot>} item tags, so their slot assignment is inferred from the foreign tags
   * here. Otherwise they would fail the validators of every slot type.</p>
   */
  private static void addCompatSlots(ItemStack stack, Map<String, ISlotType> result,
                                     Map<String, ISlotType> allSlots) {

    if (!TrinketsCompat.isLoaded() && !ForgeCuriosCompat.isLoaded()) {
      return;
    }

    for (String slotId : TrinketsCompat.slotsFor(stack)) {
      ISlotType slotType = allSlots.get(slotId);

      if (slotType != null) {
        result.putIfAbsent(slotId, slotType);
      }
    }

    for (String slotId : ForgeCuriosCompat.slotsFor(stack)) {
      ISlotType slotType = allSlots.get(slotId);

      if (slotType != null) {
        result.putIfAbsent(slotId, slotType);
      }
    }
  }

  private static Map<String, ISlotType> filteredSlots(Predicate<ISlotType> filter,
                                                      Map<String, ISlotType> map) {
    Map<String, ISlotType> result = new HashMap<>();

    for (Map.Entry<String, ISlotType> entry : map.entrySet()) {
      ISlotType slotType = entry.getValue();

      if (filter.test(slotType)) {
        result.put(entry.getKey(), slotType);
      }
    }
    return result;
  }

  /**
   * Looks up the curio behaviour of a stack. This replaces the NeoForge item capability: a
   * registered {@link ICurioItem} wins over an {@link ICurioItem} implemented by the item itself.
   */
  public static Optional<ICurio> getCurio(ItemStack stack) {

    if (stack == null || stack.isEmpty()) {
      return Optional.empty();
    }
    ICurioItem curioItem = getCurioFromRegistry(stack.getItem()).orElse(null);

    if (curioItem == null && stack.getItem() instanceof ICurioItem itemCurio) {
      curioItem = itemCurio;
    }

    if (curioItem == null) {
      // Real Trinkets content (Potion Ring and friends) keeps its behavior in Trinket#tick and the
      // equip hooks, so it has to win over the plain data driven bridge.
      if (TrinketsCompat.isLoaded()
          && dev.arakiel.curios.compat.bridge.TrinketItemAdapter.isTrinket(stack.getItem())) {
        curioItem = new dev.arakiel.curios.compat.bridge.TrinketItemAdapter(stack.getItem());
      }
    }

    if (curioItem == null) {
      curioItem = TrinketsCompat.lookup(stack).orElse(null);
    }

    if (curioItem == null) {
      // A Forge mod's curio implements the Forge edition's ICurioItem, which carries its code
      // hooks: it has to win over the plain data driven bridge, exactly like the Trinkets case.
      if (ForgeCuriosCompat.isLoaded()
          && dev.arakiel.curios.compat.bridge.ForgeCurioItemAdapter
          .isForgeCurioItem(stack.getItem())) {
        curioItem = new dev.arakiel.curios.compat.bridge.ForgeCurioItemAdapter(stack.getItem());
      }
    }

    if (curioItem == null) {
      curioItem = ForgeCuriosCompat.lookup(stack).orElse(null);
    }

    // A Trinkets item keeps its behavior in Trinket#tick/onEquip/onUnequip: run those hooks too.
    if (curioItem == null && TrinketsCompat.isLoaded()
        && dev.arakiel.curios.compat.bridge.TrinketItemAdapter.isTrinket(stack.getItem())) {
      curioItem = new dev.arakiel.curios.compat.bridge.TrinketItemAdapter(stack.getItem());
    }

    if (curioItem != null && curioItem.hasCurioCapability(stack)) {
      return Optional.of(new ItemizedCurioCapability(curioItem, stack));
    }
    return Optional.empty();
  }

  public static Optional<ICuriosItemHandler> getCuriosInventory(LivingEntity livingEntity) {

    if (livingEntity == null) {
      return Optional.empty();
    }

    if (CuriosApi.getEntitySlots(livingEntity).isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new CurioInventoryCapability(livingEntity));
  }

  public static boolean isStackValid(SlotContext slotContext, ItemStack stack) {
    String id = slotContext.identifier();
    LivingEntity entity = slotContext.entity();
    Map<String, ISlotType> map;

    if (entity != null) {
      map = getItemStackSlots(stack, entity);
    } else {
      map = getItemStackSlots(stack, CuriosConstants.IS_CLIENT);
    }
    Set<String> slots = map.keySet();

    if (!slots.isEmpty()) {
      return id.equals("curio") || slots.contains(id) || slots.contains("curio");
    } else if (id.equals("curio")) {
      // If there are no slots available to confirm validity for the generic curio slot,
      // perform fallback checks

      // tags
      if (stack.getTags()
          .anyMatch(tagKey -> CuriosConstants.isCuriosNamespace(
              tagKey.location().getNamespace()))) {
        return true;
      }

      // predicates
      Map<String, ISlotType> allSlots = CuriosApi.getSlots(false);
      SlotResult slotResult = new SlotResult(slotContext, stack);

      for (Map.Entry<String, ISlotType> entry : allSlots.entrySet()) {
        ISlotType slotType = entry.getValue();

        for (ResourceLocation validator : slotType.getValidators()) {

          if (CuriosApi.getCurioPredicate(validator).map(val -> val.test(slotResult))
              .orElse(false)) {
            return true;
          }
        }
      }

      // capability
      return CuriosApi.getCurio(stack).isPresent();
    }
    return false;
  }

  public static Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
      SlotContext slotContext, ResourceLocation id, ItemStack stack) {
    Multimap<Holder<Attribute>, AttributeModifier> multimap = LinkedHashMultimap.create();
    CurioAttributeModifiers attributemodifiers =
        stack.getOrDefault(CuriosRegistry.CURIO_ATTRIBUTE_MODIFIERS, CurioAttributeModifiers.EMPTY);

    if (!attributemodifiers.modifiers().isEmpty()) {

      for (CurioAttributeModifiers.Entry modifier : attributemodifiers.modifiers()) {

        if (modifier.slot().equals(slotContext.identifier())) {
          ResourceLocation rl = modifier.attribute();
          AttributeModifier attributeModifier = modifier.modifier();

          if (rl != null) {
            AttributeModifier.Operation operation = attributeModifier.operation();
            double amount = attributeModifier.amount();

            if (CuriosConstants.isCuriosNamespace(rl.getNamespace())) {
              String identifier1 = rl.getPath();
              LivingEntity livingEntity = slotContext.entity();
              boolean clientSide = livingEntity == null || livingEntity.level().isClientSide();

              if (CuriosApi.getSlot(identifier1, clientSide).isPresent()) {
                CuriosApi.addSlotModifier(multimap, identifier1, id, amount, operation);
              }
            } else {
              Holder<Attribute> attribute =
                  BuiltInRegistries.ATTRIBUTE.getHolder(rl).orElse(null);

              if (attribute != null) {
                multimap.put(attribute, new AttributeModifier(id, amount, operation));
              }
            }
          }
        }
      }
    } else {
      multimap = getCurio(stack).map(curio -> curio.getAttributeModifiers(slotContext, id))
          .orElse(multimap);
    }
    CurioAttributeModifierEvent evt =
        new CurioAttributeModifierEvent(stack, slotContext, id, multimap);
    CuriosEventBus.post(evt);
    return LinkedHashMultimap.create(evt.getModifiers());
  }

  public static void addSlotModifier(Multimap<Holder<Attribute>, AttributeModifier> map,
                                     String identifier, ResourceLocation id, double amount,
                                     AttributeModifier.Operation operation) {
    map.put(SlotAttribute.getOrCreate(identifier),
        new AttributeModifier(id, amount, operation));
  }

  public static void addSlotModifier(ItemStack stack, String identifier, ResourceLocation id,
                                     double amount, AttributeModifier.Operation operation,
                                     String slot) {
    addModifier(stack, SlotAttribute.getOrCreate(identifier), id, amount, operation, slot);
  }

  public static void addModifier(ItemStack stack, Holder<Attribute> attribute, ResourceLocation id,
                                 double amount, AttributeModifier.Operation operation,
                                 String slot) {
    ResourceLocation rl;

    if (attribute.value() instanceof SlotAttribute wrapper) {
      rl = ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, wrapper.getIdentifier());
    } else {
      rl = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());
    }

    CurioAttributeModifiers.Entry entry =
        new CurioAttributeModifiers.Entry(rl, new AttributeModifier(id, amount, operation), slot);
    CurioAttributeModifiers curioAttributeModifiers =
        stack.getOrDefault(CuriosRegistry.CURIO_ATTRIBUTE_MODIFIERS, CurioAttributeModifiers.EMPTY);
    List<CurioAttributeModifiers.Entry> list = new ArrayList<>(curioAttributeModifiers.modifiers());
    list.add(entry);
    stack.set(CuriosRegistry.CURIO_ATTRIBUTE_MODIFIERS,
        new CurioAttributeModifiers(list, curioAttributeModifiers.showInTooltip()));
  }

  public static void broadcastCurioBreakEvent(SlotContext slotContext) {
    LivingEntity livingEntity = slotContext.entity();

    if (livingEntity != null) {
      NetworkHandler.sendToTrackingAndSelf(livingEntity,
          new SPacketBreak(livingEntity.getId(), slotContext.identifier(), slotContext.index()));
    }
  }

  private static final Map<String, UUID> UUIDS = new HashMap<>();

  public static ResourceLocation getSlotId(SlotContext slotContext) {
    String key = slotContext.identifier() + slotContext.index();
    return ResourceLocation.fromNamespaceAndPath(CuriosConstants.MOD_ID, key);
  }


  private static final Map<ResourceLocation, Predicate<SlotResult>> SLOT_RESULT_PREDICATES =
      new HashMap<>();

  public static void registerCurioPredicate(ResourceLocation resourceLocation,
                                            Predicate<SlotResult> validator) {
    SLOT_RESULT_PREDICATES.putIfAbsent(resourceLocation, validator);
  }

  public static Optional<Predicate<SlotResult>> getCurioPredicate(
      ResourceLocation resourceLocation) {
    return Optional.ofNullable(SLOT_RESULT_PREDICATES.get(resourceLocation));
  }

  public static Map<ResourceLocation, Predicate<SlotResult>> getCurioPredicates() {
    return ImmutableMap.copyOf(SLOT_RESULT_PREDICATES);
  }

  public static boolean testCurioPredicates(Set<ResourceLocation> predicates,
                                            SlotResult slotResult) {

    for (ResourceLocation id : predicates) {

      if (CuriosApi.getCurioPredicate(id).map(
          slotResultPredicate -> slotResultPredicate.test(slotResult)).orElse(false)) {
        return true;
      }
    }
    return false;
  }

  static {
    // Our own namespace. Slot data written for the original Curios uses the legacy ids, which the
    // compatibility layer also accepts, so both spellings resolve to the same predicate.
    for (String namespace : new String[]{CuriosConstants.MOD_ID,
        CuriosConstants.LEGACY_NAMESPACE}) {
      registerCurioPredicate(ResourceLocation.fromNamespaceAndPath(namespace, "all"),
          (slotResult) -> true);
      registerCurioPredicate(ResourceLocation.fromNamespaceAndPath(namespace, "none"),
          (slotResult) -> false);
      registerCurioPredicate(ResourceLocation.fromNamespaceAndPath(namespace, "tag"),
          (slotResult) -> {
            String id = slotResult.slotContext().identifier();
            ItemStack stack = slotResult.stack();

            if (stack.is(CuriosTags.createItemTag(id)) || stack.is(CuriosTags.CURIO)) {
              return true;
            }
            // Items that a Forge mod assigned through the old "#curios:<slot>" tag.
            return ForgeCuriosCompat.isLoaded()
                && (stack.is(CuriosTags.legacyItemTag(id))
                || stack.is(CuriosTags.legacyItemTag("curio")));
          });
    }
  }
}
