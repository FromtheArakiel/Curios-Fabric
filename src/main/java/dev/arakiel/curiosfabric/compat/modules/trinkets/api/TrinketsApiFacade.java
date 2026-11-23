package dev.arakiel.curiosfabric.compat.modules.trinkets.api;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;
import dev.arakiel.curiosfabric.compat.contribution.api.ForeignApiFacade;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeInvalidationReason;
import dev.arakiel.curiosfabric.compat.modules.trinkets.TrinketsCompat;
import dev.arakiel.curiosfabric.compat.modules.trinkets.asm.TrinketsAsm;
import dev.arakiel.curiosfabric.compat.modules.trinkets.runtime.TrinketsRuntimeIntegration;
import dev.arakiel.curiosfabric.compat.shared.reflection.ForeignProxy;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketComponent;

/**
 * Presents this mod's accessory data through Trinkets' interfaces.
 *
 * <p>Mods that were written for Trinkets keep asking {@code TrinketsApi} what is equipped, how many
 * of an item are worn and whether some stack is worn. Those questions are answered from our
 * inventory. The slot structure is exposed as a consistent synthetic view: Trinkets builds its
 * own inventory screen slots from {@code getGroups()} and {@code getInventory()} together, so both
 * maps are generated from the same Curios slot definitions. The actual screen remains disabled by
 * the bridge mixins.</p>
 */
public final class TrinketsApiFacade implements ForeignApiFacade {

  private static final String API = "dev.emi.trinkets.api.";

  public TrinketsApiFacade() {
  }

  @Override
  public void initialize(BridgeContext context) {
  }

  @Override
  public void invalidate(BridgeInvalidationReason reason) {
    TrinketsCompat.invalidate();
  }

  /** The Trinkets component of the entity, or {@code null} when the entity has no curios. */
  public static Object component(LivingEntity entity) {
    ICuriosItemHandler ours = CuriosApi.getCuriosInventory(entity).orElse(null);
    return ours == null ? emptyComponent(entity) : component(ours);
  }

