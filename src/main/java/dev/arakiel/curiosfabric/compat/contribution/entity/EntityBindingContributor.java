package dev.arakiel.curiosfabric.compat.contribution.entity;

import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import java.util.List;

/** Supplies entity slot assignments to the coordinator. */
public interface EntityBindingContributor {

  List<EntitySlotBinding> bindings(BridgeContext context);
}
