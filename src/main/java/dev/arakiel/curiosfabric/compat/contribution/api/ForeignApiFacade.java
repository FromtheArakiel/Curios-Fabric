package dev.arakiel.curiosfabric.compat.contribution.api;

import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeInvalidationReason;

/** Bridge-owned view of CuriosFabric state through one foreign API. */
public interface ForeignApiFacade {

  void initialize(BridgeContext context);

  void invalidate(BridgeInvalidationReason reason);
}
