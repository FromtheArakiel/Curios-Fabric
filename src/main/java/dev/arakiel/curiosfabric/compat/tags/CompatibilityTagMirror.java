package dev.arakiel.curiosfabric.compat.tags;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.compat.CompatTargets;
import dev.arakiel.curiosfabric.compat.NativeSlotMappings;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagLoader;

/**
 * Mirrors Trinkets and NeoForge Curios item tags into {@code curiosfabric} tags, and back.
 *
 * <p>The mirror runs before tags are bound, so {@link net.minecraft.world.item.ItemStack#is} sees
 * real registry tags on both sides. Entries are copied from an immutable snapshot; generated tags
 * therefore cannot cascade into unrelated tags during the same load.</p>
 */
public final class CompatibilityTagMirror {

  private CompatibilityTagMirror() {
  }

  public static void mirror(Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags) {
    if (!CompatTargets.isTrinketsLoaded() && !CompatTargets.isNeoforgeCuriosLoaded()) {
      return;
    }

    Map<ResourceLocation, List<TagLoader.EntryWithSource>> snapshot = new LinkedHashMap<>();
    tags.forEach((id, entries) -> snapshot.put(id, new ArrayList<>(entries)));
    Set<Relation> relations = relations(snapshot.keySet());
    Map<ResourceLocation, Set<ResourceLocation>> graph = graph(relations);

    Set<ResourceLocation> visited = new HashSet<>();
    for (ResourceLocation start : graph.keySet()) {
      if (!visited.add(start)) {
        continue;
      }
      Set<TagLoader.EntryWithSource> combined = new LinkedHashSet<>();
      ArrayDeque<ResourceLocation> queue = new ArrayDeque<>();
      Set<ResourceLocation> component = new LinkedHashSet<>();
      queue.add(start);

      while (!queue.isEmpty()) {
        ResourceLocation current = queue.removeFirst();
        component.add(current);
        combined.addAll(snapshot.getOrDefault(current, List.of()));
        for (ResourceLocation next : graph.getOrDefault(current, Set.of())) {
          if (visited.add(next)) {
            queue.addLast(next);
          }
        }
      }

      for (ResourceLocation tag : component) {
        tags.put(tag, new ArrayList<>(combined));
      }
    }
  }

  static Set<Relation> relations(Set<ResourceLocation> tagIds) {
    Set<Relation> relations = new LinkedHashSet<>();
    Map<String, Set<ResourceLocation>> trinketsByCuriosId = new LinkedHashMap<>();

    for (ResourceLocation id : tagIds) {
      if (id.getNamespace().equals(CuriosFabricConstants.MOD_ID)) {
        continue;
      }
      if (id.getNamespace().equals(CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE)) {
        relations.add(relation(id,
            curiosFabricTag(id.getPath())));
      }
      if (id.getNamespace().equals("trinkets")) {
        String curiosId = trinketsSlotId(id.getPath());
        ResourceLocation curiosFabric = curiosFabricTag(curiosId);
        relations.add(relation(id, curiosFabric));
        relations.add(relation(curiosFabric, neoForgeTag(curiosId)));
        trinketsByCuriosId.computeIfAbsent(curiosId, ignored -> new LinkedHashSet<>()).add(id);
      }
    }

    for (ResourceLocation id : tagIds) {
      if (!id.getNamespace().equals(CuriosFabricConstants.MOD_ID)) {
        continue;
      }
      relations.add(relation(id, neoForgeTag(id.getPath())));
      for (ResourceLocation trinketsTag : trinketsByCuriosId.getOrDefault(
          id.getPath(), Set.of())) {
        relations.add(relation(id, trinketsTag));
      }
    }
    return relations;
  }

  static String trinketsSlotId(String path) {
    String nativeId = NativeSlotMappings.trinketsNative(path);
    return nativeId != null ? nativeId : NativeSlotMappings.appendTrinketsSlot(path);
  }

  private static Map<ResourceLocation, Set<ResourceLocation>> graph(Set<Relation> relations) {
    Map<ResourceLocation, Set<ResourceLocation>> graph = new LinkedHashMap<>();
    for (Relation relation : relations) {
      if (relation.source().equals(relation.target())) {
        continue;
      }
      graph.computeIfAbsent(relation.source(), ignored -> new LinkedHashSet<>())
          .add(relation.target());
      graph.computeIfAbsent(relation.target(), ignored -> new LinkedHashSet<>())
          .add(relation.source());
    }
    return graph;
  }

  static Relation relation(ResourceLocation left, ResourceLocation right) {
    return left.compareTo(right) <= 0
        ? new Relation(left, right) : new Relation(right, left);
  }

  private static ResourceLocation curiosFabricTag(String path) {
    return ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, path);
  }

  private static ResourceLocation neoForgeTag(String path) {
    return ResourceLocation.fromNamespaceAndPath(
        CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE, path);
  }

  record Relation(ResourceLocation source, ResourceLocation target) {
  }
}
