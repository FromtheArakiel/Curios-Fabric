package dev.arakiel.curiosfabric.compat.modules.neoforgecurios;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.common.data.CuriosSlotManager;
import dev.arakiel.curiosfabric.compat.contribution.api.ForeignApiFacade;
import dev.arakiel.curiosfabric.compat.contribution.entity.EntityBindingContributor;
import dev.arakiel.curiosfabric.compat.contribution.entity.EntitySlotBinding;
import dev.arakiel.curiosfabric.compat.contribution.event.GameplayHookContributor;
import dev.arakiel.curiosfabric.compat.contribution.item.ItemAssignmentContributor;
import dev.arakiel.curiosfabric.compat.contribution.item.ItemBehaviorContributor;
import dev.arakiel.curiosfabric.compat.contribution.item.ItemBehaviorResolution;
import dev.arakiel.curiosfabric.compat.contribution.lifecycle.ResourceDependencyContributor;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererContributor;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererQuery;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotCandidate;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotCandidateKey;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotDataDirectory;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotDiscoveryContributor;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotNamingContributor;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationContributor;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeAvailability;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeContribution;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeDescriptor;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeDiagnostic;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeInvalidationReason;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeServices;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeState;
import dev.arakiel.curiosfabric.compat.core.contract.CompatibilityBridge;
import dev.arakiel.curiosfabric.compat.core.contract.ContributionStatus;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.api.NeoforgeCuriosApiFacade;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.discovery.NeoforgeCuriosImcSlots;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.discovery.NeoforgeCuriosNativeMappings;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.item.NeoforgeCuriosItemAdapter;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.tags.NeoforgeCuriosTagRelations;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/** Autonomous NeoForge Curios compatibility module. */
public final class NeoforgeCuriosBridge implements CompatibilityBridge {

  public static final String ID = "neoforgecurios";

  private final BridgeDescriptor descriptor = new BridgeDescriptor(
      ID, "NeoForge Curios", SlotSource.NEOFORGE_CURIOS,
      20, Optional.of(CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE));
  private final NeoforgeCuriosApiFacade apiFacade = new NeoforgeCuriosApiFacade();
  private final BridgeServices services = new BridgeServices(
      new Discovery(),
      new Naming(),
      new NeoforgeCuriosTagRelations(),
      new Entities(),
      new ItemAssignments(),
      new ItemBehavior(),
      apiFacade,
      new Renderer(),
      new Gameplay(),
      new NoResourceDependencies());

  @Override
  public BridgeDescriptor descriptor() {
    return descriptor;
  }

  @Override
  public BridgeAvailability availability(BridgeContext context) {
    return CompatTargets.isNeoforgeCuriosLoaded()
        ? BridgeAvailability.ready(context.generation())
        : BridgeAvailability.unavailable(
            BridgeState.UNLOADED, "NeoForge Curios is not loaded", context.generation());
  }

  @Override
  public BridgeContribution refresh(BridgeContext context) {
    try {
      apiFacade.initialize(context);
      services.gameplay().initialize(context);
      Set<String> allSlotIds = new LinkedHashSet<>(CuriosSlotManager.SERVER.getSlots().keySet());
      allSlotIds.addAll(CuriosSlotManager.CLIENT.getSlots().keySet());
      NeoforgeCuriosCompat.register(allSlotIds);
      List<SlotCandidate> slots = services.slots().slots(context);
      List<EntitySlotBinding> entities = services.entities().bindings(context);
      return new BridgeContribution(
          slots.isEmpty() ? ContributionStatus.EMPTY : ContributionStatus.AVAILABLE,
          slots, entities, List.of());
    } catch (Throwable throwable) {
      return BridgeContribution.failed(new BridgeDiagnostic(
          ID, context.side(), context.phase(), context.generation(),
          ContributionStatus.FAILED,
          "NeoForge Curios refresh failed: " + throwable.getClass().getSimpleName()));
    }
  }

  @Override
  public BridgeServices services() {
    return services;
  }

  @Override
  public void invalidate(BridgeInvalidationReason reason) {
    apiFacade.invalidate(reason);
    services.gameplay().invalidate(reason);
  }

