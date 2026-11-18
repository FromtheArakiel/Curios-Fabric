package dev.arakiel.curiosfabric.compat.engine.state;

import dev.arakiel.curiosfabric.compat.core.contract.BridgeContribution;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeGeneration;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeSide;
import java.util.EnumMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Holds the latest client/server contributions without sharing entity state. */
public final class BridgeStateStore {

  private final Map<BridgeSide, BridgeGeneration> generations =
      new EnumMap<>(BridgeSide.class);
  private final Map<BridgeSide, Map<String, BridgeContribution>> contributions =
      new EnumMap<>(BridgeSide.class);

  public BridgeStateStore() {
    generations.put(BridgeSide.CLIENT, BridgeGeneration.initial());
    generations.put(BridgeSide.SERVER, BridgeGeneration.initial());
    contributions.put(BridgeSide.CLIENT, new LinkedHashMap<>());
    contributions.put(BridgeSide.SERVER, new LinkedHashMap<>());
  }

  public synchronized BridgeGeneration nextGeneration(BridgeSide side) {
    BridgeGeneration next = generations.get(side).next();
    generations.put(side, next);
    return next;
  }

  public synchronized BridgeGeneration currentGeneration(BridgeSide side) {
    return generations.get(side);
  }

  public synchronized void put(String bridgeId, BridgeSide side,
                               BridgeContribution contribution) {
    contributions.get(side).put(bridgeId, contribution);
  }

  public synchronized BridgeSideSnapshot snapshot(BridgeSide side) {
    return new BridgeSideSnapshot(
        generations.get(side), Collections.unmodifiableMap(
            new LinkedHashMap<>(contributions.get(side))));
  }

  public synchronized Optional<BridgeContribution> contribution(
      String bridgeId, BridgeSide side) {
    return Optional.ofNullable(contributions.get(side).get(bridgeId));
  }

  public synchronized void invalidate(BridgeSide side) {
    generations.put(side, generations.get(side).next());
    contributions.get(side).clear();
  }
}
