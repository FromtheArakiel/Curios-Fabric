package dev.arakiel.curiosfabric.compat.contribution.tag;

import java.util.LinkedHashSet;
import java.util.Set;

/** Immutable collection of tag relations contributed by one bridge. */
public record TagRelationSet(Set<TagRelation> relations) {

  public TagRelationSet {
    if (relations == null) {
      throw new IllegalArgumentException("relations must not be null");
    }
    relations = Set.copyOf(new LinkedHashSet<>(relations));
  }

  public static TagRelationSet empty() {
    return new TagRelationSet(Set.of());
  }
}
