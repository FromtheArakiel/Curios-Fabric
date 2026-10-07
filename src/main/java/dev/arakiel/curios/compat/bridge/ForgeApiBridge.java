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
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.SlotContext;
import dev.arakiel.curios.api.SlotResult;
import dev.arakiel.curios.api.type.ISlotType;
import dev.arakiel.curios.api.type.capability.ICurio;
import dev.arakiel.curios.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curios.api.type.inventory.ICurioStacksHandler;
import dev.arakiel.curios.api.type.inventory.IDynamicStackHandler;

/**
 * Presents this mod's accessory data through the Forge/NeoForge edition's interfaces.
 *
 * <p>Forge mods that were written against Curios keep calling {@code CuriosApi} and the capability
 * interfaces of the Forge edition. Those interfaces still exist (the Forge mod is present), so the
 * bridge hands them {@link java.lang.reflect.Proxy proxies} that answer with <em>our</em> inventory.
 * That is what makes "all accessories live in our Curios" work for the other side's code too.</p>
 */
public final class ForgeApiBridge {

  private static final String API = "top.theillusivec4.curios.api.";
  private static final String ITEM_HANDLER = "net.neoforged.neoforge.items.IItemHandlerModifiable";

  /**
   * Renderers taken out of the Forge edition's supplier map, keyed by item.
   *
   * <p>The Forge edition resolves its registered suppliers into a second map that is only filled by
   * its own client setup - which this mod intentionally cancels - so its {@code getRenderer} always
   * answers empty here. The suppliers themselves are read and instantiated instead.</p>
   */
  private static final Map<Item, Object> FOREIGN_RENDERERS = new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.Set<Item> FOREIGN_RENDERERS_MISSING =
      java.util.concurrent.ConcurrentHashMap.newKeySet();

  private ForgeApiBridge() {
  }

  /** The inventory of the entity as a Forge {@code ICuriosItemHandler}, or {@code null}. */
  public static Object inventory(LivingEntity entity) {
    ICuriosItemHandler ours = CuriosApi.getCuriosInventory(entity).orElse(null);
    return ours == null ? null : inventory(ours);
  }

