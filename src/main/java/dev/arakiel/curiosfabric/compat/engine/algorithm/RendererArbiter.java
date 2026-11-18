package dev.arakiel.curiosfabric.compat.engine.algorithm;

import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererDecision;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererQuery;
import dev.arakiel.curiosfabric.compat.core.contract.CompatibilityBridge;
import java.util.List;

/** Resolves renderer ownership without trial-and-error across unrelated sources. */
public final class RendererArbiter {

  private RendererArbiter() {
  }

  public static RendererDecision decide(
      RendererQuery query, List<CompatibilityBridge> bridges) {
    List<CompatibilityBridge> candidates;
    if (query.slotSource() != SlotSource.CURIOSFABRIC) {
      candidates = bridges.stream()
          .filter(bridge -> bridge.descriptor().source() == query.slotSource())
          .toList();
    } else {
      candidates = bridges;
    }

    List<CompatibilityBridge> matches = new java.util.ArrayList<>();
    List<String> failures = new java.util.ArrayList<>();
    List<String> supportTrace = new java.util.ArrayList<>();
    for (CompatibilityBridge bridge : candidates) {
      try {
        boolean supported = bridge.services().renderer().supports(query);
        supportTrace.add(bridge.descriptor().id() + "=" + supported);
        if (supported) {
          matches.add(bridge);
        }
      } catch (Throwable throwable) {
        failures.add(bridge.descriptor().id() + ":"
            + throwable.getClass().getSimpleName());
        supportTrace.add(bridge.descriptor().id() + "=ERROR");
      }
    }
    String trace = "supports[" + String.join(",", supportTrace) + "]";
    if (!failures.isEmpty()) {
      return RendererDecision.failed(trace + " failures=" + failures);
    }
    if (matches.isEmpty()) {
      return RendererDecision.none(trace);
    }
    if (matches.size() == 1) {
      return RendererDecision.unique(matches.getFirst().descriptor().source(), trace);
    }
    return RendererDecision.ambiguous(
        "renderer ownership is ambiguous for source "
            + query.slotSource().id() + ": "
            + matches.stream().map(bridge -> bridge.descriptor().id()).toList()
            + " " + trace);
  }

  public static boolean render(
      RendererDecision decision, RendererQuery query, List<CompatibilityBridge> bridges) {
    if (decision.state() != RendererDecision.State.UNIQUE
        || decision.source().isEmpty()) {
      return false;
    }
    SlotSource source = decision.source().orElseThrow();
    CompatibilityBridge bridge = bridges.stream()
        .filter(value -> value.descriptor().source() == source)
        .findFirst()
        .orElse(null);
    if (bridge == null) {
      return false;
    }
    try {
      return bridge.services().renderer().render(query);
    } catch (Throwable throwable) {
      dev.arakiel.curiosfabric.CuriosFabricConstants.LOG.warn(
          "Compatibility bridge {} failed renderer execution",
          bridge.descriptor().id(), throwable);
      return false;
    }
  }
}
