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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
  private static final String ENTITY_SLOT_LOADER = "dev.emi.trinkets.data.EntitySlotLoader";
  private static final ResourceLocation FALLBACK_ICON =
      ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID,
          "slot/empty_curio_slot");
  private static final Map<String, TagKey<Item>> TAG_CACHE = new HashMap<>();

  private static final State CLIENT_STATE = new State(true);
  private static final State SERVER_STATE = new State(false);

  private TrinketsCompat() {
  }

  public static boolean isLoaded() {
    return CompatTargets.isTrinketsLoaded();
  }

  /**
   * Re-discovers all slot groups and mirrors their slot types, entity assignments and item tags.
   *
   * <p>A normal refresh merges the new discovery into the previous mapping. Only a complete data
   * pack reload is authoritative and may remove slots that no longer exist.</p>
   */
  public static void register() {
    register(CuriosFabricConstants.IS_CLIENT, "unspecified", false);
  }

  /**
   * Refreshes the dynamic bridge.
   *
   * @param reason        lifecycle context included in diagnostics
   * @param authoritative whether discovery is a complete data-pack snapshot
   */
  public static void register(String reason, boolean authoritative) {
    register(CuriosFabricConstants.IS_CLIENT, reason, authoritative);
  }

  public static void register(boolean client, String reason, boolean authoritative) {
    if (!isLoaded()) {
      invalidate(client);
      CuriosFabricConstants.LOG.debug(
          "Trinkets bridge refresh [{}] skipped because Trinkets is not loaded", reason);
      return;
    }

    State state = state(client);
    Map<EntityType<?>, Map<String, TrinketSlot>> discoveredByEntity =
        discoverEntitySlots(client);
    Map<EntityType<?>, Map<String, TrinketSlot>> byEntity =
        mergeEntityMappings(state.slotsByEntity, discoveredByEntity, authoritative);
    Map<String, TrinketSlot> discovered = flatten(byEntity);
    Map<String, TrinketSlot> previous = state.slotsByPath;
    if (!discovered.isEmpty()) {
      CuriosFabricConstants.LOG.info(
          "Discovered {} Trinkets slot definitions across {} entity types during {}",
          flatten(discoveredByEntity).size(), discoveredByEntity.size(), reason);
    }
    Map<String, ISlotType> generatedSlots = new HashMap<>();

    for (TrinketSlot slot : discovered.values()) {
      if (CuriosApi.getSlot(slot.curiosId, client).isEmpty()) {
        generatedSlots.computeIfAbsent(slot.curiosId, ignored -> createSlotType(slot));
      }
    }
    if (!client) {
      CuriosSlotManager.SERVER.mergeSlots(generatedSlots, MOD_ID);
    }
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
      slots.forEach((path, slot) -> CuriosApi.getSlot(slot.curiosId, client)
          .ifPresent(curiosSlot -> curiosSlots.put(slot.curiosId, curiosSlot)));
      if (!curiosSlots.isEmpty()) {
        entitySlots.put(entityType, curiosSlots);
      }
    });
    if (!client) {
      CuriosEntityManager.SERVER.mergeEntitySlots(entitySlots);
    }

    state.slotsByEntity = freezeEntityMappings(byEntity);
    state.slotsByPath = Map.copyOf(discovered);
    Map<String, String> reverse = new HashMap<>();
    for (TrinketSlot slot : discovered.values()) {
      reverse.putIfAbsent(slot.curiosId, slot.path);
    }
    state.pathByCuriosSlot = Map.copyOf(reverse);
    logMappingState(client, reason, authoritative, previous, discovered);

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
    invalidate(CuriosFabricConstants.IS_CLIENT);
  }

  public static void invalidate(boolean client) {
    State state = state(client);
    state.slotsByPath = Map.of();
    state.slotsByEntity = Map.of();
    state.pathByCuriosSlot = Map.of();
  }

  /** {@code true} after the first slot discovery has completed. */
  public static boolean hasDiscoveredSlots() {
    return hasDiscoveredSlots(CuriosFabricConstants.IS_CLIENT);
  }

  public static boolean hasDiscoveredSlots(boolean client) {
    return !state(client).slotsByPath.isEmpty();
  }

  public static boolean isTrinketFor(ItemStack stack, String trinketsPath) {
    String curiosId = curiosSlotId(trinketsPath);
    return isLoaded() && (stack.is(tagFor(trinketsPath))
        || stack.is(curiosTagFor(curiosId)));
  }

  public static Set<String> slotsFor(ItemStack stack) {
    return slotsFor(stack, CuriosFabricConstants.IS_CLIENT);
  }

  public static Set<String> slotsFor(ItemStack stack, boolean client) {

    if (!isLoaded() || stack.isEmpty()) {
      return Set.of();
    }
    ensureSlots(client);
    State state = state(client);
    Set<String> result = new HashSet<>();
    for (TrinketSlot slot : state.slotsByPath.values()) {
      if (stack.is(tagFor(slot.path)) || stack.is(curiosTagFor(slot.curiosId))) {
        result.add(slot.curiosId);
      }
    }
    if (client) {
      for (String slotId : CuriosApi.getSlots(true).keySet()) {
        if (stack.is(curiosTagFor(slotId))) {
          result.add(slotId);
        }
      }
    }
    return result;
  }

  public static Optional<ICurioItem> lookup(ItemStack stack) {
    return slotsFor(stack, false).isEmpty() && slotsFor(stack, true).isEmpty()
        ? Optional.empty() : Optional.of(TrinketCurio.INSTANCE);
  }

  public static String curiosSlotOf(String trinketsPath) {
    return curiosSlotOf(trinketsPath, CuriosFabricConstants.IS_CLIENT);
  }

  public static String curiosSlotOf(String trinketsPath, boolean client) {
    ensureSlots(client);
    TrinketSlot slot = state(client).slotsByPath.get(trinketsPath);
    return slot == null ? null : slot.curiosId;
  }

  public static String trinketsPathOf(String curiosSlot) {
    return trinketsPathOf(curiosSlot, CuriosFabricConstants.IS_CLIENT);
  }

  public static String trinketsPathOf(String curiosSlot, boolean client) {
    ensureSlots(client);
    return state(client).pathByCuriosSlot.get(curiosSlot);
  }

  /** Entity-to-slot identifiers currently supplied by Trinkets. */
  public static Map<EntityType<?>, Set<String>> entitySlotIds() {
    return entitySlotIds(false);
  }

  public static Map<EntityType<?>, Set<String>> entitySlotIds(boolean client) {
    ensureSlots(client);
    Map<EntityType<?>, Set<String>> result = new LinkedHashMap<>();
    state(client).slotsByEntity.forEach((type, slots) -> result.put(type,
        slots.values().stream().map(TrinketSlot::curiosId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))));
    return result;
  }

  private static void ensureSlots(boolean client) {
    State state = state(client);
    if (!state.slotsByPath.isEmpty()) {
      return;
    }
    Map<EntityType<?>, Map<String, TrinketSlot>> byEntity = discoverEntitySlots(client);
    Map<String, TrinketSlot> discovered = flatten(byEntity);
    Map<String, String> reverse = new HashMap<>();
    for (TrinketSlot slot : discovered.values()) {
      reverse.putIfAbsent(slot.curiosId, slot.path);
    }
    state.slotsByEntity = freezeEntityMappings(byEntity);
    state.slotsByPath = Map.copyOf(discovered);
    state.pathByCuriosSlot = Map.copyOf(reverse);
  }

  private static Map<EntityType<?>, Map<String, TrinketSlot>> mergeEntityMappings(
      Map<EntityType<?>, Map<String, TrinketSlot>> previous,
      Map<EntityType<?>, Map<String, TrinketSlot>> discovered,
      boolean authoritative) {
    Map<EntityType<?>, Map<String, TrinketSlot>> merged = new LinkedHashMap<>();
    if (!authoritative) {
      previous.forEach((entityType, slots) ->
          merged.put(entityType, new LinkedHashMap<>(slots)));
    }
    discovered.forEach((entityType, slots) -> {
      Map<String, TrinketSlot> entitySlots =
          new LinkedHashMap<>(merged.getOrDefault(entityType, Map.of()));
      entitySlots.putAll(slots);
      merged.put(entityType, entitySlots);
    });
    return merged;
  }

  /**
   * Merges path-indexed mappings. Kept package-private so the lifecycle semantics can be tested
   * without starting a Minecraft server.
   */
  static Map<String, TrinketSlot> mergeSlotMappings(Map<String, TrinketSlot> previous,
                                                    Map<String, TrinketSlot> discovered,
                                                    boolean authoritative) {
    if (authoritative) {
      return Collections.unmodifiableMap(new LinkedHashMap<>(discovered));
    }
    Map<String, TrinketSlot> merged = new LinkedHashMap<>(previous);
    merged.putAll(discovered);
    return Collections.unmodifiableMap(merged);
  }

  private static Map<EntityType<?>, Map<String, TrinketSlot>> freezeEntityMappings(
      Map<EntityType<?>, Map<String, TrinketSlot>> mappings) {
    Map<EntityType<?>, Map<String, TrinketSlot>> frozen = new LinkedHashMap<>();
    mappings.forEach(
        (entityType, slots) -> frozen.put(entityType, Collections.unmodifiableMap(slots)));
    return Collections.unmodifiableMap(frozen);
  }

  private static void logMappingState(boolean client, String reason, boolean authoritative,
                                      Map<String, TrinketSlot> previous,
                                      Map<String, TrinketSlot> effective) {
    Set<String> added = new HashSet<>(effective.keySet());
    added.removeAll(previous.keySet());
    Set<String> retained = new HashSet<>(previous.keySet());
    retained.retainAll(effective.keySet());
    Set<String> removed = new HashSet<>(previous.keySet());
    removed.removeAll(effective.keySet());

    CuriosFabricConstants.LOG.info(
        "Trinkets bridge [{}] side={} mode={} entities={} mappedSlots={} added={} retained={} removed={}",
        reason, client ? "client" : "server", authoritative ? "replace" : "merge",
        state(client).slotsByEntity.size(), effective.size(),
        added.size(), retained.size(), removed.size());
    CuriosFabricConstants.LOG.debug(
        "Trinkets bridge [{}] effective paths={} added={} retained={} removed={}",
        reason, effective.keySet(), added, retained, removed);
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
    if (descriptor.validators.isEmpty()) {
      builder.validator(ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "tag"));
    } else {
      descriptor.validators.forEach(builder::validator);
    }
    return builder.build();
  }

  private static Map<String, TrinketSlot> flatten(
      Map<EntityType<?>, Map<String, TrinketSlot>> byEntity) {
    Map<String, TrinketSlot> result = new LinkedHashMap<>();
    byEntity.values().forEach(result::putAll);
    return result;
  }

  private static Map<EntityType<?>, Map<String, TrinketSlot>> discoverEntitySlots(boolean client) {
    Map<EntityType<?>, Map<String, TrinketSlot>> result = new LinkedHashMap<>();
    Map<String, RawTrinketSlots.RawSlot> rawSlotMetadata = RawTrinketSlots.read();
    Class<?> loaderClass = CompatTargets.loadClass(ENTITY_SLOT_LOADER);
    if (loaderClass == null) {
      return result;
    }

    try {
      String loaderName = client ? "CLIENT" : "SERVER";
      Object loader = loaderClass.getField(loaderName).get(null);
      Method getEntitySlots = loaderClass.getMethod("getEntitySlots", EntityType.class);
      CuriosFabricConstants.LOG.debug(
          "Reading Trinkets slots directly from EntitySlotLoader.{}", loaderName);
      for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
        Object raw = getEntitySlots.invoke(loader, entityType);
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
            RawTrinketSlots.RawSlot rawMetadata = rawSlotMetadata.get(path);
            int order = rawMetadata != null
                ? rawMetadata.order() : readInt(slot, "getOrder", 1000);
            int amount = Math.max(1,
                rawMetadata != null ? rawMetadata.amount() : readInt(slot, "getAmount", 1));
            Object rawIcon = slot.getClass().getMethod("getIcon").invoke(slot);
            ResourceLocation icon = rawMetadata != null
                ? ResourceLocation.tryParse(rawMetadata.icon())
                : rawIcon instanceof ResourceLocation value ? normalizeIcon(value) : FALLBACK_ICON;
            if (icon == null) {
              icon = FALLBACK_ICON;
            }
            ICurio.DropRule dropRule =
                rawMetadata != null
                    ? readDropRule(rawMetadata.dropRule()) : readDropRule(slot);
            Set<ResourceLocation> validators =
                rawMetadata != null
                    ? mapValidators(rawMetadata.validators()) : readValidators(slot);
            slots.put(path, new TrinketSlot(path, curiosId, nativeId != null,
                order, amount, icon, validators, dropRule));
          }
        }
        if (!slots.isEmpty()) {
          result.put(entityType, slots);
        }
      }
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug("Could not discover Trinkets slot groups", throwable);
    }
    if (client && result.isEmpty() && !rawSlotMetadata.isEmpty()) {
      Map<String, TrinketSlot> slots = new LinkedHashMap<>();
      rawSlotMetadata.forEach((path, raw) -> {
        String nativeId = NativeSlotMappings.trinketsNative(path);
        String curiosId = nativeId != null
            ? nativeId : NativeSlotMappings.appendTrinketsSlot(path);
        ResourceLocation icon = ResourceLocation.tryParse(raw.icon());
        slots.put(path, new TrinketSlot(path, curiosId, nativeId != null,
            raw.order(), Math.max(1, raw.amount()),
            icon == null ? FALLBACK_ICON : icon, mapValidators(raw.validators()),
            readDropRule(raw.dropRule())));
      });
      result.put(EntityType.PLAYER, slots);
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

  static ResourceLocation normalizeIcon(ResourceLocation icon) {
    String path = icon.getPath();
    if (path.startsWith("textures/") && path.endsWith(".png")) {
      return ResourceLocation.fromNamespaceAndPath(icon.getNamespace(),
          path.substring("textures/".length(), path.length() - ".png".length()));
    }
    return icon;
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

  private static ICurio.DropRule readDropRule(String dropRule) {
    try {
      return ICurio.DropRule.valueOf(dropRule);
    } catch (IllegalArgumentException exception) {
      return ICurio.DropRule.DEFAULT;
    }
  }

  private static Set<ResourceLocation> readValidators(Object slot) {
    try {
      Object raw = slot.getClass().getMethod("getValidatorPredicates").invoke(slot);
      if (raw instanceof Iterable<?> values) {
        Set<String> paths = new HashSet<>();
        for (Object value : values) {
          if (value instanceof ResourceLocation location) {
            paths.add(location.toString());
          }
        }
        return mapValidators(paths);
      }
    } catch (Throwable ignored) {
      // Fall back to the default tag validator.
    }
    return Set.of();
  }

  private static Set<ResourceLocation> mapValidators(Iterable<String> validators) {
    Set<ResourceLocation> result = new HashSet<>();
    for (String validator : validators) {
      String mapped = switch (validator) {
        case "trinkets:all" -> "all";
        case "trinkets:none" -> "none";
        case "trinkets:tag", "trinkets:relevant" -> "tag";
        default -> null;
      };
      if (mapped != null) {
        result.add(ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, mapped));
      }
    }
    return result;
  }

  private static TagKey<Item> tagFor(String trinketsPath) {
    return TAG_CACHE.computeIfAbsent(trinketsPath, path -> TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, path)));
  }

  private static TagKey<Item> curiosTagFor(String slotId) {
    return TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, slotId));
  }

  private static State state(boolean client) {
    return client ? CLIENT_STATE : SERVER_STATE;
  }

  private static final class State {
    private volatile Map<String, TrinketSlot> slotsByPath = Map.of();
    private volatile Map<EntityType<?>, Map<String, TrinketSlot>> slotsByEntity = Map.of();
    private volatile Map<String, String> pathByCuriosSlot = Map.of();

    private State(boolean client) {
    }
  }

  record TrinketSlot(String path, String curiosId, boolean nativeSlot,
                     int order, int amount,
                     ResourceLocation icon, Set<ResourceLocation> validators,
                     ICurio.DropRule dropRule) {
  }
}
