package dev.arakiel.curiosfabric.compat;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererDecision;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererQuery;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotDataDirectory;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeInvalidationReason;
import dev.arakiel.curiosfabric.compat.core.contract.BridgePhase;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeRefreshReason;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeSide;
import dev.arakiel.curiosfabric.compat.engine.CompatibilityCoordinator;
import dev.arakiel.curiosfabric.compat.engine.registry.CompatibilityBridgeRegistry;
import dev.arakiel.curiosfabric.compat.engine.registry.CompatibilityModuleCatalog;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.ItemStack;

/** Stable facade used by the CuriosFabric runtime and compatibility mixins. */
public final class CompatibilityRuntime {

  private static final CompatibilityCoordinator COORDINATOR =
      new CompatibilityCoordinator(
          new CompatibilityBridgeRegistry(CompatibilityModuleCatalog.builtInBridges()));

  private CompatibilityRuntime() {
  }

  public static void refresh(String reason, boolean authoritative) {
    refresh(BridgeSide.SERVER, parseReason(reason), "server", authoritative);
  }

  public static void refreshClient(String reason, boolean authoritative) {
    refresh(BridgeSide.CLIENT, parseReason(reason), reason, authoritative);
  }

  public static void refreshSource(
      SlotSource source, BridgeSide side, BridgeRefreshReason reason, boolean authoritative) {
    BridgeContext context = COORDINATOR.newContext(side, BridgePhase.DATA, reason, authoritative);
    COORDINATOR.refreshSource(source, context);
  }

  public static void mirrorTags(
      Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags, boolean client) {
    BridgeContext context = COORDINATOR.currentContext(
        client ? BridgeSide.CLIENT : BridgeSide.SERVER,
        BridgePhase.TAGS, BridgeRefreshReason.INIT, false);
    COORDINATOR.mirrorTags(tags, context);
  }

  public static List<SlotDataDirectory> slotDataDirectories() {
    return COORDINATOR.slotDataDirectories();
  }

  public static boolean isSlotNamespace(String namespace) {
    return COORDINATOR.isSlotNamespace(namespace);
  }

  public static Optional<String> nativeMappedId(SlotSource source, String upstreamId) {
    return COORDINATOR.nativeMappedId(source, upstreamId);
  }

  public static String translationKey(String slotId, String mappingId, SlotSource source) {
    return COORDINATOR.translationKey(slotId, mappingId, source);
  }

  public static Set<String> slotsForItem(ItemStack stack, boolean client) {
    return COORDINATOR.slotsForItem(stack, client);
  }

  public static boolean matchesItemSlot(String slotId, ItemStack stack, boolean client) {
    return COORDINATOR.matchesItemSlot(slotId, stack, client);
  }

  public static Optional<ICurioItem> resolveItemBehavior(ItemStack stack) {
    return COORDINATOR.resolveItemBehavior(stack);
  }

  public static RendererDecision rendererDecision(ISlotType slotType, ItemStack stack,
                                                  SlotContext context, Object[] arguments) {
    return COORDINATOR.rendererDecision(new RendererQuery(
        slotType == null ? SlotSource.CURIOSFABRIC : slotType.getSource(),
        stack, context, arguments, true));
  }

  public static boolean render(ISlotType slotType, ItemStack stack,
                               SlotContext context, Object[] arguments) {
    return COORDINATOR.render(new RendererQuery(
        slotType == null ? SlotSource.CURIOSFABRIC : slotType.getSource(),
        stack, context, arguments, true));
  }

  public static Collection<ResourceLocation> resourceDependencies() {
    return COORDINATOR.resourceDependencies();
  }

  public static void invalidate(BridgeInvalidationReason reason) {
    COORDINATOR.invalidate(reason);
  }

  public static void invalidateClient() {
    COORDINATOR.invalidateSide(
        BridgeSide.CLIENT, BridgeInvalidationReason.CLIENT_DISCONNECT);
  }

  public static void logHealth() {
    COORDINATOR.healthSnapshot();
  }

  private static void refresh(
      BridgeSide side, BridgeRefreshReason reason, String detail, boolean authoritative) {
    BridgeContext context = COORDINATOR.newContext(
        side, BridgePhase.DATA, reason, authoritative);
    CuriosFabricConstants.LOG.info(
        "Refreshing compatibility bridges side={} reason={} authoritative={}",
        side, detail, authoritative);
    COORDINATOR.refresh(context);
  }

  private static BridgeRefreshReason parseReason(String reason) {
    return switch (reason == null ? "" : reason) {
      case "data_pack_reload", "end_data_pack_reload" -> BridgeRefreshReason.DATA_PACK_RELOAD;
      case "client_sync" -> BridgeRefreshReason.CLIENT_SYNC;
      case "server_starting" -> BridgeRefreshReason.SERVER_STARTING;
      case "server_started" -> BridgeRefreshReason.SERVER_STARTED;
      case "login" -> BridgeRefreshReason.LOGIN;
      case "respawn" -> BridgeRefreshReason.RESPAWN;
      default -> BridgeRefreshReason.INIT;
    };
  }
}
