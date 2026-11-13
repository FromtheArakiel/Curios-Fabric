package dev.arakiel.curiosfabric.compat.trinkets;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.common.data.CuriosEntityManager;
import dev.arakiel.curiosfabric.common.data.CuriosSlotManager;
import dev.arakiel.curiosfabric.common.slottype.SlotType;
import dev.arakiel.curiosfabric.compat.CompatTargets;
import dev.arakiel.curiosfabric.compat.NativeSlotMappings;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Dynamic Trinkets slot bridge.
 *
 * <p>Trinkets exposes its loaded slot groups through {@code TrinketsApi.getEntitySlots}; this bridge
 * reflects that API so third-party data packs and mods that add new groups or slots are discovered
 * without a hard-coded group list.</p>
 */
public final class TrinketsCompat {

  public static final String MOD_ID = "trinkets";
  private static final String API = "dev.emi.trinkets.api.";
  private static final ResourceLocation FALLBACK_ICON =
      ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID,
          "slot/empty_curio_slot");
  private static final Map<String, TagKey<Item>> TAG_CACHE = new HashMap<>();

  private static volatile Map<String, TrinketSlot> slotsByPath = Map.of();
  private static volatile Map<String, String> pathByCuriosSlot = Map.of();

  private TrinketsCompat() {
  }

  public static boolean isLoaded() {
    return CompatTargets.isTrinketsLoaded();
  }

  /**
   * Re-discovers all slot groups and mirrors their slot types, entity assignments and item tags.
   */
  public static void register() {
    if (!isLoaded()) {
      invalidate();
      return;
    }

    Map<EntityType<?>, Map<String, TrinketSlot>> byEntity = discoverEntitySlots();
    Map<String, TrinketSlot> discovered = flatten(byEntity);
    if (!discovered.isEmpty()) {
      CuriosFabricConstants.LOG.info("Discovered {} Trinkets slot definitions",
          discovered.size());
    }
    Map<String, ISlotType> generatedSlots = new HashMap<>();

    for (TrinketSlot slot : discovered.values()) {
      if (CuriosApi.getSlot(slot.curiosId, false).isEmpty()) {
        generatedSlots.computeIfAbsent(slot.curiosId, ignored -> createSlotType(slot));
      }
    }
    CuriosSlotManager.SERVER.mergeSlots(generatedSlots, MOD_ID);
    if (!generatedSlots.isEmpty()) {
      CuriosFabricConstants.LOG.info("Added {} compatibility slots from Trinkets",
          generatedSlots.size());
      for (TrinketSlot slot : discovered.values()) {
        if (generatedSlots.containsKey(slot.curiosId)) {
          CuriosFabricConstants.LOG.info("Mirrored Trinkets slot {} as {} (source {})",
              slot.path, slot.curiosId, generatedSlots.get(slot.curiosId).getSource().id());
        }
      }
    }

    Map<EntityType<?>, Map<String, ISlotType>> entitySlots = new HashMap<>();
    byEntity.forEach((entityType, slots) -> {
      Map<String, ISlotType> curiosSlots = new LinkedHashMap<>();
      slots.forEach((path, slot) -> CuriosApi.getSlot(slot.curiosId, false)
          .ifPresent(curiosSlot -> curiosSlots.put(slot.curiosId, curiosSlot)));
      if (!curiosSlots.isEmpty()) {
        entitySlots.put(entityType, curiosSlots);
      }
    });
    CuriosEntityManager.SERVER.mergeEntitySlots(entitySlots);

    slotsByPath = Map.copyOf(discovered);
    Map<String, String> reverse = new HashMap<>();
    for (TrinketSlot slot : discovered.values()) {
      reverse.putIfAbsent(slot.curiosId, slot.path);
    }
    pathByCuriosSlot = Map.copyOf(reverse);

    for (TrinketSlot slot : discovered.values()) {
      for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagFor(slot.path))) {
        CuriosApi.registerCurio(holder.value(),
            dev.arakiel.curiosfabric.compat.bridge.TrinketItemAdapter.isTrinket(holder.value())
                ? new dev.arakiel.curiosfabric.compat.bridge.TrinketItemAdapter(holder.value())
                : TrinketCurio.INSTANCE);
      }
    }
  }

  public static void invalidate() {
    slotsByPath = Map.of();
    pathByCuriosSlot = Map.of();
  }

  /** {@code true} after the first slot discovery has completed. */
  public static boolean hasDiscoveredSlots() {
    return !slotsByPath.isEmpty();
  }

  public static boolean isTrinketFor(ItemStack stack, String trinketsPath) {
    return isLoaded() && stack.is(tagFor(trinketsPath));
  }

  public static Set<String> slotsFor(ItemStack stack) {

    if (!isLoaded() || stack.isEmpty()) {
      return Set.of();
    }
    ensureSlots();
    Set<String> result = new HashSet<>();
    for (TrinketSlot slot : slotsByPath.values()) {
      if (stack.is(tagFor(slot.path))) {
        result.add(slot.curiosId);
      }
    }
    return result;
  }

  public static Optional<ICurioItem> lookup(ItemStack stack) {
    return slotsFor(stack).isEmpty() ? Optional.empty() : Optional.of(TrinketCurio.INSTANCE);
  }

  public static String curiosSlotOf(String trinketsPath) {
    ensureSlots();
    TrinketSlot slot = slotsByPath.get(trinketsPath);
    return slot == null ? null : slot.curiosId;
  }

  public static String trinketsPathOf(String curiosSlot) {
    ensureSlots();
    return pathByCuriosSlot.get(curiosSlot);
  }

  private static void ensureSlots() {
    if (!slotsByPath.isEmpty()) {
      return;
    }
    Map<String, TrinketSlot> discovered = flatten(discoverEntitySlots());
    Map<String, String> reverse = new HashMap<>();
    for (TrinketSlot slot : discovered.values()) {
      reverse.putIfAbsent(slot.curiosId, slot.path);
    }
    slotsByPath = Map.copyOf(discovered);
    pathByCuriosSlot = Map.copyOf(reverse);
  }

  private static ISlotType createSlotType(TrinketSlot descriptor) {
    SlotType.Builder builder = new SlotType.Builder(descriptor.curiosId)
        .source(descriptor.nativeSlot ? SlotSource.CURIOSFABRIC : SlotSource.TRINKETS)
        .order(descriptor.order, true)
        .size(descriptor.amount, "SET", true)
        .useNativeGui(true, true)
        .hasCosmetic(false, true)
        .renderToggle(false, true)
        .dropRule(descriptor.dropRule);
    if (descriptor.icon != null) {
      builder.icon(descriptor.icon);
    }
    return builder.build();
  }

  private static Map<String, TrinketSlot> flatten(
      Map<EntityType<?>, Map<String, TrinketSlot>> byEntity) {
    Map<String, TrinketSlot> result = new LinkedHashMap<>();
    byEntity.values().forEach(result::putAll);
    return result;
  }

  private static Map<EntityType<?>, Map<String, TrinketSlot>> discoverEntitySlots() {
    Map<EntityType<?>, Map<String, TrinketSlot>> result = new LinkedHashMap<>();
    Class<?> api = CompatTargets.loadClass(API + "TrinketsApi");
    if (api == null) {
      return result;
    }

    try {
      Method getEntitySlots = api.getMethod("getEntitySlots", EntityType.class);
      for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
        Object raw = getEntitySlots.invoke(null, entityType);
        if (!(raw instanceof Map<?, ?> groups)) {
          continue;
        }
        Map<String, TrinketSlot> slots = new LinkedHashMap<>();
        for (Map.Entry<?, ?> groupEntry : groups.entrySet()) {
          String groupName = String.valueOf(groupEntry.getKey());
          Object group = groupEntry.getValue();
          Object rawSlots = group.getClass().getMethod("getSlots").invoke(group);
          if (!(rawSlots instanceof Map<?, ?> slotMap)) {
            continue;
          }
          for (Map.Entry<?, ?> slotEntry : slotMap.entrySet()) {
            String slotName = String.valueOf(slotEntry.getKey());
            String path = groupName + "/" + slotName;
            Object slot = slotEntry.getValue();
            String nativeId = NativeSlotMappings.trinketsNative(path);
            String curiosId = nativeId != null
                ? nativeId : NativeSlotMappings.appendTrinketsSlot(path);
            int order = readInt(slot, "getOrder", 1000);
            int amount = Math.max(1, readInt(slot, "getAmount", 1));
            Object rawIcon = slot.getClass().getMethod("getIcon").invoke(slot);
            ResourceLocation icon =
                rawIcon instanceof ResourceLocation value ? value : FALLBACK_ICON;
            ICurio.DropRule dropRule = readDropRule(slot);
            slots.put(path, new TrinketSlot(path, curiosId, nativeId != null,
                order, amount, icon, dropRule));
          }
        }
        if (!slots.isEmpty()) {
          result.put(entityType, slots);
        }
      }
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug("Could not discover Trinkets slot groups", throwable);
    }
    return result;
  }

  /**
   * Resolves a Trinkets path to a Curios identifier.
   */
  static String curiosSlotId(String path) {
    String nativeId = NativeSlotMappings.trinketsNative(path);
    return nativeId != null ? nativeId : NativeSlotMappings.appendTrinketsSlot(path);
  }

  /** Resolves the Trinkets translation key associated with one of our mirrored slot ids. */
  public static Optional<String> translationKeyFor(String curiosSlot) {
    if (!isLoaded()) {
      return Optional.empty();
    }
    String path = trinketsPathOf(curiosSlot);
    return path == null ? Optional.empty() : Optional.of(translationKeyForPath(path));
  }

  static String translationKeyForPath(String path) {
    return "trinkets.slot." + path.replace('/', '.');
  }

  private static int readInt(Object target, String method, int fallback) {
    try {
      Object value = target.getClass().getMethod(method).invoke(target);
      return value instanceof Integer integer ? integer : fallback;
    } catch (Throwable throwable) {
      return fallback;
    }
  }

  private static ICurio.DropRule readDropRule(Object slot) {
    try {
      Object value = slot.getClass().getMethod("getDropRule").invoke(slot);
      if (value instanceof Enum<?> dropRule) {
        return ICurio.DropRule.valueOf(dropRule.name());
      }
    } catch (Throwable ignored) {
      // Fall through to DEFAULT.
    }
    return ICurio.DropRule.DEFAULT;
  }

  private static TagKey<Item> tagFor(String trinketsPath) {
    return TAG_CACHE.computeIfAbsent(trinketsPath, path -> TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, path)));
  }

  private record TrinketSlot(String path, String curiosId, boolean nativeSlot,
                             int order, int amount,
                             ResourceLocation icon, ICurio.DropRule dropRule) {
  }
}
