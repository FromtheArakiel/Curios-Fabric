package dev.arakiel.curiosfabric.compat.core.contract;

/** Availability of one bridge for one side and generation. */
public record BridgeAvailability(
    BridgeState state,
    String detail,
    BridgeGeneration generation) {

  public BridgeAvailability {
    if (state == null || detail == null || generation == null) {
      throw new IllegalArgumentException("availability fields must not be null");
    }
  }

  public static BridgeAvailability ready(BridgeGeneration generation) {
    return new BridgeAvailability(BridgeState.READY, "", generation);
  }

  public static BridgeAvailability unavailable(
      BridgeState state, String detail, BridgeGeneration generation) {
    return new BridgeAvailability(state, detail, generation);
  }

  public boolean ready() {
    return state == BridgeState.READY || state == BridgeState.DEGRADED;
  }

  /**
   * Whether the bridge exists on this side and should participate in compatibility queries.
   *
   * <p>This preserves the old {@code isLoaded} semantics: a loaded bridge with degraded or failed
   * health remains callable so the coordinator can isolate its failure and continue with peers.</p>
   */
  public boolean active() {
    return state != BridgeState.UNLOADED;
  }
}