  private static Object inventory(ICuriosItemHandler ours) {
    return ForeignProxy.create(
        new String[]{API + "type.capability.ICuriosItemHandler", ITEM_HANDLER},
        (name, returnType, parameters, args) -> switch (name) {
          case "getCurios" -> {
            Map<String, Object> map = new LinkedHashMap<>();
            ours.getCurios().forEach((id, handler) -> map.put(id, stacksHandler(handler)));
            yield map;
          }
          case "getStacksHandler" -> Optional.ofNullable(
              ours.getStacksHandler((String) args[0]).map(ForgeApiBridge::stacksHandler)
                  .orElse(null));
          case "getSlots" -> ours.getSlots();
          case "getVisibleSlots" -> ours.getVisibleSlots();
          case "getWearer" -> ours.getWearer();
          case "getEquippedCurios" -> itemHandler(ours.getEquippedCurios());
          case "setEquippedCurio" -> {
            ours.setEquippedCurio((String) args[0], (Integer) args[1], (ItemStack) args[2]);
            yield null;
          }
          case "isEquipped" -> args[0] instanceof Item item ? ours.isEquipped(item)
              : ours.isEquipped(predicate(args[0]));
          case "isSlotActive" -> ours.isSlotActive((String) args[0], (Integer) args[1]);
          case "setSlotActive" -> {
            ours.setSlotActive((String) args[0], (Integer) args[1], (Boolean) args[2]);
            yield null;
          }
          case "setSlotsActive" -> {
            ours.setSlotsActive((String) args[0], (Boolean) args[1]);
            yield null;
          }
          case "findFirstCurio" -> slotResult(ours.findFirstCurio(argumentFilter(args)));
          case "findCurios" -> {
            if (args.length == 1 && args[0] instanceof Predicate) {
              yield ours.findCurios(predicate(args[0])).stream().map(ForgeApiBridge::slotResult)
                  .toList();
            }

            if (args.length >= 1 && args[0] instanceof String[] ids) {
              yield ours.findCurios(ids).stream().map(ForgeApiBridge::slotResult).toList();
            }
            yield new java.util.ArrayList<>();
          }
          case "findCurio" -> {
            int index = (Integer) args[1];
            yield args.length > 2 && args[2] instanceof Boolean includeInactive
                ? slotResult(ours.findCurio((String) args[0], index, includeInactive))
                : slotResult(ours.findCurio((String) args[0], index));
          }
          // The foreign edition's own loot mixins are still applied (its mixin config does not know
          // that we took over), and they add this value on top of ours. They must therefore see
          // nothing, otherwise every curio fortune/looting bonus would be counted twice.
          case "getFortuneLevel" -> 0;
          case "getLootingLevel" -> 0;
          case "growSlotType" -> {
            ours.growSlotType((String) args[0], (Integer) args[1]);
            yield null;
          }
          case "shrinkSlotType" -> {
            ours.shrinkSlotType((String) args[0], (Integer) args[1]);
            yield null;
          }
          case "reset" -> {
            ours.reset();
            yield null;
          }
          case "clearCachedSlotModifiers" -> {
            ours.clearCachedSlotModifiers();
            yield null;
          }
          case "handleInvalidStacks" -> {
            ours.handleInvalidStacks();
            yield null;
          }
          case "loseInvalidStack" -> {
            ours.loseInvalidStack((ItemStack) args[0]);
            yield null;
          }
          case "getModifiers" -> ours.getModifiers();
          case "addTransientSlotModifiers" -> {
            ours.addTransientSlotModifiers(modifiers(args[0]));
            yield null;
          }
          case "addPermanentSlotModifiers" -> {
            ours.addPermanentSlotModifiers(modifiers(args[0]));
            yield null;
          }
          case "removeSlotModifiers" -> {
            ours.removeSlotModifiers(modifiers(args[0]));
            yield null;
          }
          case "clearSlotModifiers" -> {
            ours.clearSlotModifiers();
            yield null;
          }
          case "writeTag" -> ours.writeTag();
          case "readTag" -> {
            ours.readTag((Tag) args[0]);
            yield null;
          }
          case "saveInventory" -> ours.saveInventory((Boolean) args[0]);
          case "loadInventory" -> {
            ours.loadInventory((net.minecraft.nbt.ListTag) args[0]);
            yield null;
          }
          case "getLockedSlots" -> new java.util.HashSet<String>();
          case "getUpdatingInventories" -> new java.util.HashSet<Object>();
          case "getFortuneBonus" -> 0;
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  private static Object stacksHandler(ICurioStacksHandler ours) {
    return ForeignProxy.create(new String[]{API + "type.inventory.ICurioStacksHandler"},
        (name, returnType, parameters, args) -> switch (name) {
          case "getStacks" -> dynamicStackHandler(ours.getStacks());
          case "getCosmeticStacks" -> dynamicStackHandler(ours.getCosmeticStacks());
          case "getRenders" -> ours.getRenders();
          case "getActiveStates" -> ours.getActiveStates();
          case "updateActiveState" -> {
            ours.updateActiveState((Integer) args[0]);
            yield null;
          }
          case "canToggleRendering" -> ours.canToggleRendering();
          case "getSlots" -> ours.getSlots();
          case "isVisible" -> ours.isVisible();
          case "hasCosmetic" -> ours.hasCosmetic();
          case "getIdentifier" -> ours.getIdentifier();
          case "serializeNBT" -> ours.serializeNBT();
          case "deserializeNBT" -> {
            ours.deserializeNBT((net.minecraft.nbt.CompoundTag) args[0]);
            yield null;
          }
          case "getDropRule" -> dropRule(ours.getDropRule());
          case "getModifiers" -> ours.getModifiers();
          case "getPermanentModifiers" -> ours.getPermanentModifiers();
          case "getCachedModifiers" -> ours.getCachedModifiers();
          case "getModifiersByOperation" -> ours.getModifiersByOperation(operation(args[0]));
          case "addTransientModifier" -> {
            ours.addTransientModifier((AttributeModifier) args[0]);
            yield null;
          }
          case "addPermanentModifier" -> {
            ours.addPermanentModifier((AttributeModifier) args[0]);
            yield null;
          }
          case "removeModifier" -> {
            ours.removeModifier((ResourceLocation) args[0]);
            yield null;
          }
          case "clearModifiers" -> {
            ours.clearModifiers();
            yield null;
          }
          case "clearCachedModifiers" -> {
            ours.clearCachedModifiers();
            yield null;
          }
          case "update" -> {
            ours.update();
            yield null;
          }
          case "getSyncTag" -> ours.getSyncTag();
          case "applySyncTag" -> {
            ours.applySyncTag((net.minecraft.nbt.CompoundTag) args[0]);
            yield null;
          }
          case "getSizeShift" -> 0;
          case "grow" -> {
            ours.grow((Integer) args[0]);
            yield null;
          }
          case "shrink" -> {
            ours.shrink((Integer) args[0]);
            yield null;
          }
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  private static Object dynamicStackHandler(IDynamicStackHandler ours) {
    return ForeignProxy.create(
        new String[]{API + "type.inventory.IDynamicStackHandler", ITEM_HANDLER},
        (name, returnType, parameters, args) -> switch (name) {
          case "getSlots" -> ours.getSlots();
          case "getStackInSlot" -> ours.getStackInSlot((Integer) args[0]);
          case "setStackInSlot" -> {
            ours.setStackInSlot((Integer) args[0], (ItemStack) args[1]);
            yield null;
          }
          case "getPreviousStackInSlot" -> ours.getPreviousStackInSlot((Integer) args[0]);
          case "setPreviousStackInSlot" -> {
            ours.setPreviousStackInSlot((Integer) args[0], (ItemStack) args[1]);
            yield null;
          }
          case "grow" -> {
            ours.grow((Integer) args[0]);
            yield null;
          }
          case "shrink" -> {
            ours.shrink((Integer) args[0]);
            yield null;
          }
          case "serializeNBT" -> ours.serializeNBT(
              (net.minecraft.core.HolderLookup.Provider) args[0]);
          case "deserializeNBT" -> {
            ours.deserializeNBT((net.minecraft.core.HolderLookup.Provider) args[0],
                (net.minecraft.nbt.CompoundTag) args[1]);
            yield null;
          }
          case "getSlotLimit" -> ours.getSlotLimit((Integer) args[0]);
          case "isItemValid" -> ours.isItemValid((Integer) args[0], (ItemStack) args[1]);
          case "insertItem" -> ours.insertItem((Integer) args[0], (ItemStack) args[1],
              (Boolean) args[2]);
          case "extractItem" -> ours.extractItem((Integer) args[0], (Integer) args[1],
              (Boolean) args[2]);
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  private static Object itemHandler(dev.arakiel.curios.api.type.inventory.IItemHandlerModifiable ours) {
    return ForeignProxy.create(new String[]{ITEM_HANDLER},
        (name, returnType, parameters, args) -> switch (name) {
          case "getSlots" -> ours.getSlots();
          case "getStackInSlot" -> ours.getStackInSlot((Integer) args[0]);
          case "setStackInSlot" -> {
            ours.setStackInSlot((Integer) args[0], (ItemStack) args[1]);
            yield null;
          }
          case "getSlotLimit" -> ours.getSlotLimit((Integer) args[0]);
          case "isItemValid" -> ours.isItemValid((Integer) args[0], (ItemStack) args[1]);
          case "insertItem" -> ours.insertItem((Integer) args[0], (ItemStack) args[1],
              (Boolean) args[2]);
          case "extractItem" -> ours.extractItem((Integer) args[0], (Integer) args[1],
              (Boolean) args[2]);
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  /** The curio behavior of the stack as a Forge {@code ICurio}, or {@code null}. */
  public static Object curio(ItemStack stack) {
    ICurio ours = CuriosApi.getCurio(stack).orElse(null);

    if (ours == null) {
      return null;
    }
    return ForeignProxy.create(new String[]{API + "type.capability.ICurio"},
        (name, returnType, parameters, args) -> switch (name) {
          case "getStack" -> ours.getStack();
          case "curioTick" -> {
            ours.curioTick(context(args[0]));
            yield null;
          }
          case "canEquip" -> ours.canEquip(context(args[0]));
          case "canUnequip" -> ours.canUnequip(context(args[0]));
          case "curioBreak" -> {
            ours.curioBreak(context(args[0]));
            yield null;
          }
          case "canSync" -> ours.canSync(context(args[0]));
          case "writeSyncData" -> ours.writeSyncData(context(args[0]));
          case "readSyncData" -> {
            ours.readSyncData(context(args[0]), (net.minecraft.nbt.CompoundTag) args[1]);
            yield null;
          }
          case "getDropRule" -> dropRule(ours.getDropRule(context(args[0]),
              (net.minecraft.world.damagesource.DamageSource) args[1], (Integer) args[2],
              (Boolean) args[3]));
          case "getFortuneLevel" -> ours.getFortuneLevel(context(args[0]),
              args.length > 1 ? (LootContext) args[1] : null);
          case "getLootingLevel" -> ours.getLootingLevel(context(args[0]),
              args.length > 1 ? (LootContext) args[1] : null);
          case "onEquipFromUse" -> {
            ours.onEquipFromUse(context(args[0]));
            yield null;
          }
          case "canEquipFromUse" -> ours.canEquipFromUse(context(args[0]));
          case "onEquip" -> {
            ours.onEquip(context(args[0]), (ItemStack) args[1]);
            yield null;
          }
          case "onUnequip" -> {
            ours.onUnequip(context(args[0]), (ItemStack) args[1]);
            yield null;
          }
          case "makesPiglinsNeutral" -> ours.makesPiglinsNeutral(context(args[0]));
          case "canWalkOnPowderedSnow" -> ours.canWalkOnPowderedSnow(context(args[0]));
          case "isEnderMask" -> ours.isEnderMask(context(args[0]), (EnderMan) args[1]);
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  /** A Forge {@code ISlotType} view of one of our slot types. */
  public static Object slotType(ISlotType ours) {
    return ForeignProxy.create(new String[]{API + "type.ISlotType"},
        (name, returnType, parameters, args) -> switch (name) {
          case "getIdentifier" -> ours.getIdentifier();
          case "getIcon" -> ours.getIcon();
          case "getOrder" -> ours.getOrder();
          case "getSize" -> ours.getSize();
          case "useNativeGui" -> ours.useNativeGui();
          case "hasCosmetic" -> ours.hasCosmetic();
          case "canToggleRendering" -> ours.canToggleRendering();
          case "getValidators" -> ours.getValidators();
          case "writeNbt" -> ours.writeNbt();
          case "getDropRule" -> dropRule(ours.getDropRule());
          case "compareTo" -> ours.getIdentifier().compareTo(
              String.valueOf(ForeignProxy.read(args[0], "getIdentifier", "")));
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  private static Object slotResult(Optional<SlotResult> result) {
    // Must always be an Optional: the foreign API returns Optional<SlotResult> and callers such as
    // Shulker Box Slot cast the result straight to Optional, so returning the bare SlotResult
    // crashed the client with a ClassCastException.
    return result.flatMap(ourResult -> Optional.ofNullable(slotResult(ourResult)));
  }

  /**
   * Invokes the renderer that the Forge edition registered for this item, if it has one.
   *
   * <p>Everything is passed as {@link Object} on purpose: this class is loaded on the dedicated
   * server too, where client-only rendering types do not exist. The argument order matches the
   * Forge renderer contract: stack, slot context, pose stack, render layer parent, buffer source,
   * light, limb swing, limb swing amount, partial ticks, age in ticks, net head yaw, head pitch.</p>
   *
   * @param args The render arguments
   * @return {@code true} when a foreign renderer was invoked
   */
  public static boolean renderForeign(Object[] args) {

    try {
      Class<?> registry = dev.arakiel.curios.compat.CompatTargets
          .loadClass(API + "client.CuriosRendererRegistry");

      if (registry == null || args.length == 0) {
        return false;
      }
      Item item = ((ItemStack) args[0]).getItem();
      Object renderer = foreignRenderer(registry, item);

      if (renderer == null) {
        return false;
      }
      Object[] call = args.clone();
      call[1] = foreignContext((SlotContext) args[1]);

      for (java.lang.reflect.Method method : renderer.getClass().getMethods()) {

        if (method.getName().equals("render") && method.getParameterCount() == args.length) {
          method.invoke(renderer, call);
          return true;
        }
      }
    } catch (Throwable throwable) {
      CuriosConstants.LOG.debug("Failed to invoke a bridged Forge curio renderer", throwable);
    }
    return false;
  }

  /**
   * Resolves a renderer that a Forge mod registered with the Forge edition's registry.
   *
   * <p>{@code CuriosRendererRegistry.getRenderer} only reads the cache that its own client setup is
   * supposed to fill. Since that setup is cancelled while this mod owns the accessory system, the
   * suppliers are read straight from the registry map and resolved here instead.</p>
   */
  private static Object foreignRenderer(Class<?> registry, Item item) {
    Object cached = FOREIGN_RENDERERS.get(item);

    if (cached != null) {
      return cached;
    }

    if (FOREIGN_RENDERERS_MISSING.contains(item)) {
      return null;
    }

    try {
      // Prefer the foreign registry's own cache so a renderer that the Forge edition did resolve is
      // reused instead of being built a second time.
      Object optional = registry.getMethod("getRenderer", Item.class).invoke(null, item);

      if (optional instanceof Optional<?> found && found.isPresent()) {
        Object renderer = found.get();
        FOREIGN_RENDERERS.put(item, renderer);
        return renderer;
      }

      java.lang.reflect.Field suppliersField = registry.getDeclaredField("RENDERER_REGISTRY");
      suppliersField.setAccessible(true);

      if (!(suppliersField.get(null) instanceof Map<?, ?> suppliers)) {
        FOREIGN_RENDERERS_MISSING.add(item);
        return null;
      }
      Object supplier = suppliers.get(item);

      if (!(supplier instanceof java.util.function.Supplier<?> rendererSupplier)) {
        FOREIGN_RENDERERS_MISSING.add(item);
        return null;
      }
      Object renderer = rendererSupplier.get();

      if (renderer != null) {
        FOREIGN_RENDERERS.put(item, renderer);
      }
      return renderer;
    } catch (Throwable throwable) {
      FOREIGN_RENDERERS_MISSING.add(item);
      return null;
    }
  }

  private static Object slotResult(SlotResult ours) {

    try {
      Class<?> contextClass = dev.arakiel.curios.compat.CompatTargets
          .loadClass(API + "SlotContext");
      Class<?> resultClass = dev.arakiel.curios.compat.CompatTargets.loadClass(API + "SlotResult");

      if (contextClass == null || resultClass == null) {
        return null;
      }
      return resultClass.getConstructor(contextClass, ItemStack.class)
          .newInstance(foreignContext(ours.slotContext()), ours.stack());
    } catch (Throwable throwable) {
      CuriosConstants.LOG.debug("Failed to build a bridged SlotResult", throwable);
      return null;
    }
  }

  /** Builds a Forge {@code SlotContext} from ours, or {@code null} when it is unavailable. */
  public static Object foreignContext(SlotContext ours) {

    try {
      Class<?> contextClass = dev.arakiel.curios.compat.CompatTargets
          .loadClass(API + "SlotContext");

      if (contextClass == null || ours == null) {
        return null;
      }
      Constructor<?> constructor = contextClass.getConstructor(String.class, LivingEntity.class,
          int.class, boolean.class, boolean.class);
      return constructor.newInstance(ours.identifier(), ours.entity(), ours.index(),
          ours.cosmetic(), ours.visible());
    } catch (Throwable throwable) {
      return null;
    }
  }

  private static Object slotResult(Object foreign) {
    return foreign == null ? Optional.empty() : Optional.of(foreign);
  }

  private static Object dropRule(ICurio.DropRule ours) {
    Class<?> type = dev.arakiel.curios.compat.CompatTargets
        .loadClass(API + "type.capability.ICurio$DropRule");
    return ForeignProxy.enumConstant(type, ours.name(), null);
  }

  private static AttributeModifier.Operation operation(Object foreign) {
    return foreign instanceof AttributeModifier.Operation operation ? operation
        : AttributeModifier.Operation.ADD_VALUE;
  }

  @SuppressWarnings("unchecked")
  private static Multimap<String, AttributeModifier> modifiers(Object foreign) {
    return foreign instanceof Multimap<?, ?> multimap
        ? (Multimap<String, AttributeModifier>) multimap : com.google.common.collect.HashMultimap.create();
  }

  /** Converts a Forge {@code SlotContext} into ours. */
  private static SlotContext context(Object foreign) {

    if (foreign instanceof SlotContext ours) {
      return ours;
    }
    return new SlotContext(
        String.valueOf(ForeignProxy.read(foreign, "identifier", "")),
        (LivingEntity) ForeignProxy.read(foreign, "entity", null),
        (Integer) ForeignProxy.read(foreign, "index", 0),
        (Boolean) ForeignProxy.read(foreign, "cosmetic", false),
        (Boolean) ForeignProxy.read(foreign, "visible", true));
  }

  @SuppressWarnings("unchecked")
  private static Predicate<ItemStack> predicate(Object foreign) {
    return foreign instanceof Predicate<?> predicate ? (Predicate<ItemStack>) predicate
        : stack -> false;
  }

  private static Predicate<ItemStack> argumentFilter(Object[] args) {

    if (args.length > 0 && args[0] instanceof Item item) {
      return stack -> stack.getItem() == item;
    }
    return args.length > 0 ? predicate(args[0]) : stack -> false;
  }

}
