package dev.arakiel.curiosfabric.compat.contribution.tag;

import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotCandidate;
import java.util.Collection;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Contributes tag graph edges; the coordinator performs the final merge. */
public interface TagRelationContributor {

  TagRelationSet relations(BridgeContext context, Collection<ResourceLocation> tagIds);

  Set<ResourceLocation> semanticTags(SlotCandidate candidate);
}
