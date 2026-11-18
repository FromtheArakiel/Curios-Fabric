package dev.arakiel.curiosfabric.compat.modules.trinkets.tags;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotCandidate;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelation;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationContributor;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationDirection;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationSet;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import dev.arakiel.curiosfabric.compat.modules.trinkets.discovery.TrinketsNativeMappings;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Trinkets-owned tag namespace conversion. */
public final class TrinketsTagRelations implements TagRelationContributor {

  @Override
  public TagRelationSet relations(
      BridgeContext context, Collection<ResourceLocation> tagIds) {
    Set<TagRelation> relations = new LinkedHashSet<>();
    for (ResourceLocation tag : tagIds) {
      if (!tag.getNamespace().equals("trinkets") || !tag.getPath().contains("/")) {
        continue;
      }
      String nativeId = TrinketsNativeMappings.nativeId(tag.getPath());
      String mappingId = nativeId == null
          ? TrinketsNativeMappings.appendedMappingId(tag.getPath()) : nativeId;
      relations.add(new TagRelation(
          tag,
          ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, mappingId),
          TagRelationDirection.BIDIRECTIONAL,
          "trinkets_slot_mapping"));
    }
    return new TagRelationSet(relations);
  }

  @Override
  public Set<ResourceLocation> semanticTags(SlotCandidate candidate) {
    if (candidate.key().source() != dev.arakiel.curiosfabric.api.type.SlotSource.TRINKETS) {
      return Set.of();
    }
    return Set.of(ResourceLocation.fromNamespaceAndPath(
        "trinkets", candidate.key().upstreamPath()));
  }
}
