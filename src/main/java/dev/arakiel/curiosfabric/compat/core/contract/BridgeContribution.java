package dev.arakiel.curiosfabric.compat.core.contract;

import dev.arakiel.curiosfabric.compat.contribution.entity.EntitySlotBinding;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotCandidate;
import java.util.List;

/** Immutable result produced by a bridge refresh. */
public record BridgeContribution(
    ContributionStatus status,
    List<SlotCandidate> slots,
    List<EntitySlotBinding> entityBindings,
    List<BridgeDiagnostic> diagnostics) {

  public BridgeContribution {
    if (status == null || slots == null || entityBindings == null || diagnostics == null) {
      throw new IllegalArgumentException("contribution fields must not be null");
    }
    slots = List.copyOf(slots);
    entityBindings = List.copyOf(entityBindings);
    diagnostics = List.copyOf(diagnostics);
  }

  public static BridgeContribution empty() {
    return new BridgeContribution(ContributionStatus.EMPTY, List.of(), List.of(), List.of());
  }

  public static BridgeContribution notApplicable() {
    return new BridgeContribution(
        ContributionStatus.NOT_APPLICABLE, List.of(), List.of(), List.of());
  }

  public static BridgeContribution failed(BridgeDiagnostic diagnostic) {
    return new BridgeContribution(
        ContributionStatus.FAILED, List.of(), List.of(), List.of(diagnostic));
  }
}
