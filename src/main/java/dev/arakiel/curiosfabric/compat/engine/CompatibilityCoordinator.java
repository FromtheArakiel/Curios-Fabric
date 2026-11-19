package dev.arakiel.curiosfabric.compat.engine;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.common.data.CuriosEntityManager;
import dev.arakiel.curiosfabric.common.data.CuriosSlotManager;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererDecision;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererQuery;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotCandidate;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotDataDirectory;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelation;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationDirection;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationSet;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeAvailability;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeContribution;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeDiagnostic;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeHealth;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeInvalidationReason;
import dev.arakiel.curiosfabric.compat.core.contract.BridgePhase;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeRefreshReason;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeSide;
import dev.arakiel.curiosfabric.compat.core.contract.CompatibilityBridge;
import dev.arakiel.curiosfabric.compat.core.contract.ContributionStatus;
import dev.arakiel.curiosfabric.compat.engine.algorithm.EntityBindingMerger;
import dev.arakiel.curiosfabric.compat.engine.algorithm.ItemBehaviorArbiter;
import dev.arakiel.curiosfabric.compat.engine.algorithm.RendererArbiter;
import dev.arakiel.curiosfabric.compat.engine.algorithm.TagRelationMerger;
import dev.arakiel.curiosfabric.compat.engine.registry.CompatibilityBridgeRegistry;
import dev.arakiel.curiosfabric.compat.engine.state.BridgeHealthRegistry;
import dev.arakiel.curiosfabric.compat.engine.state.BridgeStateStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Coordinates all bridge contributions without owning bridge-specific implementation. */
public final class CompatibilityCoordinator {

  private final CompatibilityBridgeRegistry registry;
  private final BridgeStateStore stateStore = new BridgeStateStore();
  private final BridgeHealthRegistry health = new BridgeHealthRegistry();

  public CompatibilityCoordinator(CompatibilityBridgeRegistry registry) {
    this.registry = registry;
  }

  public BridgeContext newContext(
      BridgeSide side, BridgePhase phase, BridgeRefreshReason reason, boolean authoritative) {
    return new BridgeContext(
        side, phase, reason, authoritative, stateStore.nextGeneration(side));
  }

  public BridgeContext currentContext(
      BridgeSide side, BridgePhase phase, BridgeRefreshReason reason, boolean authoritative) {
    return new BridgeContext(
        side, phase, reason, authoritative, stateStore.currentGeneration(side));
  }

  public List<CompatibilityBridge> active(BridgeContext context) {
    return registry.active(context);
  }

  public Optional<BridgeContribution> contribution(String bridgeId, BridgeSide side) {
    return stateStore.contribution(bridgeId, side);
  }

  public void refresh(BridgeContext context) {
    for (CompatibilityBridge bridge : registry.all()) {
      BridgeAvailability availability = safeAvailability(bridge, context);
      health.put(bridge.descriptor().id(), context.side(),
          new BridgeHealth(availability, availability.detail()));
      if (!availability.active()) {
        stateStore.put(bridge.descriptor().id(), context.side(),
            BridgeContribution.notApplicable());
        continue;
      }
      BridgeContribution contribution;
      try {
        contribution = bridge.refresh(context);
      } catch (Throwable throwable) {
        contribution = BridgeContribution.failed(new BridgeDiagnostic(
            bridge.descriptor().id(), context.side(), context.phase(), context.generation(),
            ContributionStatus.FAILED,
            "Unhandled bridge refresh failure: " + throwable.getClass().getSimpleName()));
      }
      stateStore.put(bridge.descriptor().id(), context.side(), contribution);
      recordContributionHealth(bridge, context, contribution);
      logDiagnostics(contribution);
    }
    publishSlotsAndEntities(context);
  }

  public void refreshSource(SlotSource source, BridgeContext context) {
    for (CompatibilityBridge bridge : registry.all()) {
      if (bridge.descriptor().source() != source) {
        continue;
      }
      BridgeAvailability availability = safeAvailability(bridge, context);
      health.put(bridge.descriptor().id(), context.side(),
          new BridgeHealth(availability, availability.detail()));
      if (!availability.active()) {
        stateStore.put(bridge.descriptor().id(), context.side(),
            BridgeContribution.notApplicable());
        continue;
      }
      BridgeContribution contribution;
      try {
        contribution = bridge.refresh(context);
      } catch (Throwable throwable) {
        contribution = BridgeContribution.failed(new BridgeDiagnostic(
            bridge.descriptor().id(), context.side(), context.phase(), context.generation(),
            ContributionStatus.FAILED,
            "Unhandled bridge refresh failure: " + throwable.getClass().getSimpleName()));
      }
      stateStore.put(bridge.descriptor().id(), context.side(), contribution);
      recordContributionHealth(bridge, context, contribution);
      logDiagnostics(contribution);
    }
    publishSlotsAndEntities(context);
  }

