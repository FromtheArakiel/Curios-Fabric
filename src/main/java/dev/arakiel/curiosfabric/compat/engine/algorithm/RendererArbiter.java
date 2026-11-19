package dev.arakiel.curiosfabric.compat.engine.algorithm;

import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererDecision;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererQuery;
import dev.arakiel.curiosfabric.compat.core.contract.CompatibilityBridge;
import java.util.ArrayList;
import java.util.List;

/** Resolves renderer ownership without trial-and-error across unrelated sources. */
public final class RendererArbiter {

  private RendererArbiter() {
  }

  public static RendererDecision decide(
      RendererQuery query, List<CompatibilityBridge> bridges) {
    List<String> trace = new ArrayList<>();
    Evaluation primary = evaluate(query, primaryCandidates(query, bridges), false);
    trace.add("supports[" + primary.trace() + "]");
    RendererDecision primaryDecision = decision(query, primary, trace);
    if (primaryDecision != null) {
      return primaryDecision;
    }

    if (query.slotSource() != SlotSource.CURIOSFABRIC
        && query.slotSource() != SlotSource.ACCESSORIES) {
      List<CompatibilityBridge> alternates = alternateCandidates(query, bridges);
      if (!alternates.isEmpty()) {
        Evaluation alternate = evaluate(query, alternates, false);
        trace.add("alternate[" + alternate.trace() + "]");
        RendererDecision alternateDecision = decision(query, alternate, trace);
        if (alternateDecision != null) {
          return alternateDecision;
        }
      }
    }

    if (query.slotSource() != SlotSource.ACCESSORIES
        && isTrinketItem(query, bridges)) {
      List<CompatibilityBridge> fallbacks = bridges.stream()
          .filter(bridge -> bridge.descriptor().source() == SlotSource.ACCESSORIES)
          .toList();
      Evaluation fallback = evaluate(query, fallbacks, true);
      trace.add("trinketFallback[" + fallback.trace() + "]");
      RendererDecision fallbackDecision = decision(query, fallback, trace);
      if (fallbackDecision != null) {
        return fallbackDecision;
      }
    }

    return RendererDecision.none(String.join(" ", trace));
  }

  private static List<CompatibilityBridge> primaryCandidates(
      RendererQuery query, List<CompatibilityBridge> bridges) {
    if (query.slotSource() == SlotSource.CURIOSFABRIC) {
      return bridges;
    }
    return bridges.stream()
        .filter(bridge -> bridge.descriptor().source() == query.slotSource())
        .toList();
  }

  private static List<CompatibilityBridge> alternateCandidates(
      RendererQuery query, List<CompatibilityBridge> bridges) {
    return bridges.stream()
        .filter(bridge -> bridge.descriptor().source() != SlotSource.ACCESSORIES)
        .filter(bridge -> bridge.descriptor().source() != query.slotSource())
        .filter(bridge -> bridge.descriptor().source() == SlotSource.TRINKETS
            || bridge.descriptor().source() == SlotSource.NEOFORGE_CURIOS)
        .toList();
  }

  private static Evaluation evaluate(
      RendererQuery query,
      List<CompatibilityBridge> candidates,
      boolean fallback) {
    List<CompatibilityBridge> matches = new ArrayList<>();
    List<String> failures = new ArrayList<>();
    List<String> supportTrace = new ArrayList<>();
    for (CompatibilityBridge bridge : candidates) {
      try {
        boolean supported = fallback
            ? bridge.services().renderer().supportsFallback(query)
            : bridge.services().renderer().supports(query);
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
    return new Evaluation(
        List.copyOf(matches), List.copyOf(failures),
        String.join(",", supportTrace));
  }

  private static RendererDecision decision(
      RendererQuery query, Evaluation evaluation, List<String> trace) {
    if (!evaluation.failures().isEmpty()) {
      return RendererDecision.failed(
          String.join(" ", trace) + " failures=" + evaluation.failures());
    }
    if (evaluation.matches().isEmpty()) {
      return null;
    }
    if (evaluation.matches().size() == 1) {
      return RendererDecision.unique(
          evaluation.matches().getFirst().descriptor().source(),
          String.join(" ", trace));
    }
    return RendererDecision.ambiguous(
        "renderer ownership is ambiguous for source "
            + query.slotSource().id() + ": "
            + evaluation.matches().stream()
                .map(bridge -> bridge.descriptor().id()).toList()
            + " " + String.join(" ", trace));
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

  private static boolean isTrinketItem(
      RendererQuery query, List<CompatibilityBridge> bridges) {
    for (CompatibilityBridge bridge : bridges) {
      if (bridge.descriptor().source() != SlotSource.TRINKETS) {
        continue;
      }
      try {
        if (bridge.services().renderer().ownsItemForFallback(query)) {
          return true;
        }
      } catch (Throwable ignored) {
        // A failed assignment probe must not turn an unrelated item into a fallback candidate.
      }
    }
    return false;
  }

  private record Evaluation(
      List<CompatibilityBridge> matches,
      List<String> failures,
      String trace) {
  }
}