  /**
   * Returns a consistent empty component during entity construction, before CuriosFabric has
   * attached its inventory component. Falling through to the upstream Trinkets component here
   * exposes Trinkets groups without matching inventory maps and crashes its PlayerScreenHandler.
   */
  private static Object emptyComponent(LivingEntity entity) {
    return TrinketsAsm.proxy(new String[]{API + "TrinketComponent"},
        (name, returnType, parameters, args) -> switch (name) {
          case "getEntity" -> entity;
          case "getGroups" -> Map.of();
          case "getInventory" -> Map.of();
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  private static Object component(ICuriosItemHandler ours) {
    LivingEntity entity = ours.getWearer();
    Object[] component = new Object[1];
    Object proxy = TrinketsAsm.proxy(new String[]{API + "TrinketComponent"},
        (name, returnType, parameters, args) -> switch (name) {
          case "getEntity" -> entity;
          case "getGroups" -> groupView(entity);
          case "getInventory" -> inventoryView(
              ours, (TrinketComponent) component[0], entity);
          case "isEquipped" -> isEquipped(ours, args);
          case "getEquipped", "getAllEquipped" -> equipped(ours, entity, filterOf(args));
          case "getModifiers" -> trinketModifiers(ours);
          case "addTemporaryModifiers" -> {
            ours.addTransientSlotModifiers(curiosModifiers(args[0]));
            yield null;
          }
          case "addPersistentModifiers" -> {
            ours.addPermanentSlotModifiers(curiosModifiers(args[0]));
            yield null;
          }
          case "removeModifiers" -> {
            ours.removeSlotModifiers(curiosModifiers(args[0]));
            yield null;
          }
          case "clearModifiers" -> {
            ours.clearSlotModifiers();
            yield null;
          }
          case "clearCachedModifiers" -> {
            ours.clearCachedSlotModifiers();
            yield null;
          }
          case "forEach" -> {
            forEach(ours, entity, args);
            yield null;
          }
          // Both are mutable: Trinkets itself clears/mutates them (player connect, slot sync).
          case "update" -> null;
          default -> ForeignProxy.defaultValue(returnType);
        });
    component[0] = proxy;
    return proxy;
  }

  private static Map<String, SlotGroup> groupView(LivingEntity entity) {
    Map<String, SlotGroup> result = new LinkedHashMap<>();
    entitySlots(entity).forEach((group, value) -> {
      if (value instanceof SlotGroup slotGroup) {
        result.put(group, slotGroup);
      }
    });
    return result;
  }

  private static Map<String, Map<String, dev.emi.trinkets.api.TrinketInventory>> inventoryView(
      ICuriosItemHandler ours, TrinketComponent component, LivingEntity entity) {
    Map<String, Map<String, dev.emi.trinkets.api.TrinketInventory>> result =
        new LinkedHashMap<>(TrinketsRuntimeIntegration.inventoryView(ours, component));
    // Trinkets assumes every group returned by getGroups() has a corresponding inventory entry.
    // Keep empty groups present so its PlayerScreenHandler never dereferences a missing map.
    groupView(entity).keySet().forEach(group ->
        result.computeIfAbsent(group, ignored -> new LinkedHashMap<>()));
    return result;
  }

  /** Builds the Trinkets slot-group view from Curios' entity slot registry. */
  public static Map<String, Object> entitySlots(LivingEntity entity) {
    return entity == null ? Map.of() : entitySlots(CuriosApi.getEntitySlots(entity));
  }

  /** Builds the Trinkets slot-group view for any entity used by Trinkets' public overload. */
  public static Map<String, Object> entitySlots(Entity entity) {
    boolean client = entity != null && entity.level() != null && entity.level().isClientSide();
    if (!TrinketsCompat.hasDiscoveredSlots(client)) {
      return Map.of();
    }
    if (entity instanceof LivingEntity livingEntity) {
      return entitySlots(livingEntity);
    }
    return entity == null || entity.level() == null
        ? Map.of()
        : entitySlots(entity.getType(), entity.level().isClientSide());
  }

  /** Builds the Trinkets slot-group view for an entity type. */
  public static Map<String, Object> entitySlots(EntityType<?> type, boolean isClient) {
    return type == null || !TrinketsCompat.hasDiscoveredSlots(isClient)
        ? Map.of()
        : entitySlots(CuriosApi.getEntitySlots(type, isClient));
  }

  private static Map<String, Object> entitySlots(Map<String, ISlotType> source) {
    Map<String, SlotGroup.Builder> builders = new LinkedHashMap<>();
    for (ISlotType slot : source.values()) {
      String path = TrinketsCompat.trinketsPathOf(slot.getIdentifier(), false);
      if (path == null) {
        path = TrinketsCompat.trinketsPathOf(slot.getIdentifier(), true);
      }
      if (path == null) {
        continue;
      }
      int separator = path.indexOf('/');
      if (separator < 1 || separator == path.length() - 1) {
        continue;
      }
      String group = path.substring(0, separator);
      String name = path.substring(separator + 1);
      SlotType trinketSlot = TrinketsCompat.apiSlotTypeForPath(path, false);
      if (trinketSlot == null) {
        trinketSlot = TrinketsCompat.apiSlotTypeForPath(path, true);
      }
      if (trinketSlot == null) {
        continue;
      }
      SlotGroup.Builder builder = builders.computeIfAbsent(
          group, ignored -> new SlotGroup.Builder(group, builders.size(), slot.getOrder().orElse(0)));
      builder.addSlot(name, trinketSlot);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    builders.forEach((group, builder) -> result.put(group, builder.build()));
    return result;
  }

  private static boolean isEquipped(ICuriosItemHandler ours, Object[] args) {

    if (args.length > 0 && args[0] instanceof Item item) {
      return ours.isEquipped(item);
    }
    return args.length > 0 && args[0] instanceof Predicate<?> predicate
        && ours.isEquipped(castPredicate(predicate));
  }

  private static Multimap<String, AttributeModifier> trinketModifiers(
      ICuriosItemHandler ours) {
    Multimap<String, AttributeModifier> result = LinkedHashMultimap.create();
    boolean client = ours.getWearer() != null && ours.getWearer().level() != null
        && ours.getWearer().level().isClientSide();
    ours.getModifiers().forEach((curiosSlot, modifiers) -> {
      String trinketsPath = TrinketsCompat.trinketsPathOf(curiosSlot, client);
      result.put(trinketsPath == null ? curiosSlot : trinketsPath, modifiers);
    });
    return result;
  }

  private static Multimap<String, AttributeModifier> curiosModifiers(Object raw) {
    Multimap<String, AttributeModifier> result = LinkedHashMultimap.create();
    if (!(raw instanceof Multimap<?, ?> modifiers)) {
      return result;
    }
    for (Map.Entry<?, ?> entry : modifiers.entries()) {
      if (!(entry.getKey() instanceof String path)
          || !(entry.getValue() instanceof AttributeModifier modifier)) {
        continue;
      }
      String curiosSlot = TrinketsCompat.curiosSlotOf(path, false);
      if (curiosSlot == null) {
        curiosSlot = TrinketsCompat.curiosSlotOf(path, true);
      }
      if (curiosSlot == null && CuriosApi.getSlot(path, false).isPresent()) {
        curiosSlot = path;
      }
      result.put(curiosSlot == null ? path : curiosSlot, modifier);
    }
    return result;
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
    return TrinketsRuntimeIntegration.slotReference(curiosSlot, index, entity, stack);
  }

  /** A complete Trinkets {@code group/slot} path is mapped onto a Curios identifier. */
  public static Optional<String> curiosSlotOf(String trinketsPath) {
    String server = TrinketsCompat.curiosSlotOf(trinketsPath, false);
    return Optional.ofNullable(server != null
        ? server : TrinketsCompat.curiosSlotOf(trinketsPath, true));
  }

  @SuppressWarnings("unchecked")
  private static Predicate<ItemStack> castPredicate(Predicate<?> predicate) {
    return (Predicate<ItemStack>) predicate;
  }
}
