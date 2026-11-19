package dev.arakiel.curiosfabric.compat.modules.trinkets;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
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
import dev.arakiel.curiosfabric.compat.modules.trinkets.api.TrinketsApiFacade;
import dev.arakiel.curiosfabric.compat.modules.trinkets.client.TrinketsRendererIntegration;
import dev.arakiel.curiosfabric.compat.modules.trinkets.discovery.TrinketsNativeMappings;
import dev.arakiel.curiosfabric.compat.modules.trinkets.item.TrinketItemAdapter;
import dev.arakiel.curiosfabric.compat.modules.trinkets.lifecycle.TrinketsResourceDependencies;
import dev.arakiel.curiosfabric.compat.modules.trinkets.tags.TrinketsTagRelations;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/** Autonomous Trinkets compatibility module. */
public final class TrinketsBridge implements CompatibilityBridge {

  public static final String ID = "trinkets";

  private final BridgeDescriptor descriptor = new BridgeDescriptor(
      ID, "Trinkets", SlotSource.TRINKETS,
      30, Optional.of("trinkets"));
  private final TrinketsApiFacade apiFacade = new TrinketsApiFacade();
  private final BridgeServices services = new BridgeServices(
      new Discovery(),
      new Naming(),
      new TrinketsTagRelations(),
      new Entities(),
      new ItemAssignments(),
      new ItemBehavior(),
      apiFacade,
      new Renderer(),
      new Gameplay(),
      new TrinketsResourceDependencies());

  @Override
  public BridgeDescriptor descriptor() {
    return descriptor;
  }

  @Override
  public BridgeAvailability availability(BridgeContext context) {
    return CompatTargets.isTrinketsLoaded()
        ? BridgeAvailability.ready(context.generation())
        : BridgeAvailability.unavailable(
            BridgeState.UNLOADED, "Trinkets is not loaded", context.generation());
  }

  @Override
  public BridgeContribution refresh(BridgeContext context) {
    try {
      apiFacade.initialize(context);
      services.gameplay().initialize(context);
      TrinketsCompat.register(context.client(), context.reason().name().toLowerCase(),
          context.authoritative());
      List<SlotCandidate> slots = services.slots().slots(context);
      List<EntitySlotBinding> entities = services.entities().bindings(context);
      return new BridgeContribution(
          slots.isEmpty() ? ContributionStatus.EMPTY : ContributionStatus.AVAILABLE,
          slots, entities, List.of());
    } catch (Throwable throwable) {
      return BridgeContribution.failed(new BridgeDiagnostic(
          ID, context.side(), context.phase(), context.generation(),
          ContributionStatus.FAILED,
          "Trinkets refresh failed: " + throwable.getClass().getSimpleName()));
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
      List<SlotCandidate> result = new ArrayList<>();
      for (TrinketsCompat.TrinketSlot slot : TrinketsCompat.slots(context.client())) {
        ISlotType slotType = TrinketsCompat.createSlotType(slot);
        result.add(new SlotCandidate(
            new SlotCandidateKey(SlotSource.TRINKETS, slot.path()),
            slot.curiosId(), slot.mappingId(), slotType.getSource(),
            slot.nativeSlot(), slotType));
      }
      return List.copyOf(result);
    }

    @Override
    public List<SlotDataDirectory> dataDirectories() {
      return List.of();
    }

    @Override
    public Optional<String> nativeMappedId(String upstreamId) {
      return Optional.ofNullable(TrinketsNativeMappings.nativeId(upstreamId));
    }
  }

  private static final class Naming implements SlotNamingContributor {
    @Override
    public String translationKey(String slotId, String mappingId, SlotSource source) {
      if (source == SlotSource.TRINKETS) {
        return TrinketsCompat.translationKeyFor(slotId)
            .orElse("trinkets.slot." + slotId);
      }
      return source.translationKey(mappingId)
          .orElse(CuriosFabricConstants.MOD_ID + ".identifier." + mappingId);
    }
  }

  private static final class Entities implements EntityBindingContributor {
    @Override
    public List<EntitySlotBinding> bindings(BridgeContext context) {
      List<EntitySlotBinding> result = new ArrayList<>();
      for (Map.Entry<EntityType<?>, Set<String>> entry :
          TrinketsCompat.entitySlotIds(context.client()).entrySet()) {
        result.add(new EntitySlotBinding(entry.getKey(), entry.getValue()));
      }
      return List.copyOf(result);
    }
  }

  private static final class ItemAssignments implements ItemAssignmentContributor {
    @Override
    public Set<String> slotsFor(ItemStack stack, boolean client) {
      return TrinketsCompat.slotsFor(stack, client);
    }

    @Override
    public boolean matches(String slotId, ItemStack stack, boolean client) {
      String path = TrinketsCompat.trinketsPathOf(slotId, client);
      return path != null && TrinketsCompat.isTrinketFor(stack, path);
    }
  }

  private static final class ItemBehavior implements ItemBehaviorContributor {
    @Override
    public Optional<ItemBehaviorResolution> resolve(ItemStack stack) {
      if (TrinketItemAdapter.isTrinket(stack.getItem())) {
        return Optional.of(new ItemBehaviorResolution(
            new TrinketItemAdapter(stack.getItem()), true));
      }
      return TrinketsCompat.lookup(stack)
          .map(behavior -> new ItemBehaviorResolution(behavior, false));
    }
  }

  private static final class Renderer implements RendererContributor {
    @Override
    public boolean supports(RendererQuery query) {
      return TrinketsRendererIntegration.hasRenderer(query.stack());
    }

    @Override
    public Optional<String> fallbackSlot(RendererQuery query) {
      if (!ownsItemForFallback(query)) {
        return Optional.empty();
      }
      return Optional.ofNullable(
          TrinketsCompat.rendererFallbackSlot(
              query.slotContext().identifier(), query.client()));
    }

    @Override
    public boolean ownsItemForFallback(RendererQuery query) {
      return TrinketsCompat.isOriginalTrinketItem(
          query.stack(), query.slotContext().entity(), query.client());
    }

    @Override
    public boolean render(RendererQuery query) {
      Object reference = TrinketsApiFacade.slotReference(
          query.slotContext().identifier(), query.slotContext().index(),
          query.slotContext().entity(), query.stack());
      return TrinketsRendererIntegration.render(rendererArguments(query, reference));
    }
  }

  static Object[] rendererArguments(RendererQuery query, Object reference) {
    Object[] shared = query.arguments();
    return new Object[]{
        query.stack(), reference, shared[0], shared[2], shared[3], shared[4], shared[5],
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
}
