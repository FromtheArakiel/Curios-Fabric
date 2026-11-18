package dev.arakiel.curiosfabric.compat.core.contract;

/** Why a compatibility bridge snapshot is being refreshed. */
public enum BridgeRefreshReason {
  INIT,
  DATA_PACK_RELOAD,
  CLIENT_SYNC,
  SERVER_STARTING,
  SERVER_STARTED,
  LOGIN,
  RESPAWN
}
