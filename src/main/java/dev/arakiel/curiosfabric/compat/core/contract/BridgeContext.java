package dev.arakiel.curiosfabric.compat.core.contract;

/** Side-aware lifecycle context passed to every bridge. */
public record BridgeContext(
    BridgeSide side,
    BridgePhase phase,
    BridgeRefreshReason reason,
    boolean authoritative,
    BridgeGeneration generation) {

  public BridgeContext {
    if (side == null || phase == null || reason == null || generation == null) {
      throw new IllegalArgumentException("bridge context fields must not be null");
    }
  }

  public static BridgeContext server(
      BridgePhase phase, BridgeRefreshReason reason, boolean authoritative,
      BridgeGeneration generation) {
    return new BridgeContext(BridgeSide.SERVER, phase, reason, authoritative, generation);
  }

  public static BridgeContext client(
      BridgePhase phase, BridgeRefreshReason reason, boolean authoritative,
      BridgeGeneration generation) {
    return new BridgeContext(BridgeSide.CLIENT, phase, reason, authoritative, generation);
  }

  public boolean client() {
    return side == BridgeSide.CLIENT;
  }
}
