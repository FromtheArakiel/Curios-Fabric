package dev.arakiel.curiosfabric.compat.core.contract;

/** Explicit lifecycle state of a compatibility bridge. */
public enum BridgeState {
  UNLOADED,
  LOADING,
  READY,
  DEGRADED,
  FAILED
}
