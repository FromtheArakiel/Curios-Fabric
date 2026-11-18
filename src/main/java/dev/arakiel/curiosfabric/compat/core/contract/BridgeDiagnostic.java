package dev.arakiel.curiosfabric.compat.core.contract;

/** Structured diagnostic emitted by a compatibility bridge. */
public record BridgeDiagnostic(
    String bridgeId,
    BridgeSide side,
    BridgePhase phase,
    BridgeGeneration generation,
    ContributionStatus status,
    String message) {

  public BridgeDiagnostic {
    if (bridgeId == null || side == null || phase == null || generation == null
        || status == null || message == null) {
      throw new IllegalArgumentException("diagnostic fields must not be null");
    }
  }
}
