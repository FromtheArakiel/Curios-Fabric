package dev.arakiel.curiosfabric.compat.engine.algorithm;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelation;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagLoader;

/**
 * Copies upstream tag entries into CuriosFabric semantic tags.
 *
 * <p>The merger deliberately does not build a bidirectional tag graph. A canonical tag may
 * aggregate entries from many bridges, but a third-party tag must never receive entries from
 * another bridge's namespace.</p>
 */
public final class TagRelationMerger {

  private TagRelationMerger() {
  }

  public static void merge(
      Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags,
      List<TagRelationSet> contributedRelations) {
    if (tags.isEmpty() || contributedRelations.isEmpty()) {
      return;
    }

    Map<ResourceLocation, List<TagLoader.EntryWithSource>> snapshot = new LinkedHashMap<>();
    tags.forEach((id, entries) -> snapshot.put(id, new ArrayList<>(entries)));

    for (TagRelationSet relationSet : contributedRelations) {
      for (TagRelation relation : relationSet.relations()) {
        if (!isAllowed(relation)) {
          continue;
        }
        Set<TagLoader.EntryWithSource> entries = new LinkedHashSet<>(
            tags.getOrDefault(relation.semanticTag(), List.of()));
        entries.addAll(snapshot.getOrDefault(relation.sourceTag(), List.of()));
        tags.put(relation.semanticTag(), new ArrayList<>(entries));
      }
    }
  }

  private static boolean isAllowed(TagRelation relation) {
    ResourceLocation source = relation.sourceTag();
    ResourceLocation semantic = relation.semanticTag();
    if (source.equals(semantic)) {
      return false;
    }
    return semantic.getNamespace().equals(CuriosFabricConstants.MOD_ID)
        && !source.getNamespace().equals(CuriosFabricConstants.MOD_ID);
  }
}
