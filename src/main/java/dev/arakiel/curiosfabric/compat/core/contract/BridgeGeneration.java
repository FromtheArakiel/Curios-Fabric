package dev.arakiel.curiosfabric.compat.core.contract;

/** Monotonic generation shared by a bridge context and its contributions. */
public record BridgeGeneration(long value) {

  public BridgeGeneration {
    if (value < 0L) {
      throw new IllegalArgumentException("generation must not be negative");
    }
  }

  public static BridgeGeneration initial() {
    return new BridgeGeneration(0L);
  }

  public BridgeGeneration next() {
    return new BridgeGeneration(value + 1L);
  }

  public boolean isOlderThan(BridgeGeneration other) {
    return other != null && value < other.value;
  }
}
