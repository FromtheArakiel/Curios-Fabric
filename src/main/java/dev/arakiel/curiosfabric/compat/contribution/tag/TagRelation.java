package dev.arakiel.curiosfabric.compat.contribution.tag;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** One source-to-semantic tag relation contributed by a bridge. */
public record TagRelation(
    ResourceLocation sourceTag,
    ResourceLocation semanticTag,
    TagRelationDirection direction,
    String reason) {

  public TagRelation {
    if (sourceTag == null || semanticTag == null || direction == null || reason == null) {
      throw new IllegalArgumentException("tag relation fields must not be null");
    }
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TagRelation relation
        && sourceTag.equals(relation.sourceTag)
        && semanticTag.equals(relation.semanticTag)
        && direction == relation.direction;
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceTag, semanticTag, direction);
  }
}
