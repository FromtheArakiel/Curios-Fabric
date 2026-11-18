package dev.arakiel.curiosfabric.compat.contribution.event;

import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeInvalidationReason;

/** Optional bridge hook for upstream gameplay event translation. */
public interface GameplayHookContributor {

  void initialize(BridgeContext context);

  void invalidate(BridgeInvalidationReason reason);
}
