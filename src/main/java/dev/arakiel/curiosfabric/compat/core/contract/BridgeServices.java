package dev.arakiel.curiosfabric.compat.core.contract;

import dev.arakiel.curiosfabric.compat.contribution.api.ForeignApiFacade;
import dev.arakiel.curiosfabric.compat.contribution.entity.EntityBindingContributor;
import dev.arakiel.curiosfabric.compat.contribution.event.GameplayHookContributor;
import dev.arakiel.curiosfabric.compat.contribution.item.ItemAssignmentContributor;
import dev.arakiel.curiosfabric.compat.contribution.item.ItemBehaviorContributor;
import dev.arakiel.curiosfabric.compat.contribution.lifecycle.ResourceDependencyContributor;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererContributor;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotDiscoveryContributor;
import dev.arakiel.curiosfabric.compat.contribution.slot.SlotNamingContributor;
import dev.arakiel.curiosfabric.compat.contribution.tag.TagRelationContributor;

/** The optional services a bridge can contribute to the compatibility coordinator. */
public record BridgeServices(
    SlotDiscoveryContributor slots,
    SlotNamingContributor naming,
    TagRelationContributor tags,
    EntityBindingContributor entities,
    ItemAssignmentContributor itemAssignments,
    ItemBehaviorContributor itemBehavior,
    ForeignApiFacade api,
    RendererContributor renderer,
    GameplayHookContributor gameplay,
    ResourceDependencyContributor resourceDependencies) {

  public BridgeServices {
    if (slots == null || naming == null || tags == null || entities == null
        || itemAssignments == null || itemBehavior == null || api == null
        || renderer == null || gameplay == null || resourceDependencies == null) {
      throw new IllegalArgumentException("bridge services must not be null");
    }
  }
}
