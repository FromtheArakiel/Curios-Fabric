package dev.arakiel.curiosfabric.compat.api;

import dev.arakiel.curiosfabric.api.type.SlotSource;

/** One compatibility target split into independent data and behavior bridges. */
public interface CompatibilityBridge {

  String modId();

  SlotSource source();

  boolean isLoaded();

  SlotMetadataBridge metadata();

  ItemTagBridge tags();

  EntityBindingBridge entityBindings();

  ItemBehaviorBridge itemBehavior();

  RendererBridge renderer();
}
