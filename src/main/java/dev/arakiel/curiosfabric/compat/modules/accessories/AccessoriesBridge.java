package dev.arakiel.curiosfabric.compat.modules.accessories;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.type.SlotSource;
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
import dev.arakiel.curiosfabric.compat.modules.accessories.discovery.AccessoriesNativeMappings;
import dev.arakiel.curiosfabric.compat.modules.accessories.item.AccessoriesItemAdapter;
import dev.arakiel.curiosfabric.compat.modules.accessories.tags.AccessoriesTagRelations;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Autonomous Accessories compatibility module. */
public final class AccessoriesBridge implements CompatibilityBridge {

  public static final String ID = "accessories";

  private final BridgeDescriptor descriptor = new BridgeDescriptor(
      ID, "Accessories", SlotSource.ACCESSORIES,
      40, Optional.of(AccessoriesCompat.MOD_ID));
  private final BridgeServices services = new BridgeServices(
      new Discovery(),
      new Naming(),
      new AccessoriesTagRelations(),
      new Entities(),
      new ItemAssignments(),
      new ItemBehavior(),
      new Api(),
      new Renderer(),
      new Gameplay(),
      new ResourceDependencies());

  @Override
  public BridgeDescriptor descriptor() {
    return descriptor;
  }

  @Override
  public BridgeAvailability availability(BridgeContext context) {
    return CompatTargets.isAccessoriesLoaded()
        ? BridgeAvailability.ready(context.generation())
        : BridgeAvailability.unavailable(
            BridgeState.UNLOADED, "Accessories is not loaded", context.generation());
  }

  @Override
  public BridgeContribution refresh(BridgeContext context) {
    try {
      services.gameplay().initialize(context);
      AccessoriesCompat.refresh(
          context.client(), context.reason().name().toLowerCase(), context.authoritative());
      if (context.client()) {
        AccessoriesRendererBackend.reloadRendererCache();
      }
      List<SlotCandidate> slots = services.slots().slots(context);
      List<EntitySlotBinding> entities = services.entities().bindings(context);
      return new BridgeContribution(
          slots.isEmpty() ? ContributionStatus.EMPTY : ContributionStatus.AVAILABLE,
          slots, entities, List.of());
    } catch (Throwable throwable) {
      return BridgeContribution.failed(new BridgeDiagnostic(
          ID, context.side(), context.phase(), context.generation(),
          ContributionStatus.FAILED,
          "Accessories refresh failed: " + throwable.getClass().getSimpleName()));
    }
  }

  @Override
  public BridgeServices services() {
    return services;
  }

  @Override
  public void invalidate(BridgeInvalidationReason reason) {
    if (CompatTargets.isAccessoriesLoaded()) {
      AccessoriesCompat.invalidateAll();
      AccessoriesRendererBackend.invalidate();
    }
  }

  private static final class Discovery implements SlotDiscoveryContributor {
    @Override
    public List<SlotCandidate> slots(BridgeContext context) {
      return AccessoriesCompat.candidates(context.client());
    }

    @Override
    public List<SlotDataDirectory> dataDirectories() {
      // Accessories slot fields differ from CuriosFabric's loader. Consume its final loader output
      // instead of registering a second generic JSON parser.
      return List.of();
    }

    @Override
    public Optional<String> nativeMappedId(String upstreamId) {
      return AccessoriesNativeMappings.nativeId(upstreamId) == null
          ? Optional.empty()
          : Optional.of(AccessoriesNativeMappings.nativeId(upstreamId));
    }
  }

  private static final class Naming implements SlotNamingContributor {
    @Override
    public String translationKey(String slotId, String mappingId, SlotSource source) {
      return AccessoriesCompat.translationKey(
          slotId, mappingId, CuriosFabricConstants.IS_CLIENT);
    }
  }

  private static final class Entities implements EntityBindingContributor {
    @Override
    public List<EntitySlotBinding> bindings(BridgeContext context) {
      return AccessoriesCompat.entityBindings(context.client());
    }
  }

  private static final class ItemAssignments implements ItemAssignmentContributor {
    @Override
    public Set<String> slotsFor(ItemStack stack, boolean client) {
      return AccessoriesCompat.slotsFor(stack, null, client);
    }

    @Override
    public Set<String> slotsFor(
        ItemStack stack, LivingEntity entity, boolean client) {
      return AccessoriesCompat.slotsFor(stack, entity, client);
    }

    @Override
    public boolean matches(String slotId, ItemStack stack, boolean client) {
      return AccessoriesCompat.matches(
          new dev.arakiel.curiosfabric.api.SlotContext(
              slotId, null, 0, false, true), stack, client);
    }

    @Override
    public boolean matches(
        String slotId, ItemStack stack, LivingEntity entity, boolean client) {
      return AccessoriesCompat.matches(
          new dev.arakiel.curiosfabric.api.SlotContext(
              slotId, entity, 0, false, true), stack, client);
    }
  }

  private static final class ItemBehavior implements ItemBehaviorContributor {
    @Override
    public Optional<ItemBehaviorResolution> resolve(ItemStack stack) {
      return AccessoriesItemAdapter.resolve(stack);
    }
  }

  private static final class Renderer implements RendererContributor {
    @Override
    public boolean supports(RendererQuery query) {
      return query.slotSource() == SlotSource.ACCESSORIES
          || AccessoriesCompat.isAccessoriesItem(query.stack(), true);
    }

    @Override
    public boolean render(RendererQuery query) {
      return AccessoriesRendererBackend.render(query);
    }
  }

  static Object[] rendererArguments(RendererQuery query, Object reference) {
    Object[] shared = query.arguments();
    return new Object[]{
        query.stack(), reference, shared[2], shared[0], shared[3], shared[4],
        shared[6], shared[7], shared[8], shared[9], shared[10], shared[11]};
  }

  private static final class Gameplay implements GameplayHookContributor {
    @Override
    public void initialize(BridgeContext context) {
      AccessoriesCompat.initialize();
    }

    @Override
    public void invalidate(BridgeInvalidationReason reason) {
    }
  }

  private static final class Api implements ForeignApiFacade {
    @Override
    public void initialize(BridgeContext context) {
      AccessoriesCompat.initialize();
    }

    @Override
    public void invalidate(BridgeInvalidationReason reason) {
    }
  }

  private static final class ResourceDependencies implements ResourceDependencyContributor {
    @Override
    public List<ResourceLocation> dependencies() {
      return List.of(
          ResourceLocation.fromNamespaceAndPath(AccessoriesCompat.MOD_ID, "slot_loader"),
          ResourceLocation.fromNamespaceAndPath(AccessoriesCompat.MOD_ID, "entity_slot_loader"),
          ResourceLocation.fromNamespaceAndPath(AccessoriesCompat.MOD_ID, "slot_group_loader"));
    }
  }
}
