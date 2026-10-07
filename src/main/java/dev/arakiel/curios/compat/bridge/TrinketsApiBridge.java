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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curios.api.type.inventory.IDynamicStackHandler;
import dev.arakiel.curios.compat.trinkets.TrinketSlots;

/**
 * Presents this mod's accessory data through Trinkets' interfaces.
 *
 * <p>Mods that were written for Trinkets keep asking {@code TrinketsApi} what is equipped, how many
 * of an item are worn and whether some stack is worn. Those questions are answered from our
 * inventory. The slot structure itself is reported as empty: Trinkets builds its own inventory
 * screen slots from {@code getGroups()} and {@code getInventory()} together, and reporting only one
 * of them as empty makes Trinkets throw while a player is being placed into the world, because it
 * looks each inventory group name up in the group map and dereferences the missing result. Both
 * maps are therefore deliberately empty and have to stay consistent with each other.</p>
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
    LivingEntity entity = ours.getWearer();
    return ForeignProxy.create(new String[]{API + "TrinketComponent"},
        (name, returnType, parameters, args) -> switch (name) {
          case "getEntity" -> entity;
          case "isEquipped" -> isEquipped(ours, args);
          case "getEquipped", "getAllEquipped" -> equipped(ours, entity, filterOf(args));
          // Deliberately empty and deliberately consistent with the empty getGroups() above.
          // Trinkets' PlayerScreenHandlerMixin walks this map and looks every group name up in the
          // group map; a non-empty inventory paired with an empty group map is a null dereference
          // on the player join path, which aborts the join entirely.
          case "getInventory" -> new LinkedHashMap<String, Map<String, Object>>();
          case "forEach" -> {
            forEach(ours, entity, args);
            yield null;
          }
          // Both are mutable: Trinkets itself clears/mutates them (player connect, slot sync).
          case "update" -> null;
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  private static boolean isEquipped(ICuriosItemHandler ours, Object[] args) {

    if (args.length > 0 && args[0] instanceof Item item) {
      return ours.isEquipped(item);
    }
    return args.length > 0 && args[0] instanceof Predicate<?> predicate
        && ours.isEquipped(castPredicate(predicate));
  }

  private static Predicate<ItemStack> filterOf(Object[] args) {

    if (args.length == 0) {
      return stack -> true;
    }

    if (args[0] instanceof Item item) {
      return stack -> stack.getItem() == item;
    }
    return args[0] instanceof Predicate<?> predicate ? castPredicate(predicate) : stack -> true;
  }

  /**
   * The equipped curios as Trinkets {@code (SlotReference, ItemStack)} tuples.
   *
   * <p>Mods such as Potion Ring count how many of a ring are worn through this list, so it has to
   * reflect the real stacks instead of being an empty placeholder.</p>
   */
  private static List<Object> equipped(ICuriosItemHandler ours, LivingEntity entity,
                                       Predicate<ItemStack> filter) {
    List<Object> result = new ArrayList<>();

    if (entity == null) {
      return result;
    }
    ours.getCurios().forEach((id, handler) -> {
      IDynamicStackHandler stacks = handler.getStacks();

      for (int i = 0; i < stacks.getSlots(); i++) {
        ItemStack stack = stacks.getStackInSlot(i);

        if (!stack.isEmpty() && filter.test(stack)) {
          result.add(new Tuple<>(slotReference(id, i, entity, stack), stack));
        }
      }
    });
    return result;
  }

  private static void forEach(ICuriosItemHandler ours, LivingEntity entity, Object[] args) {

    if (entity == null || args.length == 0 || !(args[0] instanceof java.util.function.BiConsumer<?, ?> consumer)) {
      return;
    }

    @SuppressWarnings("unchecked")
    java.util.function.BiConsumer<Object, Object> forwarder =
        (java.util.function.BiConsumer<Object, Object>) consumer;

    for (Object entry : equipped(ours, entity, stack -> true)) {

      if (entry instanceof Tuple<?, ?> tuple) {
        forwarder.accept(tuple.getA(), tuple.getB());
      }
    }
  }

  /** All equipped stacks, used when a mod only needs the stacks themselves. */
  public static List<ItemStack> equippedStacks(LivingEntity entity) {
    return CuriosApi.getCuriosInventory(entity)
        .map(handler -> handler.findCurios(stack -> true).stream()
            .map(result -> result.stack()).filter(stack -> !stack.isEmpty()).toList())
        .orElse(List.of());
  }

  /** The Trinkets group name of a {@code group/slot} path. */
  private static String groupOf(String path) {
    int separator = path.indexOf('/');
    return separator < 0 ? path : path.substring(0, separator);
  }

  /** The Trinkets slot name of a {@code group/slot} path. */
  private static String slotOf(String path) {
    int separator = path.indexOf('/');
    return separator < 0 ? path : path.substring(separator + 1);
  }

  /**
   * Builds a real Trinkets {@code SlotReference} for one of our slots.
   *
   * <p>Trinkets only creates these for its own inventory, but the record is
   * {@code (TrinketInventory, int)} and {@code TrinketInventory} has a plain three argument
   * constructor, so a faithful instance can be fabricated for the foreign code that expects one
   * (renderers and the {@code Trinket} hooks read the slot type, the index, the component and the
   * stack sitting in the slot).</p>
   *
   * @param curiosSlot The Curios slot identifier
   * @param index      The slot index
   * @param entity     The wearer
   * @return The slot reference, or {@code null} when it cannot be built
   */
  public static Object slotReference(String curiosSlot, int index, LivingEntity entity) {
    return slotReference(curiosSlot, index, entity, ItemStack.EMPTY);
  }

  /** {@link #slotReference(String, int, LivingEntity)} with the stack that currently sits there. */
  public static Object slotReference(String curiosSlot, int index, LivingEntity entity,
                                     ItemStack stack) {

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
      String path = TrinketSlots.toTrinkets(curiosSlot);
      String group = path == null ? curiosSlot : groupOf(path);
      String slot = path == null ? curiosSlot : slotOf(path);
      int size = Math.max(1, index + 1);
      Object slotType = slotType(slotTypeClass, dropRuleClass, group, slot, size);
      Object inventory = inventoryClass
          .getConstructor(slotTypeClass, componentClass, Consumer.class)
          .newInstance(slotType, component(entity), (Consumer<Object>) ignored -> {
          });
      seedStacks(inventoryClass, inventory, index, stack);
      return referenceClass.getConstructor(inventoryClass, int.class).newInstance(inventory, index);
    } catch (Throwable throwable) {
      CuriosConstants.LOG.debug("Failed to build a bridged Trinkets SlotReference", throwable);
      return null;
    }
  }

  private static Object slotType(Class<?> slotTypeClass, Class<?> dropRuleClass, String group,
                                 String slot, int size) throws Exception {
    Object dropRule = ForeignProxy.enumConstant(dropRuleClass, "DEFAULT",
        dropRuleClass.getEnumConstants()[0]);
    return slotTypeClass
        .getConstructor(String.class, String.class, int.class, int.class, ResourceLocation.class,
            java.util.Set.class, java.util.Set.class, java.util.Set.class, dropRuleClass)
        .newInstance(group, slot, 0, size,
            ResourceLocation.fromNamespaceAndPath(CuriosConstants.MOD_ID, "slot/empty_curio_slot"),
            java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), dropRule);
  }

  private static void seedStacks(Class<?> inventoryClass, Object inventory, int index,
                                 ItemStack stack) throws Exception {

    if (stack == null || stack.isEmpty()) {
      return;
    }
    Field field = stacksField(inventoryClass);

    if (field == null) {
      return;
    }
    @SuppressWarnings("unchecked")
    NonNullList<ItemStack> list = (NonNullList<ItemStack>) field.get(inventory);

    if (list != null && index >= 0 && index < list.size()) {
      list.set(index, stack);
    }
  }

  private static Field stacksField(Class<?> inventoryClass) {

    try {
      Field field = inventoryClass.getDeclaredField("stacks");
      field.setAccessible(true);
      return field;
    } catch (Throwable throwable) {
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
    return Optional.ofNullable(TrinketSlots.toCurios(trinketsGroup));
  }

  @SuppressWarnings("unchecked")
  private static Predicate<ItemStack> castPredicate(Predicate<?> predicate) {
    return (Predicate<ItemStack>) predicate;
  }
}