  public List<SlotDataDirectory> slotDataDirectories() {
    List<SlotDataDirectory> result = new ArrayList<>();
    for (CompatibilityBridge bridge : registry.all()) {
      try {
        result.addAll(bridge.services().slots().dataDirectories());
      } catch (Throwable throwable) {
        CuriosFabricConstants.LOG.warn(
            "Compatibility bridge {} failed to expose data directories",
            bridge.descriptor().id(), throwable);
      }
    }
    return List.copyOf(result);
  }

  public boolean isSlotNamespace(String namespace) {
    if (CuriosFabricConstants.MOD_ID.equals(namespace)) {
      return true;
    }
    return registry.all().stream().anyMatch(bridge ->
        bridge.descriptor().apiNamespace().filter(namespace::equals).isPresent());
  }

  public Optional<String> nativeMappedId(SlotSource source, String upstreamId) {
    CompatibilityBridge bridge = registry.all().stream()
        .filter(value -> value.descriptor().source() == source)
        .findFirst()
        .orElse(null);
    if (bridge == null) {
      return Optional.empty();
    }
    try {
      return bridge.services().slots().nativeMappedId(upstreamId);
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.warn(
          "Compatibility bridge {} failed native slot mapping",
          bridge.descriptor().id(), throwable);
      return Optional.empty();
    }
  }

  public String translationKey(String slotId, String mappingId, SlotSource source) {
    if (source == SlotSource.CURIOSFABRIC) {
      return source.translationKey(mappingId)
          .orElse(CuriosFabricConstants.MOD_ID + ".identifier." + mappingId);
    }
    return registry.all().stream()
        .filter(bridge -> bridge.descriptor().source() == source)
        .findFirst()
        .map(bridge -> {
          try {
            return bridge.services().naming()
                .translationKey(slotId, mappingId, source);
          } catch (Throwable throwable) {
            CuriosFabricConstants.LOG.warn(
                "Compatibility bridge {} failed slot naming",
                bridge.descriptor().id(), throwable);
            return source.translationKey(mappingId)
                .orElse(CuriosFabricConstants.MOD_ID + ".identifier." + mappingId);
          }
        })
        .orElseGet(() -> source.translationKey(mappingId)
            .orElse(CuriosFabricConstants.MOD_ID + ".identifier." + mappingId));
  }

  public String rendererFallbackSlot(RendererQuery query) {
    BridgeContext bridgeContext = currentContext(
        query.client() ? BridgeSide.CLIENT : BridgeSide.SERVER,
        BridgePhase.RUNTIME, BridgeRefreshReason.INIT, false);
    for (CompatibilityBridge bridge : registry.active(bridgeContext)) {
      try {
        String hint = bridge.services().renderer()
            .fallbackSlot(query).orElse(null);
        if (hint != null) {
          return hint;
        }
      } catch (Throwable throwable) {
        CuriosFabricConstants.LOG.warn(
            "Compatibility bridge {} failed renderer fallback-slot discovery",
            bridge.descriptor().id(), throwable);
      }
    }
    return null;
  }

  public Set<String> slotsForItem(ItemStack stack, boolean client) {
    return slotsForItem(stack, null, client);
  }

  public Set<String> slotsForItem(
      ItemStack stack, LivingEntity entity, boolean client) {
    Set<String> result = new LinkedHashSet<>();
    BridgeContext context = currentContext(
        client ? BridgeSide.CLIENT : BridgeSide.SERVER,
        BridgePhase.RUNTIME, BridgeRefreshReason.INIT, false);
    for (CompatibilityBridge bridge : registry.active(context)) {
      try {
        result.addAll(bridge.services().itemAssignments().slotsFor(stack, entity, client));
      } catch (Throwable throwable) {
        CuriosFabricConstants.LOG.warn(
            "Compatibility bridge {} failed item slot assignment",
            bridge.descriptor().id(), throwable);
      }
    }
    return Set.copyOf(result);
  }

