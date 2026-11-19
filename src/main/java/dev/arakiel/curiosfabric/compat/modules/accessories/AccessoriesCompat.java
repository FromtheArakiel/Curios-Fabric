package dev.arakiel.curiosfabric.compat.modules.accessories;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.compat.contribution.entity.EntitySlotBinding;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotCandidate;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotCandidateKey;
import dev.arakiel.curiosfabric.compat.CompatibilityRuntime;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeRefreshReason;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeSide;
import dev.arakiel.curiosfabric.api.event.CurioDropsEvent;
import dev.arakiel.curiosfabric.api.event.CurioChangeEvent;
import dev.arakiel.curiosfabric.api.event.CuriosEventBus;
import dev.arakiel.curiosfabric.api.event.SlotModifiersUpdatedEvent;
import dev.arakiel.curiosfabric.compat.modules.accessories.api.AccessoriesCapabilityFacade;
import dev.arakiel.curiosfabric.compat.modules.accessories.discovery.AccessoriesNativeMappings;
import dev.arakiel.curiosfabric.compat.modules.accessories.discovery.AccessoriesSlotIdentity;
import dev.arakiel.curiosfabric.compat.modules.accessories.discovery.AccessoriesTranslation;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessorySlotValidationComponent;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.api.events.OnDeathCallback;
import io.wispforest.accessories.api.events.AccessoryChangeCallback;
import io.wispforest.accessories.api.events.ContainersChangeCallback;
import io.wispforest.accessories.api.events.SlotStateChange;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/**
 * Strongly typed Accessories adapter, isolated from the always-loadable bridge entry point.
 *
 * <p>This class is only referenced after Accessories availability has been established.</p>
 */
public final class AccessoriesCompat {

  public static final String MOD_ID = "accessories";
  public static final ResourceLocation VALIDATOR = ResourceLocation.fromNamespaceAndPath(
      CuriosFabricConstants.MOD_ID, "accessories");
  private static final ResourceLocation FALLBACK_ICON = ResourceLocation.fromNamespaceAndPath(
      CuriosFabricConstants.MOD_ID, "slot/empty_curio_slot");

  private static final State CLIENT_STATE = new State();
  private static final State SERVER_STATE = new State();
  private static volatile Level serverLevel;
  private static volatile boolean predicateRegistered;
  private static volatile boolean levelEventsRegistered;

  private AccessoriesCompat() {
  }