  private static final class Discovery implements SlotDiscoveryContributor {
    @Override
    public List<SlotCandidate> slots(BridgeContext context) {
      CuriosSlotManager manager = context.client()
          ? CuriosSlotManager.CLIENT : CuriosSlotManager.SERVER;
      List<SlotCandidate> result = new ArrayList<>();
      for (ISlotType slot : manager.getSlots().values()) {
        if (slot.getSource() != SlotSource.NEOFORGE_CURIOS) {
          continue;
        }
        result.add(new SlotCandidate(
            new SlotCandidateKey(SlotSource.NEOFORGE_CURIOS, slot.getIdentifier()),
            slot.getIdentifier(), slot.getMappingIdentifier(), SlotSource.NEOFORGE_CURIOS,
            NeoforgeCuriosNativeMappings.nativeId(slot.getMappingIdentifier()) != null,
            slot));
      }
      for (ISlotType slot : NeoforgeCuriosImcSlots.read()) {
        result.add(new SlotCandidate(
            new SlotCandidateKey(SlotSource.NEOFORGE_CURIOS, slot.getIdentifier()),
            slot.getIdentifier(), slot.getMappingIdentifier(), SlotSource.NEOFORGE_CURIOS,
            NeoforgeCuriosNativeMappings.nativeId(slot.getMappingIdentifier()) != null,
            slot));
      }
      return List.copyOf(result);
    }

    @Override
    public List<SlotDataDirectory> dataDirectories() {
      return List.of(
          new SlotDataDirectory(
              CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE + "/slots",
              SlotSource.NEOFORGE_CURIOS, true),
          new SlotDataDirectory(
              CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE + "/entities",
              SlotSource.NEOFORGE_CURIOS, true));
    }

    @Override
    public Optional<String> nativeMappedId(String upstreamId) {
      return Optional.ofNullable(NeoforgeCuriosNativeMappings.nativeId(upstreamId));
    }
  }

  private static final class Naming implements SlotNamingContributor {
    @Override
    public String translationKey(String slotId, String mappingId, SlotSource source) {
      return source.translationKey(mappingId)
          .orElse("curiosfabric.identifier." + mappingId);
    }
  }

  private static final class Entities implements EntityBindingContributor {
    @Override
    public List<EntitySlotBinding> bindings(BridgeContext context) {
      Map<EntityType<?>, Set<String>> merged = new LinkedHashMap<>();
      for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
        Set<String> slots = new LinkedHashSet<>();
        CuriosApi.getEntitySlots(type, context.client()).forEach((id, slot) -> {
          if (slot.getSource() == SlotSource.NEOFORGE_CURIOS) {
            slots.add(id);
          }
        });
        if (!slots.isEmpty()) {
          merged.put(type, slots);
        }
      }
      List<EntitySlotBinding> result = new ArrayList<>();
      merged.forEach((type, slots) -> result.add(new EntitySlotBinding(type, slots)));
      return List.copyOf(result);
    }
  }

  private static final class ItemAssignments implements ItemAssignmentContributor {
    @Override
    public Set<String> slotsFor(ItemStack stack, boolean client) {
      return NeoforgeCuriosCompat.slotsFor(stack, client);
    }

    @Override
    public boolean matches(String slotId, ItemStack stack, boolean client) {
      return NeoforgeCuriosCompat.isNeoforgeCuriosItemFor(slotId, stack);
    }
  }

  private static final class ItemBehavior implements ItemBehaviorContributor {
    @Override
    public Optional<ItemBehaviorResolution> resolve(ItemStack stack) {
      if (NeoforgeCuriosItemAdapter.isNeoforgeCuriosItemItem(stack.getItem())) {
        return Optional.of(new ItemBehaviorResolution(
            new NeoforgeCuriosItemAdapter(stack.getItem()), true));
      }
      return NeoforgeCuriosCompat.lookup(stack)
          .map(behavior -> new ItemBehaviorResolution(behavior, false));
    }
  }

  private static final class Renderer implements RendererContributor {
    @Override
    public boolean supports(RendererQuery query) {
      return NeoforgeCuriosApiFacade.hasRenderer(query.stack());
    }

    @Override
    public boolean render(RendererQuery query) {
      return NeoforgeCuriosApiFacade.renderForeign(rendererArguments(query));
    }
  }

  static Object[] rendererArguments(RendererQuery query) {
    Object[] shared = query.arguments();
    return new Object[]{
        query.stack(), query.slotContext(), shared[2], shared[1], shared[3], shared[4],
        shared[6], shared[7], shared[8], shared[9], shared[10], shared[11]};
  }

  private static final class Gameplay implements GameplayHookContributor {
    @Override
    public void initialize(BridgeContext context) {
    }

    @Override
    public void invalidate(BridgeInvalidationReason reason) {
    }
  }

  private static final class NoResourceDependencies implements ResourceDependencyContributor {
    @Override
    public java.util.Collection<ResourceLocation> dependencies() {
      return List.of();
    }
  }
}
