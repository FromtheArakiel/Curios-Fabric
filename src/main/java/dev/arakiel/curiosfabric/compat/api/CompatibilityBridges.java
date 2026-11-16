package dev.arakiel.curiosfabric.compat.api;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.common.data.CuriosEntityManager;
import dev.arakiel.curiosfabric.common.data.CuriosSlotManager;
import dev.arakiel.curiosfabric.compat.bridge.NeoforgeCuriosApiBridge;
import dev.arakiel.curiosfabric.compat.bridge.TrinketsApiBridge;
import dev.arakiel.curiosfabric.compat.neoforgecurios.NeoforgeCuriosCompat;
import dev.arakiel.curiosfabric.compat.trinkets.TrinketsCompat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/** Registered compatibility modules, expressed through their individual bridge facets. */
public final class CompatibilityBridges {

  private static final Set<String> REPORTED_RENDER_ROUTES = ConcurrentHashMap.newKeySet();

  private static final CompatibilityBridge TRINKETS = new CompatibilityBridge() {
    @Override
    public String modId() {
      return TrinketsCompat.MOD_ID;
    }

    @Override
    public SlotSource source() {
      return SlotSource.TRINKETS;
    }

    @Override
    public boolean isLoaded() {
      return TrinketsCompat.isLoaded();
    }

    @Override
    public SlotMetadataBridge metadata() {
      return (reason, authoritative) ->
          TrinketsCompat.register(false, reason, authoritative);
    }

    @Override
    public ItemTagBridge tags() {
      return new ItemTagBridge() {
        @Override
        public Set<String> slotsFor(ItemStack stack, boolean client) {
          return TrinketsCompat.slotsFor(stack, client);
        }

        @Override
        public boolean matches(String slotId, ItemStack stack, boolean client) {
          String path = TrinketsCompat.trinketsPathOf(slotId, client);
          return path != null && TrinketsCompat.isTrinketFor(stack, path);
        }

        @Override
        public boolean originatesFrom(String mappingId, ItemStack stack, boolean client) {
          return TrinketsCompat.hasTrinketsItemTag(mappingId, stack, client);
        }
      };
    }

    @Override
    public EntityBindingBridge entityBindings() {
      return TrinketsCompat::entitySlotIds;
    }

    @Override
    public ItemBehaviorBridge itemBehavior() {
      return TrinketsCompat::lookup;
    }

    @Override
    public RendererBridge renderer() {
      return new RendererBridge() {
        @Override
        public boolean supports(ItemStack stack) {
          return TrinketsApiBridge.hasRenderer(stack);
        }

        @Override
        public boolean render(ItemStack stack, SlotContext context, Object[] arguments) {
          Object reference = TrinketsApiBridge.slotReference(context.identifier(),
              context.index(), context.entity(), stack);
          return TrinketsApiBridge.renderForeign(prepend(stack, reference,
              ForeignRenderArguments.trinkets(arguments)));
        }
      };
    }
  };

  private static final CompatibilityBridge NEOFORGE_CURIOS = new CompatibilityBridge() {
    @Override
    public String modId() {
      return NeoforgeCuriosCompat.NEOFORGE_MOD_ID;
    }

    @Override
    public SlotSource source() {
      return SlotSource.NEOFORGE_CURIOS;
    }

    @Override
    public boolean isLoaded() {
      return NeoforgeCuriosCompat.isLoaded();
    }

    @Override
    public SlotMetadataBridge metadata() {
      return (reason, authoritative) ->
          NeoforgeCuriosCompat.register(CuriosSlotManager.SERVER.getSlots().keySet());
    }

    @Override
    public ItemTagBridge tags() {
      return new ItemTagBridge() {
        @Override
        public Set<String> slotsFor(ItemStack stack, boolean client) {
          return NeoforgeCuriosCompat.slotsFor(stack, client);
        }

        @Override
        public boolean matches(String slotId, ItemStack stack, boolean client) {
          return NeoforgeCuriosCompat.isNeoforgeCuriosItemFor(slotId, stack);
        }

        @Override
        public boolean originatesFrom(String mappingId, ItemStack stack, boolean client) {
          return NeoforgeCuriosCompat.hasNeoforgeItemTag(mappingId, stack, client);
        }
      };
    }

    @Override
    public EntityBindingBridge entityBindings() {
      return client -> {
        Map<EntityType<?>, Set<String>> result = new LinkedHashMap<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
          Set<String> slots = new LinkedHashSet<>(
              CuriosApi.getEntitySlots(type, client).keySet());
          if (!slots.isEmpty()) {
            result.put(type, slots);
          }
        }
        return result;
      };
    }

    @Override
    public ItemBehaviorBridge itemBehavior() {
      return NeoforgeCuriosCompat::lookup;
    }

