package dev.arakiel.curiosfabric.compat.core.contract;

/** Why a compatibility bridge snapshot is no longer valid. */
public enum BridgeInvalidationReason {
  DATA_PACK_RELOAD,
  CLIENT_DISCONNECT,
  SERVER_STOP,
  CLIENT_STOP,
  SHUTDOWN
}
