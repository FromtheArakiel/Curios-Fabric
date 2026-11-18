package dev.arakiel.curiosfabric.compat.engine.state;

import dev.arakiel.curiosfabric.compat.core.contract.BridgeHealth;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeSide;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Last observed health by bridge and side. */
public final class BridgeHealthRegistry {

  private final Map<Key, BridgeHealth> health = new ConcurrentHashMap<>();

  public void put(String bridgeId, BridgeSide side, BridgeHealth value) {
    health.put(new Key(bridgeId, side), value);
  }

  public Optional<BridgeHealth> get(String bridgeId, BridgeSide side) {
    return Optional.ofNullable(health.get(new Key(bridgeId, side)));
  }

  private record Key(String bridgeId, BridgeSide side) {
  }
}
