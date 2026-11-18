package dev.arakiel.curiosfabric.compat.contribution.item;

import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;

/** A bridge behavior claim, with code-backed adapters ranked above tag-only defaults. */
public record ItemBehaviorResolution(ICurioItem behavior, boolean codeBacked) {

  public ItemBehaviorResolution {
    if (behavior == null) {
      throw new IllegalArgumentException("behavior must not be null");
    }
  }
}
