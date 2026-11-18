package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.tags;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotCandidate;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelation;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationContributor;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationDirection;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationSet;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** NeoForge Curios-owned tag namespace conversion. */
public final class NeoforgeCuriosTagRelations implements TagRelationContributor {

  @Override
  public TagRelationSet relations(
      BridgeContext context, Collection<ResourceLocation> tagIds) {
    Set<TagRelation> relations = new LinkedHashSet<>();
    for (ResourceLocation tag : tagIds) {
      if (!tag.getNamespace().equals(CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE)) {
        continue;
      }
      relations.add(new TagRelation(
          tag,
          ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, tag.getPath()),
          TagRelationDirection.BIDIRECTIONAL,
          "neoforge_curios_slot_mapping"));
    }
    return new TagRelationSet(relations);
  }

  @Override
  public Set<ResourceLocation> semanticTags(SlotCandidate candidate) {
    if (candidate.key().source() != SlotSource.NEOFORGE_CURIOS) {
      return Set.of();
    }
    return Set.of(ResourceLocation.fromNamespaceAndPath(
        CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE, candidate.mappingId()));
  }
}