  public static void initialize() {
    if (!predicateRegistered) {
      synchronized (AccessoriesCompat.class) {
        if (!predicateRegistered) {
          CuriosApi.registerCurioPredicate(VALIDATOR,
              result -> matches(result.slotContext(), result.stack()));
          CuriosEventBus.register(CurioDropsEvent.class, event -> {
            List<ItemStack> droppedStacks = event.getDrops().stream()
                .map(ItemEntity::getItem)
                .map(ItemStack::copy)
                .toList();
            TriState result = OnDeathCallback.EVENT.invoker().shouldDrop(
                TriState.DEFAULT,
                event.getEntity(),
                AccessoriesCapabilityFacade.of(event.getEntity()),
                event.getSource(),
                droppedStacks);
            if (!result.orElse(true)) {
              event.setCanceled(true);
            }
          });
          CuriosEventBus.register(CurioChangeEvent.class, event -> {
            var context = new dev.arakiel.curiosfabric.api.SlotContext(
                event.getIdentifier(), event.getEntity(), event.getSlotIndex(), false, true);
            SlotReference reference = referenceOrFallback(context, event.getTo());
            if (reference != null) {
              AccessoryChangeCallback.EVENT.invoker().onChange(
                  event.getFrom(), event.getTo(), reference,
                  ItemStack.isSameItem(event.getFrom(), event.getTo())
                      ? SlotStateChange.MUTATION : SlotStateChange.REPLACEMENT);
            }
          });
          CuriosEventBus.register(SlotModifiersUpdatedEvent.class, event -> {
            AccessoriesCapabilityFacade facade = AccessoriesCapabilityFacade.of(event.getEntity());
            Map<io.wispforest.accessories.api.AccessoriesContainer, Boolean> changed =
                new LinkedHashMap<>();
            for (String curiosId : event.getTypes()) {
              for (String upstream : upstreamNamesFor(
                  curiosId, event.getEntity().level().isClientSide())) {
                var container = facade.getContainers().get(upstream);
                if (container != null) {
                  changed.put(container, true);
                }
              }
            }
            if (!changed.isEmpty()) {
              ContainersChangeCallback.EVENT.invoker().onChange(
                  event.getEntity(), facade, changed);
            }
          });
          predicateRegistered = true;
        }
      }
    }
    if (!levelEventsRegistered) {
      synchronized (AccessoriesCompat.class) {
        if (!levelEventsRegistered) {
          ServerLifecycleEvents.SERVER_STARTED.register(server -> serverLevel = server.overworld());
          ServerLifecycleEvents.SERVER_STOPPED.register(server -> serverLevel = null);
          ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
              (server, resourceManager, success) -> {
                if (success) {
                  CompatibilityRuntime.refreshSource(
                      SlotSource.ACCESSORIES,
                      BridgeSide.SERVER,
                      BridgeRefreshReason.DATA_PACK_RELOAD,
                      true);
                }
              });
          levelEventsRegistered = true;
        }
      }
    }
  }

  public static void refresh(boolean client, String reason, boolean authoritative) {
    initialize();
    State state = state(client);
    Map<String, SlotType> discoveredSlots = filterSlots(
        new LinkedHashMap<>(SlotTypeLoader.INSTANCE.getSlotTypes(client)));
    Map<EntityType<?>, Map<String, SlotType>> discoveredEntities =
        copyEntityData(EntitySlotLoader.INSTANCE.getEntitySlotData(client));

    Map<String, SlotType> slots = filterSlots(authoritative
        ? discoveredSlots : mergeSlots(state.snapshot == null
            ? Map.of() : state.snapshot.upstreamSlots, discoveredSlots));
    Map<EntityType<?>, Map<String, SlotType>> entities = filterEntityData(authoritative
        ? discoveredEntities : mergeEntities(state.snapshot == null
            ? Map.of() : state.snapshot.entitySlots, discoveredEntities));

    if (slots.isEmpty() && state.snapshot != null && !state.snapshot.upstreamSlots.isEmpty()
        && !authoritative) {
      return;
    }

    state.snapshot = Snapshot.create(slots, entities);
    CuriosFabricConstants.LOG.info(
        "Accessories bridge [{}] side={} mode={} slots={} candidates={} entities={}",
        reason, client ? "client" : "server", authoritative ? "replace" : "merge",
        slots.size(), state.snapshot.candidates.size(), entities.size());
    if (!client && reason.equals("data_pack_reload")) {
      String mappings = state.snapshot.definitions.values().stream()
          .filter(definition -> !definition.nativeSlot)
          .map(definition -> definition.upstreamName + " -> "
              + definition.instanceId + " [" + definition.slot.translation() + "]")
          .collect(Collectors.joining(", "));
      CuriosFabricConstants.LOG.info("Accessories slot mappings: {}", mappings);
    }
  }

  public static void invalidate(boolean client) {
    state(client).snapshot = null;
  }

  public static void invalidateAll() {
    CLIENT_STATE.snapshot = null;
    SERVER_STATE.snapshot = null;
    dev.arakiel.curiosfabric.compat.modules.accessories.api.AccessoriesCapabilityFacade
        .clearCache();
  }

  public static List<SlotCandidate> candidates(boolean client) {
    return ensure(client).candidates;
  }

  public static List<EntitySlotBinding> entityBindings(boolean client) {
    return ensure(client).entityBindings;
  }

  public static Optional<String> nativeMappedId(String upstreamId) {
    return Optional.ofNullable(AccessoriesNativeMappings.nativeId(upstreamId));
  }

  public static String translationKey(String instanceId, String fallbackId, boolean client) {
    Definition definition = ensure(client).definitions.get(instanceId);
    if (definition != null) {
      return definition.slot.translation();
    }
    return AccessoriesTranslation.fallbackKey(fallbackId);
  }

  public static Set<String> slotsFor(
      ItemStack stack, @Nullable LivingEntity entity, boolean client) {
    if (stack.isEmpty()) {
      return Set.of();
    }
    Snapshot snapshot = ensure(client);
    Set<String> result = new LinkedHashSet<>();
    Collection<SlotType> slots;
    Level level = entity != null ? entity.level() : currentLevel(client);

    if (entity != null) {
      slots = EntitySlotLoader.getEntitySlots(entity).values();
    } else {
      slots = snapshot.upstreamSlots.values();
    }

    for (SlotType slot : slots) {
      if (matchesPredicates(slot, stack, level, entity)) {
        String curiosId = snapshot.curiosId(slot.name());
        if (curiosId != null) {
          result.add(curiosId);
        }
      }
    }
    return Set.copyOf(result);
  }

  public static boolean matches(
      dev.arakiel.curiosfabric.api.SlotContext context, ItemStack stack) {
    return matches(context, stack,
        context.entity() != null
            ? context.entity().level().isClientSide()
            : CuriosFabricConstants.IS_CLIENT);
  }

  public static boolean matches(
      dev.arakiel.curiosfabric.api.SlotContext context, ItemStack stack, boolean client) {
    if (stack.isEmpty()) {
      return false;
    }
    Snapshot snapshot = ensure(client);
    List<String> upstreamNames = snapshot.aliases.get(context.identifier());
    if (upstreamNames == null) {
      upstreamNames = List.of(context.identifier());
    }
    Level level = context.entity() != null
        ? context.entity().level() : currentLevel(client);
    for (String upstreamName : upstreamNames) {
      SlotType slot = snapshot.upstreamSlots.get(upstreamName);
      if (slot != null && matchesPredicates(slot, stack, level, context.entity())) {
        return true;
      }
    }
    return false;
  }

  public static boolean isAccessoriesItem(ItemStack stack, boolean client) {
    if (stack.isEmpty()) {
      return false;
    }
    if (AccessoriesAPI.getAccessory(stack.getItem()) != null
        || stack.is(AccessoriesTags.ANY_TAG)
        || stack.has(AccessoriesDataComponents.NESTED_ACCESSORIES)
        || stack.has(AccessoriesDataComponents.SLOT_VALIDATION)
        || stack.has(AccessoriesDataComponents.ATTRIBUTES)
        || stack.has(AccessoriesDataComponents.RENDER_OVERRIDE)
        || stack.has(AccessoriesDataComponents.RENDER_TRANSFORMATIONS)
        || stack.has(AccessoriesDataComponents.CUSTOM_RENDERER)
        || stack.has(AccessoriesDataComponents.STACK_SIZE)) {
      return true;
    }
    return !slotsFor(stack, null, client).isEmpty();
  }

  @Nullable
  public static SlotReference reference(
      dev.arakiel.curiosfabric.api.SlotContext context, ItemStack stack) {
    Snapshot snapshot = ensure(context.entity() != null
        ? context.entity().level().isClientSide()
        : CuriosFabricConstants.IS_CLIENT);
    String upstreamName = snapshot.resolveUpstream(
        context.identifier(), context.entity(), stack, context.index());
    if (upstreamName == null) {
      return null;
    }
    return SlotReference.of(context.entity(), upstreamName, Math.max(0, context.index()));
  }

  @Nullable
  public static SlotReference referenceOrFallback(
      dev.arakiel.curiosfabric.api.SlotContext context, ItemStack stack) {
    return referenceOrFallback(context, stack, null);
  }

  @Nullable
  public static SlotReference referenceOrFallback(
      dev.arakiel.curiosfabric.api.SlotContext context,
      ItemStack stack,
      @Nullable String fallbackSlot) {
    SlotReference reference = reference(context, stack);
    if (fallbackSlot == null && reference != null) {
      return reference;
    }
    boolean client = context.entity() != null
        ? context.entity().level().isClientSide() : CuriosFabricConstants.IS_CLIENT;
    if (fallbackSlot != null) {
      String fallbackCuriosId = curiosIdFor(fallbackSlot, client);
      if (fallbackCuriosId != null
          && ensure(client).upstreamSlots.containsKey(fallbackSlot)) {
        return SlotReference.of(
            context.entity(), fallbackSlot, Math.max(0, context.index()));
      }
    }
    if (reference != null) {
      return reference;
    }
    List<String> aliases = upstreamNamesFor(context.identifier(), client);
    if (aliases.isEmpty()) {
      return null;
    }
    return SlotReference.of(
        context.entity(), aliases.getFirst(), Math.max(0, context.index()));
  }

  public static String curiosIdFor(String upstreamName, boolean client) {
    return ensure(client).curiosId(upstreamName);
  }

  public static List<String> upstreamNamesFor(String curiosId, boolean client) {
    return ensure(client).aliases.getOrDefault(curiosId, List.of());
  }

  public static ICurio.DropRule mapDropRule(DropRule rule) {
    return switch (rule) {
      case KEEP -> ICurio.DropRule.ALWAYS_KEEP;
      case DROP -> ICurio.DropRule.ALWAYS_DROP;
      case DESTROY -> ICurio.DropRule.DESTROY;
      case DEFAULT -> ICurio.DropRule.DEFAULT;
    };
  }

  public static boolean allowsEquipFromUse(SlotReference reference) {
    return io.wispforest.accessories.api.slot.ExtraSlotTypeProperties
        .getProperty(reference.slotName(), reference.entity().level().isClientSide())
        .allowEquipFromUse();
  }

  @Nullable
  public static SlotType slotType(String upstreamName, boolean client) {
    return ensure(client).upstreamSlots.get(upstreamName);
  }

  public static Collection<SlotType> slots(boolean client) {
    return ensure(client).upstreamSlots.values();
  }

  public static Optional<String> upstreamNameForInstance(String instanceId, boolean client) {
    Definition definition = ensure(client).definitions.get(instanceId);
    return definition == null ? Optional.empty() : Optional.of(definition.upstreamName);
  }

  public static Optional<String> knownUpstreamName(String upstreamName, boolean client) {
    return ensure(client).upstreamSlots.containsKey(upstreamName)
        ? Optional.of(upstreamName) : Optional.empty();
  }

  public static ResourceLocation slotTag(String upstreamName, boolean client) {
    SlotType slot = ensure(client).upstreamSlots.get(upstreamName);
    if (slot == null) {
      return ResourceLocation.fromNamespaceAndPath(MOD_ID, upstreamName);
    }
    return AccessoriesAPI.getSlotTag(slot).location();
  }

  private static boolean matchesPredicates(
      SlotType slot,
      ItemStack stack,
      @Nullable Level level,
      @Nullable LivingEntity entity) {
    if (level != null) {
      return AccessoriesAPI.getPredicateResults(
          slot.validators(), level, entity, slot, 0, stack);
    }
    return manualPredicateMatch(slot, stack);
  }

  private static boolean manualPredicateMatch(SlotType slot, ItemStack stack) {
    Set<ResourceLocation> validators = slot.validators();
    if (stack.is(AccessoriesTags.ANY_TAG)
        || stack.is(AccessoriesAPI.getSlotTag(slot))) {
      return true;
    }

    AccessorySlotValidationComponent validation =
        stack.get(AccessoriesDataComponents.SLOT_VALIDATION);
    if (validation != null) {
      if (validation.invalidSlotOverrides().contains(slot.name())) {
        return false;
      }
      if (validation.validSlotOverrides().contains("any")
          || validation.validSlotOverrides().contains(slot.name())) {
        return true;
      }
    }

    if (validators.contains(ResourceLocation.fromNamespaceAndPath(MOD_ID, "none"))) {
      return false;
    }
    if (validators.contains(ResourceLocation.fromNamespaceAndPath(MOD_ID, "all"))) {
      return true;
    }
    if (validators.contains(ResourceLocation.fromNamespaceAndPath(MOD_ID, "relevant"))) {
      return !AccessoriesAPI.getAttributeModifiers(stack, null, slot.name(), 0)
          .getAttributeModifiers(false).isEmpty();
    }
    return false;
  }

  private static Snapshot ensure(boolean client) {
    State state = state(client);
    Snapshot snapshot = state.snapshot;
    if (snapshot == null) {
      refresh(client, "lazy", false);
      snapshot = state.snapshot;
    }
    return snapshot == null ? Snapshot.empty() : snapshot;
  }

  private static State state(boolean client) {
    return client ? CLIENT_STATE : SERVER_STATE;
  }

  private static Level currentLevel(boolean client) {
    return client ? AccessoriesClientContext.level() : serverLevel;
  }

  private static Map<String, SlotType> mergeSlots(
      Map<String, SlotType> previous,
      Map<String, SlotType> discovered) {
    if (discovered.isEmpty()) {
      return previous;
    }
    Map<String, SlotType> merged = new LinkedHashMap<>(previous);
    merged.putAll(discovered);
    return Collections.unmodifiableMap(merged);
  }

  private static Map<EntityType<?>, Map<String, SlotType>> mergeEntities(
      Map<EntityType<?>, Map<String, SlotType>> previous,
      Map<EntityType<?>, Map<String, SlotType>> discovered) {
    if (discovered.isEmpty()) {
      return previous;
    }
    Map<EntityType<?>, Map<String, SlotType>> merged =
        new LinkedHashMap<>(previous);
    discovered.forEach((type, slots) -> {
      Map<String, SlotType> mergedSlots =
          new LinkedHashMap<>(merged.getOrDefault(type, Map.of()));
      mergedSlots.putAll(slots);
      merged.put(type, Collections.unmodifiableMap(mergedSlots));
    });
    return Collections.unmodifiableMap(merged);
  }

  private static Map<EntityType<?>, Map<String, SlotType>> copyEntityData(
      Map<EntityType<?>, ? extends Map<String, SlotType>> source) {
    Map<EntityType<?>, Map<String, SlotType>> result = new LinkedHashMap<>();
    source.forEach((type, slots) -> {
      Map<String, SlotType> filtered = filterSlots(slots);
      if (!filtered.isEmpty()) {
        result.put(type, filtered);
      }
    });
    return Collections.unmodifiableMap(result);
  }

  private static Map<String, SlotType> filterSlots(Map<String, SlotType> slots) {
    Map<String, SlotType> result = new LinkedHashMap<>();
    slots.forEach((name, slot) -> {
      if (!AccessoriesNativeMappings.isIgnored(name)) {
        result.put(name, slot);
      }
    });
    return Collections.unmodifiableMap(result);
  }

  private static Map<EntityType<?>, Map<String, SlotType>> filterEntityData(
      Map<EntityType<?>, ? extends Map<String, SlotType>> source) {
    Map<EntityType<?>, Map<String, SlotType>> result = new LinkedHashMap<>();
    source.forEach((type, slots) -> {
      Map<String, SlotType> filtered = filterSlots(slots);
      if (!filtered.isEmpty()) {
        result.put(type, filtered);
      }
    });
    return Collections.unmodifiableMap(result);
  }

  private static final class State {
    private volatile Snapshot snapshot;
  }

  private record Definition(
      String upstreamName,
      String instanceId,
      String mappingId,
      boolean nativeSlot,
      SlotType slot) {
  }

  private record Snapshot(
      Map<String, SlotType> upstreamSlots,
      Map<EntityType<?>, Map<String, SlotType>> entitySlots,
      Map<String, Definition> definitions,
      Map<String, List<String>> aliases,
      List<SlotCandidate> candidates,
      List<EntitySlotBinding> entityBindings) {

    private static Snapshot empty() {
      return new Snapshot(Map.of(), Map.of(), Map.of(), Map.of(), List.of(), List.of());
    }

    private static Snapshot create(
        Map<String, SlotType> slots,
        Map<EntityType<?>, Map<String, SlotType>> entities) {
      Map<String, Definition> definitions = new LinkedHashMap<>();
      Set<String> claimedIds = new LinkedHashSet<>();
      Map<String, List<String>> aliases = new LinkedHashMap<>();

      for (SlotType slot : slots.values()) {
        String upstreamName = slot.name();
        if (AccessoriesNativeMappings.isIgnored(upstreamName)) {
          continue;
        }
        String nativeId = AccessoriesNativeMappings.nativeId(upstreamName);
        String instanceId = nativeId != null
            ? nativeId : AccessoriesSlotIdentity.uniqueId(
                "accessories_", upstreamName, claimedIds);
        String mappingId = nativeId != null ? nativeId : instanceId;
        claimedIds.add(instanceId);
        Definition definition = new Definition(
            upstreamName, instanceId, mappingId, nativeId != null, slot);
        definitions.put(instanceId, definition);
        aliases.computeIfAbsent(instanceId, ignored -> new ArrayList<>()).add(upstreamName);
      }

      List<SlotCandidate> candidates = new ArrayList<>();
      for (Definition definition : definitions.values()) {
        if (definition.nativeSlot) {
          continue;
        }
        ISlotType slotType = createSlotType(definition);
        candidates.add(new SlotCandidate(
            new SlotCandidateKey(SlotSource.ACCESSORIES, definition.upstreamName),
            definition.instanceId,
            definition.mappingId,
            SlotSource.ACCESSORIES,
            false,
            slotType));
      }

      List<EntitySlotBinding> bindings = new ArrayList<>();
      entities.forEach((entityType, entitySlotMap) -> {
        Set<String> ids = new LinkedHashSet<>();
        for (String upstreamName : entitySlotMap.keySet()) {
          Definition definition = definition(definitions, upstreamName);
          if (definition != null) {
            ids.add(definition.instanceId);
          }
        }
        if (!ids.isEmpty()) {
          bindings.add(new EntitySlotBinding(entityType, ids));
        }
      });

      Map<String, List<String>> frozenAliases = new LinkedHashMap<>();
      aliases.forEach((id, names) -> frozenAliases.put(id, List.copyOf(names)));
      return new Snapshot(
          Collections.unmodifiableMap(new LinkedHashMap<>(slots)),
          Collections.unmodifiableMap(new LinkedHashMap<>(entities)),
          Collections.unmodifiableMap(new LinkedHashMap<>(definitions)),
          Collections.unmodifiableMap(new LinkedHashMap<>(frozenAliases)),
          List.copyOf(candidates), List.copyOf(bindings));
    }

    private String curiosId(String upstreamName) {
      Definition definition = definition(definitions, upstreamName);
      return definition == null ? null : definition.instanceId;
    }

    @Nullable
    private String resolveUpstream(
        String curiosId,
        @Nullable LivingEntity entity,
        ItemStack stack,
        int index) {
      List<String> names = aliases.get(curiosId);
      if (names == null) {
        names = List.of(curiosId);
      }
      String fallback = null;
      for (String name : names) {
        SlotType slot = upstreamSlots.get(name);
        if (slot == null) {
          continue;
        }
        if (entity != null && !EntitySlotLoader.getEntitySlots(entity).containsKey(name)) {
          continue;
        }
        if (fallback == null) {
          fallback = name;
        }
        if (stack.isEmpty() || entity == null
            || matchesPredicates(slot, stack, entity.level(), entity)) {
          return name;
        }
      }
      return fallback;
    }

    private static Definition definition(
        Map<String, Definition> definitions, String upstreamName) {
      for (Definition definition : definitions.values()) {
        if (definition.upstreamName.equals(upstreamName)) {
          return definition;
        }
      }
      return null;
    }

    private static ISlotType createSlotType(Definition definition) {
      SlotType slot = definition.slot;
      dev.arakiel.curiosfabric.common.slottype.SlotType.Builder builder =
          new dev.arakiel.curiosfabric.common.slottype.SlotType.Builder(definition.instanceId)
          .mappingIdentifier(definition.mappingId)
          .source(SlotSource.ACCESSORIES)
          .icon(AccessoriesSlotIdentity.normalizeIcon(slot.icon(), FALLBACK_ICON))
          .size(Math.max(0, slot.amount()), "SET", true)
          .useNativeGui(true, true)
          .hasCosmetic(true, true)
          .renderToggle(true, true)
          .dropRule(mapDropRule(slot.dropRule()))
          .validator(VALIDATOR);
      return builder.build();
    }
  }
}