  public boolean matchesItemSlot(String slotId, ItemStack stack, boolean client) {
    return matchesItemSlot(slotId, stack, null, client);
  }

  public boolean matchesItemSlot(
      String slotId, ItemStack stack, LivingEntity entity, boolean client) {
    BridgeContext context = currentContext(
        client ? BridgeSide.CLIENT : BridgeSide.SERVER,
        BridgePhase.RUNTIME, BridgeRefreshReason.INIT, false);
    for (CompatibilityBridge bridge : registry.active(context)) {
      try {
        if (bridge.services().itemAssignments().matches(slotId, stack, entity, client)) {
          return true;
        }
      } catch (Throwable throwable) {
        CuriosFabricConstants.LOG.warn(
            "Compatibility bridge {} failed item-slot matching",
            bridge.descriptor().id(), throwable);
      }
    }
    return false;
  }

  public Optional<ICurioItem> resolveItemBehavior(ItemStack stack) {
    BridgeContext context = currentContext(
        BridgeSide.SERVER, BridgePhase.RUNTIME, BridgeRefreshReason.INIT, false);
    return ItemBehaviorArbiter.resolve(stack, registry.active(context));
  }

  public RendererDecision rendererDecision(RendererQuery query) {
    BridgeContext context = currentContext(
        query.client() ? BridgeSide.CLIENT : BridgeSide.SERVER,
        BridgePhase.RUNTIME, BridgeRefreshReason.INIT, false);
    return RendererArbiter.decide(query, registry.active(context));
  }

  public boolean render(RendererQuery query) {
    BridgeContext context = currentContext(
        query.client() ? BridgeSide.CLIENT : BridgeSide.SERVER,
        BridgePhase.RUNTIME, BridgeRefreshReason.INIT, false);
    List<CompatibilityBridge> active = registry.active(context);
    RendererDecision decision = RendererArbiter.decide(query, active);
    if (decision.state() == RendererDecision.State.AMBIGUOUS
        || decision.state() == RendererDecision.State.FAILED) {
      CuriosFabricConstants.LOG.warn("Compatibility renderer {}: {}",
          decision.state(), decision.detail());
    }
    return RendererArbiter.render(decision, query, active);
  }

  public void mirrorTags(
      Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags,
      BridgeContext context) {
    List<TagRelationSet> relations = new ArrayList<>();
    List<SlotCandidate> slots = contributions(context.side()).stream()
        .flatMap(contribution -> contribution.slots().stream())
        .toList();
    for (CompatibilityBridge bridge : registry.active(context)) {
      try {
        relations.add(bridge.services().tags().relations(context, tags.keySet()));
      } catch (Throwable throwable) {
        CuriosFabricConstants.LOG.warn(
            "Compatibility bridge {} failed tag relation generation",
            bridge.descriptor().id(), throwable);
      }
    }
    relations.add(directNamespaceRelations(context, tags.keySet()));
    relations.add(crossBridgeSemanticRelations(context, relations));
    relations.add(semanticRelations(context.side(), slots));
    TagRelationMerger.merge(tags, relations);
  }

  public Collection<ResourceLocation> resourceDependencies() {
    BridgeContext context = currentContext(
        BridgeSide.SERVER, BridgePhase.DISCOVER, BridgeRefreshReason.INIT, false);
    List<ResourceLocation> result = new ArrayList<>();
    for (CompatibilityBridge bridge : registry.active(context)) {
      try {
        result.addAll(bridge.services().resourceDependencies().dependencies());
      } catch (Throwable throwable) {
        CuriosFabricConstants.LOG.warn(
            "Compatibility bridge {} failed resource dependency discovery",
            bridge.descriptor().id(), throwable);
      }
    }
    return List.copyOf(result);
  }

  public void invalidate(BridgeInvalidationReason reason) {
    for (CompatibilityBridge bridge : registry.all()) {
      try {
        bridge.invalidate(reason);
      } catch (Throwable throwable) {
        CuriosFabricConstants.LOG.warn("Failed to invalidate compatibility bridge {}",
            bridge.descriptor().id(), throwable);
      }
    }
    stateStore.invalidate(BridgeSide.CLIENT);
    stateStore.invalidate(BridgeSide.SERVER);
  }