    @Override
    public RendererBridge renderer() {
      return new RendererBridge() {
        @Override
        public boolean supports(ItemStack stack) {
          return NeoforgeCuriosApiBridge.hasRenderer(stack);
        }

        @Override
        public boolean render(ItemStack stack, SlotContext context, Object[] arguments) {
          return NeoforgeCuriosApiBridge.renderForeign(prepend(stack,
              NeoforgeCuriosApiBridge.foreignContext(context),
              ForeignRenderArguments.neoforge(arguments)));
        }
      };
    }
  };

  private static final List<CompatibilityBridge> ALL =
      List.of(TRINKETS, NEOFORGE_CURIOS);

  private CompatibilityBridges() {
  }

  public static List<CompatibilityBridge> active() {
    return ALL.stream().filter(CompatibilityBridge::isLoaded).toList();
  }

  public static List<ItemTagBridge> itemTags() {
    return active().stream().map(CompatibilityBridge::tags).toList();
  }

  public static List<ItemBehaviorBridge> itemBehaviors() {
    return active().stream().map(CompatibilityBridge::itemBehavior).toList();
  }

  /**
   * Selects exactly one renderer source. Default CuriosFabric slots may contain items supplied by an
   * upstream API, so those fall back to the item's real upstream tag rather than probing renderers.
   */
  public static Optional<RendererBridge> rendererFor(ISlotType slotType, ItemStack stack,
                                                     boolean client) {
    return rendererSourceFor(slotType, stack, client)
        .flatMap(source -> ALL.stream()
            .filter(CompatibilityBridge::isLoaded)
            .filter(bridge -> bridge.source() == source)
            .findFirst())
        .map(CompatibilityBridge::renderer);
  }

  /** Resolves the compatibility API that owns rendering for the source or upstream item. */
  public static Optional<SlotSource> rendererSourceFor(ISlotType slotType, ItemStack stack,
                                                       boolean client) {
    SlotSource source = slotType == null ? SlotSource.CURIOSFABRIC : slotType.getSource();
    List<CompatibilityBridge> active = active();
    String mappingId = slotType == null ? null : slotType.getMappingIdentifier();
    Optional<SlotSource> resolved =
        resolveRendererSource(source, mappingId, stack, client, active);
    String reportKey = BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|"
        + (slotType == null ? "<missing>" : slotType.getIdentifier());
    if (REPORTED_RENDER_ROUTES.add(reportKey)) {
      List<String> tagEvidence = active.stream()
          .map(bridge -> bridge.source().id() + "="
              + (mappingId != null
                  && bridge.tags().originatesFrom(mappingId, stack, client)))
          .toList();
      List<String> rendererEvidence = active.stream()
          .map(bridge -> bridge.source().id() + "=" + bridge.renderer().supports(stack))
          .toList();
      CuriosFabricConstants.LOG.info(
          "CURIOS_RENDER_DEBUG route item={} slot={} slotSource={} tags={} renderers={} resolved={}",
          BuiltInRegistries.ITEM.getKey(stack.getItem()),
          slotType == null ? "<missing>" : slotType.getIdentifier(), source,
          tagEvidence, rendererEvidence, resolved.orElse(null));
    }
    return resolved;
  }

  static Optional<SlotSource> resolveRendererSource(SlotSource source, String mappingId,
                                                    ItemStack stack, boolean client,
                                                    List<CompatibilityBridge> bridges) {
    if (source != SlotSource.CURIOSFABRIC) {
      return bridges.stream()
          .filter(bridge -> bridge.source() == source)
          .findFirst()
          .map(CompatibilityBridge::source);
    }

    List<CompatibilityBridge> rendererMatches = bridges.stream()
        .filter(bridge -> bridge.renderer().supports(stack))
        .toList();
    if (rendererMatches.size() == 1) {
      return Optional.of(rendererMatches.getFirst().source());
    }
    if (rendererMatches.size() > 1 && mappingId != null) {
      List<CompatibilityBridge> tagMatches = rendererMatches.stream()
          .filter(bridge -> bridge.tags().originatesFrom(mappingId, stack, client))
          .toList();
      return Optional.of((tagMatches.size() == 1
          ? tagMatches.getFirst() : rendererMatches.getFirst()).source());
    }
    return Optional.empty();
  }

  public static void refreshMetadata(String reason, boolean authoritative) {
    for (CompatibilityBridge bridge : active()) {
      bridge.metadata().refresh(reason, authoritative);
    }
    Map<EntityType<?>, Map<String, ISlotType>> bindings = new LinkedHashMap<>();
    for (CompatibilityBridge bridge : active()) {
      bridge.entityBindings().slotsByEntity(false).forEach((type, slots) -> {
        Map<String, ISlotType> resolved = new LinkedHashMap<>();
        for (String slot : slots) {
          CuriosApi.getSlot(slot, false)
              .ifPresent(slotType -> resolved.put(slot, slotType));
        }
        if (!resolved.isEmpty()) {
          bindings.put(type, resolved);
        }
      });
    }
    CuriosEntityManager.SERVER.mergeEntitySlots(bindings);
  }

  private static Object[] prepend(ItemStack stack, Object slot, Object[] arguments) {
    Object[] result = new Object[arguments.length + 2];
    result[0] = stack;
    result[1] = slot;
    System.arraycopy(arguments, 0, result, 2, arguments.length);
    return result;
  }
}
