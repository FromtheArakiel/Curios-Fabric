package dev.arakiel.curiosfabric.compat.contribution.slot;

import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import java.util.List;
import java.util.Optional;

/** Supplies upstream slot definitions without deciding the final global order. */
public interface SlotDiscoveryContributor {

  List<SlotCandidate> slots(BridgeContext context);

  List<SlotDataDirectory> dataDirectories();

  Optional<String> nativeMappedId(String upstreamId);
}
