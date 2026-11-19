package dev.arakiel.curiosfabric.compat.modules.accessories.tags;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotCandidate;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelation;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationContributor;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationDirection;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationSet;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import dev.arakiel.curiosfabric.compat.modules.accessories.AccessoriesCompat;
import dev.arakiel.curiosfabric.compat.modules.accessories.discovery.AccessoriesNativeMappings;
import dev.arakiel.curiosfabric.compat.modules.accessories.discovery.AccessoriesSlotIdentity;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Accessories-owned item-tag namespace conversion. */
public final class AccessoriesTagRelations implements TagRelationContributor {

  @Override
  public TagRelationSet relations(
      BridgeContext context, Collection<ResourceLocation> tagIds) {
    Set<TagRelation> relations = new LinkedHashSet<>();
    for (ResourceLocation tag : tagIds) {
      if (tag.getNamespace().equals(AccessoriesCompat.MOD_ID)
          && AccessoriesNativeMappings.isIgnored(tag.toString())) {
        continue;
      }
      String upstreamName = upstreamName(tag, context);
      if (upstreamName == null) {
        continue;
      }
      String mappingId = semanticMapping(upstreamName, context);
      if (mappingId == null) {
        continue;
      }
      relations.add(new TagRelation(
          tag,
          ResourceLocation.fromNamespaceAndPath(
              CuriosFabricConstants.MOD_ID, mappingId),
          TagRelationDirection.ONE_WAY,
          "accessories_slot_mapping"));
    }
    return new TagRelationSet(relations);
  }

  @Override
  public Set<ResourceLocation> semanticTags(SlotCandidate candidate) {
    String upstreamName = candidate.key().upstreamPath();
    ResourceLocation tag = upstreamName.contains(":")
        ? ResourceLocation.parse(upstreamName)
        : ResourceLocation.fromNamespaceAndPath(
            AccessoriesCompat.MOD_ID, upstreamName);
    return Set.of(tag);
  }

  static String semanticMapping(String upstreamName, BridgeContext context) {
    if (upstreamName == null) {
      return null;
    }
    if (AccessoriesNativeMappings.isIgnored(upstreamName)
        || AccessoriesNativeMappings.isIgnored(
            AccessoriesCompat.MOD_ID + ":" + upstreamName)) {
      return null;
    }
    if (upstreamName.equals("any")) {
      return "curio";
    }
    if (upstreamName.equals("all")) {
      return null;
    }
    String nativeId = AccessoriesNativeMappings.nativeId(upstreamName);
    if (nativeId != null) {
      return nativeId;
    }
    return AccessoriesSlotIdentity.normalizeUpstream(
        "accessories_", upstreamName);
  }

  private static String upstreamName(
      ResourceLocation tag, BridgeContext context) {
    if (tag.getNamespace().equals(AccessoriesCompat.MOD_ID)) {
      String path = tag.getPath();
      if (path.equals("any")
          || AccessoriesNativeMappings.builtInNames().contains(path)) {
        return path;
      }
      return AccessoriesCompat.knownUpstreamName(path, context.client()).orElse(null);
    }
    return AccessoriesCompat.knownUpstreamName(
        tag.toString(), context.client()).orElse(null);
  }
}
