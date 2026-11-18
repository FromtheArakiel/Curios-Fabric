package dev.arakiel.curiosfabric.compat.core.contract;

/** The compatibility lifecycle phase currently being processed. */
public enum BridgePhase {
  DISCOVER,
  INITIALIZE,
  TAGS,
  DATA,
  ENTITY,
  RUNTIME,
  PUBLISH,
  INVALIDATE,
  SHUTDOWN
}
