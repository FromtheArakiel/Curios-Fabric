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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.type.capability.ICuriosItemHandler;

/**
 * Presents this mod's accessory data through Trinkets' interfaces.
 *
 * <p>Mods that were written for Trinkets keep asking {@code TrinketsApi} whether something is
 * equipped; the bridge answers those questions from our inventory. Per slot inventories are not
 * exposed, since Trinkets' {@code TrinketInventory} carries slot references that only Trinkets
 * itself can create.</p>
 */
public final class TrinketsApiBridge {

  private static final String API = "dev.emi.trinkets.api.";

  private TrinketsApiBridge() {
  }

  /** The Trinkets component of the entity, or {@code null} when the entity has no curios. */
  public static Object component(LivingEntity entity) {
    ICuriosItemHandler ours = CuriosApi.getCuriosInventory(entity).orElse(null);
    return ours == null ? null : component(ours);
  }

  private static Object component(ICuriosItemHandler ours) {
    return ForeignProxy.create(new String[]{API + "TrinketComponent"},
        (name, returnType, parameters, args) -> switch (name) {
          case "isEquipped" -> {
            if (args.length > 0 && args[0] instanceof Item item) {
              yield ours.isEquipped(item);
            }
            Predicate<ItemStack> filter =
                args.length > 0 && args[0] instanceof Predicate<?> predicate
                    ? castPredicate(predicate) : stack -> false;
            yield ours.isEquipped(filter);
          }
          // Both are mutable: Trinkets itself clears/mutates them (player connect, slot sync).
          case "getInventory" -> new java.util.HashMap<String, Object>();
          case "getEquipped" -> new java.util.ArrayList<>();
          case "forEach" -> null;
          case "update" -> null;
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  /** All equipped stacks, used when a mod only needs the stacks themselves. */
  public static List<ItemStack> equippedStacks(LivingEntity entity) {
    return CuriosApi.getCuriosInventory(entity)
        .map(handler -> handler.findCurios(stack -> true).stream()
            .map(result -> result.stack()).filter(stack -> !stack.isEmpty()).toList())
        .orElse(List.of());
  }

  /**
   * Builds a real Trinkets {@code SlotReference} for one of our slots.
   *
   * <p>Trinkets only creates these for its own inventory, but the record is
   * {@code (TrinketInventory, int)} and {@code TrinketInventory} has a plain three argument
   * constructor, so a faithful instance can be fabricated for the foreign code that expects one
   * (renderers and the {@code Trinket} hooks read the slot type, the index and the component).</p>
   *
   * @param curiosSlot The Curios slot identifier
   * @param index      The slot index
   * @param entity     The wearer
   * @return The slot reference, or {@code null} when it cannot be built
   */
  public static Object slotReference(String curiosSlot, int index, LivingEntity entity) {

    try {
      Class<?> slotTypeClass = dev.arakiel.curios.compat.CompatTargets.loadClass(API + "SlotType");
      Class<?> inventoryClass = dev.arakiel.curios.compat.CompatTargets
          .loadClass(API + "TrinketInventory");
      Class<?> referenceClass = dev.arakiel.curios.compat.CompatTargets
          .loadClass(API + "SlotReference");
      Class<?> componentClass = dev.arakiel.curios.compat.CompatTargets
          .loadClass(API + "TrinketComponent");
      Class<?> dropRuleClass = dev.arakiel.curios.compat.CompatTargets
          .loadClass(API + "TrinketEnums$DropRule");

      if (slotTypeClass == null || inventoryClass == null || referenceClass == null
          || componentClass == null || dropRuleClass == null) {
        return null;
      }
      String group = dev.arakiel.curios.compat.trinkets.TrinketSlots.toTrinkets(curiosSlot);
      String groupName = group == null ? curiosSlot : group;
      String slotName = groupName.contains("/")
          ? groupName.substring(groupName.indexOf('/') + 1) : groupName;
      Object dropRule = ForeignProxy.enumConstant(dropRuleClass, "DEFAULT",
          dropRuleClass.getEnumConstants()[0]);
      Object slotType = slotTypeClass
          .getConstructor(String.class, String.class, int.class, int.class, ResourceLocation.class,
              java.util.Set.class, java.util.Set.class, java.util.Set.class, dropRuleClass)
          .newInstance(groupName, slotName, 0, 1,
              ResourceLocation.fromNamespaceAndPath("curiosfabric", "slot/empty_curio_slot"),
              java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), dropRule);
      Object inventory = inventoryClass
          .getConstructor(slotTypeClass, componentClass, java.util.function.Consumer.class)
          .newInstance(slotType, component(entity),
              (java.util.function.Consumer<Object>) ignored -> {
              });
      return referenceClass.getConstructor(inventoryClass, int.class).newInstance(inventory, index);
    } catch (Throwable throwable) {
      CuriosConstants.LOG.debug("Failed to build a bridged Trinkets SlotReference", throwable);
      return null;
    }
  }

  /**
   * Invokes the renderer that Trinkets registered for this item, if it has one.
   *
   * <p>All arguments are passed as {@link Object} because this class is loaded on the dedicated
   * server as well, where client only rendering types do not exist. The order matches the Trinkets
   * contract for 1.21.1: stack, slot reference, entity model, matrices, vertex consumers, light,
   * entity, limb angle, limb distance, tick delta, animation progress, head yaw, head pitch.</p>
   *
   * @param args The render arguments
   * @return {@code true} when a Trinkets renderer was invoked
   */
  public static boolean renderForeign(Object[] args) {

    try {
      Class<?> registry = dev.arakiel.curios.compat.CompatTargets
          .loadClass(API + "client.TrinketRendererRegistry");

      if (registry == null || args.length < 3) {
        return false;
      }
      Object optional = registry.getMethod("getRenderer", Item.class)
          .invoke(null, ((ItemStack) args[0]).getItem());

      if (!(optional instanceof Optional<?> found) || found.isEmpty()) {
        return false;
      }
      Object renderer = found.get();

      for (Method method : renderer.getClass().getMethods()) {

        if (method.getName().equals("render") && method.getParameterCount() == args.length) {
          method.invoke(renderer, args);
          return true;
        }
      }
    } catch (Throwable throwable) {
      CuriosConstants.LOG.debug("Failed to invoke a bridged Trinkets renderer", throwable);
    }
    return false;
  }

  /** A Trinkets slot group is mapped onto a Curios slot identifier. */
  public static Optional<String> curiosSlotOf(String trinketsGroup) {
    return Optional.ofNullable(
        dev.arakiel.curios.compat.trinkets.TrinketSlots.toCurios(trinketsGroup));
  }

  @SuppressWarnings("unchecked")
  private static Predicate<ItemStack> castPredicate(Predicate<?> predicate) {
    return (Predicate<ItemStack>) predicate;
  }
}
