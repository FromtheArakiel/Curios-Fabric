package dev.arakiel.curiosfabric.compat.core.contract;

/**
 * Standard macro contract implemented by every compatibility bridge.
 *
 * <p>The bridge owns all upstream-specific discovery and adaptation. The coordinator owns
 * cross-bridge ordering, conflict resolution and publication.</p>
 */
public interface CompatibilityBridge {

  BridgeDescriptor descriptor();

  BridgeAvailability availability(BridgeContext context);

  BridgeServices services();

  BridgeContribution refresh(BridgeContext context);

  void invalidate(BridgeInvalidationReason reason);
}
