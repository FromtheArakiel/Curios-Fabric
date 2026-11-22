package dev.arakiel.curiosfabric.compat.engine.algorithm;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.compat.contribution.item.ItemBehaviorResolution;
import dev.arakiel.curiosfabric.compat.core.contract.CompatibilityBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Chooses exactly one deterministic foreign item behavior adapter. */
public final class ItemBehaviorArbiter {

  private static final Map<Item, Optional<ICurioItem>> CACHE = new ConcurrentHashMap<>();

  private ItemBehaviorArbiter() {
  }

  public static Optional<ICurioItem> resolve(
      ItemStack stack, List<CompatibilityBridge> bridges) {
    if (stack == null) {
      return Optional.empty();
    }
    return CACHE.computeIfAbsent(
        stack.getItem(), ignored -> resolveUncached(stack, bridges));
  }

  public static void clearCache() {
    CACHE.clear();
  }

  private static Optional<ICurioItem> resolveUncached(
      ItemStack stack, List<CompatibilityBridge> bridges) {
    List<Resolution> matches = new ArrayList<>();
    for (CompatibilityBridge bridge : bridges) {
      try {
        bridge.services().itemBehavior().resolve(stack)
            .ifPresent(resolution -> matches.add(new Resolution(bridge, resolution)));
      } catch (Throwable throwable) {
        CuriosFabricConstants.LOG.warn(
            "Compatibility bridge {} failed item behavior resolution",
            bridge.descriptor().id(), throwable);
      }
    }
    if (matches.isEmpty()) {
      return Optional.empty();
    }
    if (matches.size() > 1) {
      CuriosFabricConstants.LOG.warn(
          "Multiple compatibility bridges claim item behavior for {}: {}; using {}",
          stack, matches.stream().map(match -> match.bridge().descriptor().id()).toList(),
          matches.getFirst().bridge().descriptor().id());
    }
    Resolution selected = matches.stream()
        .filter(match -> match.resolution().codeBacked())
        .findFirst()
        .orElse(matches.getFirst());
    return Optional.of(selected.resolution().behavior());
  }

  private record Resolution(CompatibilityBridge bridge, ItemBehaviorResolution resolution) {
  }
}