  public void invalidateSide(BridgeSide side, BridgeInvalidationReason reason) {
    for (CompatibilityBridge bridge : registry.all()) {
      try {
        bridge.invalidate(reason);
      } catch (Throwable throwable) {
        CuriosFabricConstants.LOG.warn(
            "Failed to invalidate compatibility bridge {} for {}",
            bridge.descriptor().id(), side, throwable);
      }
    }
    stateStore.invalidate(side);
  }

  public void healthSnapshot() {
    for (CompatibilityBridge bridge : registry.all()) {
      for (BridgeSide side : BridgeSide.values()) {
        health.get(bridge.descriptor().id(), side)
            .ifPresent(value -> CuriosFabricConstants.LOG.debug(
                "Compatibility bridge {} side={} state={} detail={}",
                bridge.descriptor().id(), side, value.availability().state(),
                value.detail()));
      }
    }
  }

  private void publishSlotsAndEntities(BridgeContext context) {
    BridgeSide side = context.side();
    Map<String, ISlotType> slots = new LinkedHashMap<>();
    List<dev.arakiel.curiosfabric.compat.contribution.entity.EntitySlotBinding> entities =
        new ArrayList<>();
    Map<String, BridgeContribution> byBridge =
        stateStore.snapshot(side).contributions();
    for (Map.Entry<String, BridgeContribution> entry : byBridge.entrySet()) {
      String bridgeId = entry.getKey();
      BridgeContribution contribution = entry.getValue();
      if (contribution.status() == ContributionStatus.FAILED
          || contribution.status() == ContributionStatus.NOT_READY) {
        continue;
      }
      Map<String, ISlotType> bridgeSlots = new LinkedHashMap<>();
      for (SlotCandidate candidate : contribution.slots()) {
        bridgeSlots.putIfAbsent(candidate.instanceId(), candidate.slotType());
      }
      managerFor(side).mergeSlots(bridgeSlots, bridgeId);
      bridgeSlots.forEach(slots::putIfAbsent);
      entities.addAll(contribution.entityBindings());
    }
    CuriosSlotManager manager = managerFor(side);
    if (context.authoritative()) {
      Set<String> bridgeOwners = registry.all().stream()
          .map(bridge -> bridge.descriptor().id())
          .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
      manager.retainDynamicSlots(slots.keySet(), bridgeOwners);
    }
    CuriosEntityManager entityManager = side == BridgeSide.CLIENT
        ? CuriosEntityManager.CLIENT : CuriosEntityManager.SERVER;
    entityManager.retainSlots(manager.getSlots().keySet());
    Map<EntityType<?>, Map<String, ISlotType>> bindings =
        EntityBindingMerger.merge(manager.getSlots(), entities);
    entityManager.mergeEntitySlots(bindings);
  }

  private CuriosSlotManager managerFor(BridgeSide side) {
    return side == BridgeSide.CLIENT ? CuriosSlotManager.CLIENT : CuriosSlotManager.SERVER;
  }

  private List<BridgeContribution> contributions(BridgeSide side) {
    return stateStore.snapshot(side).contributions().values().stream().toList();
  }

  private TagRelationSet semanticRelations(
      BridgeSide side, List<SlotCandidate> slots) {
    Map<String, Set<ResourceLocation>> tagsByMapping = new LinkedHashMap<>();
    for (SlotCandidate candidate : slots) {
      Set<ResourceLocation> tags = new LinkedHashSet<>();
      tags.add(ResourceLocation.fromNamespaceAndPath(
          CuriosFabricConstants.MOD_ID, candidate.mappingId()));
      CompatibilityBridge bridge = registry.all().stream()
          .filter(value -> value.descriptor().source() == candidate.key().source())
          .findFirst()
          .orElse(null);
      if (bridge != null) {
        try {
          tags.addAll(bridge.services().tags().semanticTags(candidate));
        } catch (Throwable throwable) {
          CuriosFabricConstants.LOG.warn(
              "Compatibility bridge {} failed semantic tag generation",
              bridge.descriptor().id(), throwable);
        }
      }
      tagsByMapping.computeIfAbsent(candidate.mappingId(), ignored -> new LinkedHashSet<>())
          .addAll(tags);
    }
    Set<TagRelation> relations = new LinkedHashSet<>();
    tagsByMapping.values().forEach(tags -> {
      List<ResourceLocation> values = List.copyOf(tags);
      ResourceLocation semantic = values.getFirst();
      for (int index = 1; index < values.size(); index++) {
        relations.add(new TagRelation(
            values.get(index), semantic, TagRelationDirection.ONE_WAY,
            "shared_semantic_slot"));
      }
    });
    return new TagRelationSet(relations);
  }

