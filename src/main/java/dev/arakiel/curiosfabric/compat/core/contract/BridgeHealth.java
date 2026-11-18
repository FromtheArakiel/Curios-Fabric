package dev.arakiel.curiosfabric.compat.core.contract;

/** Last observed health for one bridge and side. */
public record BridgeHealth(BridgeAvailability availability, String detail) {

  public BridgeHealth {
    if (availability == null || detail == null) {
      throw new IllegalArgumentException("health fields must not be null");
    }
  }
}
