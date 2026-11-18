package dev.arakiel.curiosfabric.compat.engine.algorithm;

import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelation;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationDirection;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagLoader;

/** Generic tag graph merger; concrete namespaces are supplied by bridges. */
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

    Set<Edge> bidirectional = new LinkedHashSet<>();
    List<TagRelation> directed = new ArrayList<>();
    for (TagRelationSet relationSet : contributedRelations) {
      for (TagRelation relation : relationSet.relations()) {
        if (relation.direction() == TagRelationDirection.BIDIRECTIONAL) {
          bidirectional.add(new Edge(relation.sourceTag(), relation.semanticTag()));
        } else {
          directed.add(relation);
        }
      }
    }

    Map<ResourceLocation, Set<ResourceLocation>> graph = graph(bidirectional);
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

    for (TagRelation relation : directed) {
      Set<TagLoader.EntryWithSource> entries = new LinkedHashSet<>(
          tags.getOrDefault(relation.semanticTag(), List.of()));
      entries.addAll(snapshot.getOrDefault(relation.sourceTag(), List.of()));
      tags.put(relation.semanticTag(), new ArrayList<>(entries));
    }
  }

  private static Map<ResourceLocation, Set<ResourceLocation>> graph(Set<Edge> edges) {
    Map<ResourceLocation, Set<ResourceLocation>> graph = new LinkedHashMap<>();
    for (Edge edge : edges) {
      if (edge.left().equals(edge.right())) {
        continue;
      }
      graph.computeIfAbsent(edge.left(), ignored -> new LinkedHashSet<>()).add(edge.right());
      graph.computeIfAbsent(edge.right(), ignored -> new LinkedHashSet<>()).add(edge.left());
    }
    return graph;
  }

  private record Edge(ResourceLocation left, ResourceLocation right) {

    private Edge {
      if (right.compareTo(left) < 0) {
        ResourceLocation swap = left;
        left = right;
        right = swap;
      }
    }
  }
}