  private TagRelationSet crossBridgeSemanticRelations(
      BridgeContext context, List<TagRelationSet> existingRelations) {
    Map<String, ResourceLocation> semanticTags = new LinkedHashMap<>();
    for (TagRelationSet relationSet : existingRelations) {
      for (TagRelation relation : relationSet.relations()) {
        if (relation.sourceTag().getNamespace().equals(CuriosFabricConstants.MOD_ID)) {
          semanticTags.putIfAbsent(relation.sourceTag().getPath(), relation.sourceTag());
        }
        if (relation.semanticTag().getNamespace().equals(CuriosFabricConstants.MOD_ID)) {
          semanticTags.putIfAbsent(relation.semanticTag().getPath(), relation.semanticTag());
        }
      }
    }
    Set<TagRelation> result = new LinkedHashSet<>();
    for (CompatibilityBridge bridge : registry.active(context)) {
      if (!bridge.descriptor().source().usesDirectTagAliases()) {
        continue;
      }
      String namespace = bridge.descriptor().apiNamespace().orElse(null);
      if (namespace == null || namespace.equals(CuriosFabricConstants.MOD_ID)) {
        continue;
      }
      semanticTags.forEach((path, semanticTag) -> {
        ResourceLocation upstream =
            ResourceLocation.fromNamespaceAndPath(namespace, path);
        result.add(new TagRelation(
            upstream,
            semanticTag,
            TagRelationDirection.ONE_WAY,
            "cross_bridge_semantic_alias"));
      });
    }
    return new TagRelationSet(result);
  }

  private TagRelationSet directNamespaceRelations(
      BridgeContext context, Set<ResourceLocation> tagIds) {
    Set<TagRelation> result = new LinkedHashSet<>();
    for (CompatibilityBridge bridge : registry.active(context)) {
      if (!bridge.descriptor().source().usesDirectTagAliases()) {
        continue;
      }
      String namespace = bridge.descriptor().apiNamespace().orElse(null);
      if (namespace == null || namespace.equals(CuriosFabricConstants.MOD_ID)) {
        continue;
      }
      for (ResourceLocation tag : tagIds) {
        if (!tag.getNamespace().equals(CuriosFabricConstants.MOD_ID)) {
          continue;
        }
        result.add(new TagRelation(
            ResourceLocation.fromNamespaceAndPath(namespace, tag.getPath()),
            tag,
            TagRelationDirection.ONE_WAY,
            "direct_namespace_alias"));
      }
    }
    return new TagRelationSet(result);
  }

  private void logDiagnostics(BridgeContribution contribution) {
    for (BridgeDiagnostic diagnostic : contribution.diagnostics()) {
      if (diagnostic.status() == ContributionStatus.FAILED) {
        CuriosFabricConstants.LOG.warn("[compat:{}:{}] {}",
            diagnostic.bridgeId(), diagnostic.side(), diagnostic.message());
      } else {
        CuriosFabricConstants.LOG.debug("[compat:{}:{}] {}",
            diagnostic.bridgeId(), diagnostic.side(), diagnostic.message());
      }
    }
  }

  private void recordContributionHealth(
      CompatibilityBridge bridge, BridgeContext context, BridgeContribution contribution) {
    if (contribution.status() != ContributionStatus.FAILED
        && contribution.status() != ContributionStatus.PARTIAL) {
      return;
    }
    BridgeAvailability degraded = BridgeAvailability.unavailable(
        dev.arakiel.curiosfabric.compat.core.contract.BridgeState.DEGRADED,
        contribution.status().name().toLowerCase(),
        context.generation());
    health.put(bridge.descriptor().id(), context.side(),
        new BridgeHealth(degraded, degraded.detail()));
  }

  private BridgeAvailability safeAvailability(
      CompatibilityBridge bridge, BridgeContext context) {
    try {
      return bridge.availability(context);
    } catch (Throwable throwable) {
      return BridgeAvailability.unavailable(
          dev.arakiel.curiosfabric.compat.core.contract.BridgeState.FAILED,
          throwable.getClass().getSimpleName(), context.generation());
    }
  }
}
